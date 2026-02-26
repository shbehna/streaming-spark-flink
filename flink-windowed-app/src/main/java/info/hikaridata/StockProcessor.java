package info.hikaridata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StockProcessor {
    
    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String KAFKA_TOPIC = "stock-data";
    private static final String KAFKA_ALERTS_TOPIC = "flink-windowed-alerts";
    private static final String KAFKA_GROUP_ID = "flink-windowed-consumer-group";
    private static final double ALERT_THRESHOLD = 0.05;
    private static final long WINDOW_SIZE_SECONDS = 5;
    private static final long WATERMARK_DELAY_SECONDS = 2;
    
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        
        KafkaSource<String> source = KafkaSource.<String>builder()
            .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
            .setTopics(KAFKA_TOPIC)
            .setGroupId(KAFKA_GROUP_ID)
            .setStartingOffsets(OffsetsInitializer.latest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();
        
        DataStream<String> kafkaStream = env.fromSource(
            source, 
            WatermarkStrategy.noWatermarks(), 
            "Kafka Source"
        );
        
        // Create Kafka sink for alerts
        KafkaSink<String> alertSink = KafkaSink.<String>builder()
            .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic(KAFKA_ALERTS_TOPIC)
                .setValueSerializationSchema(new SimpleStringSchema())
                .build()
            )
            .build();
        
        kafkaStream
            .flatMap(new JsonParser())
            .assignTimestampsAndWatermarks(
                WatermarkStrategy
                    .<StockData>forBoundedOutOfOrderness(Duration.ofSeconds(WATERMARK_DELAY_SECONDS))
                    .withTimestampAssigner((element, recordTimestamp) -> System.currentTimeMillis())
            )
            .keyBy(StockData::getSymbol)
            .window(TumblingEventTimeWindows.of(Time.seconds(WINDOW_SIZE_SECONDS)))
            .process(new WindowedPriceAlertFunction())
            .sinkTo(alertSink);
        
        env.execute("Flink Windowed Stock Price Alert Processor");
    }
    
    public static class JsonParser extends RichFlatMapFunction<String, StockData> {
        private transient ObjectMapper objectMapper;
        
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            objectMapper = new ObjectMapper();
        }
        
        @Override
        public void flatMap(String jsonString, Collector<StockData> out) throws Exception {
            try {
                StockData stockData = objectMapper.readValue(jsonString, StockData.class);
                out.collect(stockData);
            } catch (IOException e) {
                System.err.println("Error parsing JSON: " + jsonString + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Processes each 5-second tumbling window:
     *  - Computes the average price of all events in the window
     *  - Compares it against the previous window's average (kept in keyed state)
     *  - Emits an alert if the change is >= 5%
     */
    public static class WindowedPriceAlertFunction
            extends ProcessWindowFunction<StockData, String, String, TimeWindow> {
        
        private transient ValueState<WindowedStockState> windowState;
        private transient ObjectMapper objectMapper;
        
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            ValueStateDescriptor<WindowedStockState> descriptor = new ValueStateDescriptor<>(
                "windowedStockState",
                WindowedStockState.class
            );
            windowState = getRuntimeContext().getState(descriptor);
            objectMapper = new ObjectMapper();
        }
        
        @Override
        public void process(String symbol, Context context, Iterable<StockData> elements,
                            Collector<String> out) throws Exception {
            
            double sum = 0;
            long count = 0;
            
            for (StockData data : elements) {
                sum += data.getPrice();
                count++;
            }
            
            if (count == 0) {
                return;
            }
            
            double averagePrice = sum / count;
            String windowStart = formatTimestamp(context.window().getStart());
            String windowEnd = formatTimestamp(context.window().getEnd());
            
            WindowedStockState state = windowState.value();
            
            if (state != null && state.getPreviousAveragePrice() != null) {
                double previousAverage = state.getPreviousAveragePrice();
                double percentChange = (averagePrice - previousAverage) / previousAverage;
                
                if (Math.abs(percentChange) >= ALERT_THRESHOLD) {
                    WindowedAlert alert = new WindowedAlert(
                        symbol,
                        averagePrice,
                        previousAverage,
                        percentChange * 100,
                        windowStart,
                        windowEnd,
                        count
                    );
                    out.collect(objectMapper.writeValueAsString(alert));
                }
            }
            
            windowState.update(new WindowedStockState(averagePrice, windowEnd));
        }
    }
    
    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    
    private static String formatTimestamp(long millis) {
        return TIME_FMT.format(Instant.ofEpochMilli(millis));
    }
}
