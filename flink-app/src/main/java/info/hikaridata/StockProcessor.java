package info.hikaridata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.io.IOException;

public class StockProcessor {
    
    private static final String SOCKET_HOST = "localhost";
    private static final int SOCKET_PORT = 9999;
    private static final double ALERT_THRESHOLD = 0.05;
    
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        DataStream<String> socketStream = env.socketTextStream(SOCKET_HOST, SOCKET_PORT);
        
        socketStream
            .flatMap(new JsonParser())
            .keyBy(StockData::getSymbol)
            .flatMap(new PriceChangeDetector())
            .print();
        
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
        
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            ValueStateDescriptor<StockState> descriptor = new ValueStateDescriptor<>(
                "stockState",
                StockState.class
            );
            priceState = getRuntimeContext().getState(descriptor);
        }
        
        @Override
        public void flatMap(StockData current, Collector<String> out) throws Exception {
            StockState state = priceState.value();
            
            if (state != null && state.getLastPrice() != null) {
                double previousPrice = state.getLastPrice();
                double currentPrice = current.getPrice();
                double percentChange = (currentPrice - previousPrice) / previousPrice;
                
                if (Math.abs(percentChange) >= ALERT_THRESHOLD) {
                    String changeStr = String.format("%s%.2f%%", 
                        percentChange > 0 ? "+" : "", 
                        percentChange * 100);
                    String alert = String.format(
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
                    out.collect(alert);
                }
            }
            
            priceState.update(new StockState(current.getPrice(), current.getTimestamp()));
        }
    }
}
