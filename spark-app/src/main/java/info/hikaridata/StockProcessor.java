package info.hikaridata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapGroupsWithStateFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.GroupState;
import org.apache.spark.sql.streaming.GroupStateTimeout;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class StockProcessor {
    
    private static final String SOCKET_HOST = "localhost";
    private static final int SOCKET_PORT = 9999;
    private static final double ALERT_THRESHOLD = 0.05;
    
    public static void main(String[] args) throws Exception {
        SparkConf conf = new SparkConf()
            .setAppName("Spark Stock Price Alert Processor")
            .setMaster("local[*]")
            .set("spark.sql.shuffle.partitions", "1");
        
        SparkSession spark = SparkSession.builder()
            .config(conf)
            .getOrCreate();
        
        spark.sparkContext().setLogLevel("ERROR");
        
        Dataset<Row> lines = spark
            .readStream()
            .format("socket")
            .option("host", SOCKET_HOST)
            .option("port", SOCKET_PORT)
            .load();
        
        Dataset<StockData> stockData = lines
            .flatMap(new JsonParser(), Encoders.bean(StockData.class));
        
        Dataset<String> alerts = stockData
            .groupByKey(
                (MapFunction<StockData, String>) StockData::getSymbol,
                Encoders.STRING()
            )
            .mapGroupsWithState(
                new PriceChangeDetector(),
                Encoders.bean(StockState.class),
                Encoders.STRING(),
                GroupStateTimeout.NoTimeout()
            )
            .filter((FilterFunction<String>) alert -> alert != null && !alert.isEmpty());
        
        StreamingQuery query = alerts
            .writeStream()
            .outputMode("update")
            .foreach(new ForeachWriter<String>() {
                @Override
                public boolean open(long partitionId, long epochId) {
                    return true;
                }
                
                @Override
                public void process(String value) {
                    System.out.println(value);
                }
                
                @Override
                public void close(Throwable errorOrNull) {
                }
            })
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
    
    public static class PriceChangeDetector implements MapGroupsWithStateFunction<String, StockData, StockState, String> {
        
        @Override
        public String call(String symbol, Iterator<StockData> values, GroupState<StockState> state) throws Exception {
            StockData current = null;
            while (values.hasNext()) {
                current = values.next();
            }
            
            if (current == null) {
                return "";
            }
            
            String alert = "";
            
            if (state.exists()) {
                StockState previousState = state.get();
                Double previousPrice = previousState.getLastPrice();
                
                if (previousPrice != null) {
                    double currentPrice = current.getPrice();
                    double percentChange = (currentPrice - previousPrice) / previousPrice;
                    
                    if (Math.abs(percentChange) >= ALERT_THRESHOLD) {
                        String changeStr = String.format("%s%.2f%%", 
                            percentChange > 0 ? "+" : "", 
                            percentChange * 100);
                        alert = String.format(
                            "\n" +
                            "╔═══════════════════════════════════════════════════════════════════════════╗\n" +
                            "║                              PRICE ALERT                                  ║\n" +
                            "╠═══════════════════════════════════════════════════════════════════════════╣\n" +
                            "║  Symbol:         %-57s║\n" +
                            "║  Event Time:     %-57s║\n" +
                            "║  Current Price:  $%-56.2f║\n" +
                            "║  Previous Price: $%-56.2f║\n" +
                            "║  Change:         %-57s║\n" +
                            "╚═══════════════════════════════════════════════════════════════════════════╝\n",
                            current.getSymbol(),
                            current.getTimestamp(),
                            currentPrice,
                            previousPrice,
                            changeStr
                        );
                    }
                }
            }
            
            state.update(new StockState(current.getPrice(), current.getTimestamp()));
            return alert;
        }
    }
}
