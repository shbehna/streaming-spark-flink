package info.hikaridata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapGroupsWithStateFunction;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.GroupState;
import org.apache.spark.sql.streaming.GroupStateTimeout;
import org.apache.spark.sql.streaming.StreamingQuery;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.apache.spark.sql.functions.*;

public class StockProcessor {
    
    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String KAFKA_TOPIC = "stock-data";
    private static final String KAFKA_ALERTS_TOPIC = "spark-windowed-alerts";
    private static final double ALERT_THRESHOLD = 0.05;
    private static final String WINDOW_DURATION = "10 seconds";
    private static final String WATERMARK_DELAY = "2 seconds";
    
    public static void main(String[] args) throws Exception {
        SparkConf conf = new SparkConf()
            .setAppName("Spark Windowed Stock Price Alert Processor")
            .setMaster("local[*]")
            .set("spark.sql.shuffle.partitions", "1")
            // Disable the watermark correctness check for chained stateful operations
            // We understand the risk: windowed aggregation output may arrive "late" to mapGroupsWithState
            .set("spark.sql.streaming.statefulOperator.checkCorrectness.enabled", "false");
        
        SparkSession spark = SparkSession.builder()
            .config(conf)
            .getOrCreate();
        
        spark.sparkContext().setLogLevel("ERROR");
        
        // Read from Kafka
        Dataset<Row> kafkaData = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
            .option("subscribe", KAFKA_TOPIC)
            .option("startingOffsets", "latest")
            .option("failOnDataLoss", "false")
            .load();
        
        Dataset<Row> lines = kafkaData.selectExpr("CAST(value AS STRING) as value");
        
        // Parse JSON into StockData
        Dataset<StockData> stockData = lines
            .flatMap(new JsonParser(), Encoders.bean(StockData.class));
        
        // Add an event_time column (current timestamp) for watermarking,
        // then apply watermark and compute windowed average
        Dataset<Row> withTimestamp = stockData
            .withColumn("event_time", current_timestamp());
        
        Dataset<Row> windowedAgg = withTimestamp
            .withWatermark("event_time", WATERMARK_DELAY)
            .groupBy(
                col("symbol"),
                window(col("event_time"), WINDOW_DURATION)
            )
            .agg(
                avg("price").alias("avg_price"),
                count("*").alias("data_count")
            )
            .select(
                col("symbol"),
                col("window"),
                col("avg_price"),
                col("data_count")
            );
        
        // Map rows to WindowAggregateRow beans
        Dataset<WindowAggregateRow> aggRows = windowedAgg.map(
            (MapFunction<Row, WindowAggregateRow>) row -> {
                Row window = row.getStruct(row.fieldIndex("window"));
                return new WindowAggregateRow(
                    row.getString(row.fieldIndex("symbol")),
                    window.getTimestamp(0).toString(),
                    window.getTimestamp(1).toString(),
                    row.getDouble(row.fieldIndex("avg_price")),
                    row.getLong(row.fieldIndex("data_count"))
                );
            },
            Encoders.bean(WindowAggregateRow.class)
        );
        
        // Use mapGroupsWithState to compare consecutive window averages per symbol
        Dataset<String> alerts = aggRows
            .groupByKey(
                (MapFunction<WindowAggregateRow, String>) WindowAggregateRow::getSymbol,
                Encoders.STRING()
            )
            .mapGroupsWithState(
                new WindowedPriceAlertDetector(),
                Encoders.bean(WindowedStockState.class),
                Encoders.STRING(),
                GroupStateTimeout.NoTimeout()
            )
            .filter((FilterFunction<String>) alert -> alert != null && !alert.isEmpty());
        
        StreamingQuery query = alerts
            .selectExpr("CAST(value AS STRING) as value")
            .writeStream()
            .outputMode("update")
            .format("kafka")
            .option("kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
            .option("topic", KAFKA_ALERTS_TOPIC)
            .option("checkpointLocation", "/tmp/spark-windowed-alerts-checkpoint")
            .start();
        
        query.awaitTermination();
    }
    
    public static class JsonParser implements FlatMapFunction<Row, StockData> {
        private transient ObjectMapper objectMapper;
        
        @Override
        public Iterator<StockData> call(Row row) throws Exception {
            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
            }
            
            List<StockData> result = new ArrayList<>();
            try {
                String jsonString = row.getString(0);
                StockData stockData = objectMapper.readValue(jsonString, StockData.class);
                result.add(stockData);
            } catch (Exception e) {
                System.err.println("Error parsing JSON: " + row.getString(0) + " - " + e.getMessage());
            }
            return result.iterator();
        }
    }
    
    /**
     * Compares consecutive window averages for each symbol.
     * Emits an alert when the average price changes by >= 5% between windows.
     */
    public static class WindowedPriceAlertDetector
            implements MapGroupsWithStateFunction<String, WindowAggregateRow, WindowedStockState, String> {
        private transient ObjectMapper objectMapper;
        
        @Override
        public String call(String symbol, Iterator<WindowAggregateRow> values,
                           GroupState<WindowedStockState> state) throws Exception {
            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
            }
            
            // Consume all rows in this micro-batch for this key (typically one window result)
            WindowAggregateRow latest = null;
            while (values.hasNext()) {
                latest = values.next();
            }
            
            if (latest == null) {
                return "";
            }
            
            double averagePrice = latest.getAvgPrice();
            long count = latest.getCount();
            String alert = "";
            
            if (state.exists()) {
                WindowedStockState prev = state.get();
                Double previousAverage = prev.getPreviousAveragePrice();
                
                if (previousAverage != null) {
                    double percentChange = (averagePrice - previousAverage) / previousAverage;
                    
                    if (Math.abs(percentChange) >= ALERT_THRESHOLD) {
                        WindowedAlert alertObj = new WindowedAlert(
                            symbol,
                            averagePrice,
                            previousAverage,
                            percentChange * 100,
                            latest.getWindowStart(),
                            latest.getWindowEnd(),
                            count
                        );
                        alert = objectMapper.writeValueAsString(alertObj);
                    }
                }
            } else {
                WindowedAlert alertObj = new WindowedAlert(
                    symbol,
                    averagePrice,
                    null,
                    null,
                    latest.getWindowStart(),
                    latest.getWindowEnd(),
                    count
                );
                alert = objectMapper.writeValueAsString(alertObj);
            }
            
            state.update(new WindowedStockState(averagePrice, latest.getWindowEnd()));
            return alert;
        }
    }
}
