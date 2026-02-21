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
import org.apache.flink.util.Collector;

import java.io.IOException;

public class StockProcessor {
    
    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String KAFKA_TOPIC = "stock-data";
    private static final String KAFKA_ALERTS_TOPIC = "flink-alerts";
    private static final String KAFKA_GROUP_ID = "flink-consumer-group";
    private static final double ALERT_THRESHOLD = 0.05;
    
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
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
            .keyBy(StockData::getSymbol)
            .flatMap(new PriceChangeDetector())
            .sinkTo(alertSink);
        
        env.execute("Flink Stock Price Alert Processor");
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
    
    public static class PriceChangeDetector extends RichFlatMapFunction<StockData, String> {
        private transient ValueState<StockState> priceState;
        private transient ObjectMapper objectMapper;
        
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            ValueStateDescriptor<StockState> descriptor = new ValueStateDescriptor<>(
                "stockState",
                StockState.class
            );
            priceState = getRuntimeContext().getState(descriptor);
            objectMapper = new ObjectMapper();
        }
        
        @Override
        public void flatMap(StockData current, Collector<String> out) throws Exception {
            StockState state = priceState.value();
            
            if (state != null && state.getLastPrice() != null) {
                double previousPrice = state.getLastPrice();
                double currentPrice = current.getPrice();
                double percentChange = (currentPrice - previousPrice) / previousPrice;
                
                if (Math.abs(percentChange) >= ALERT_THRESHOLD) {
                    Alert alert = new Alert(
                        current.getSymbol(),
                        currentPrice,
                        previousPrice,
                        percentChange * 100,
                        current.getTimestamp()
                    );
                    out.collect(objectMapper.writeValueAsString(alert));
                }
            }
            
            priceState.update(new StockState(current.getPrice(), current.getTimestamp()));
        }
    }
}
