package info.hikaridata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * POJO class representing stock data
 */
public class StockData implements Serializable {
    
    @JsonProperty("symbol")
    private String symbol;
    
    @JsonProperty("price")
    private double price;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    public StockData() {
    }
    
    public StockData(String symbol, double price, String timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "StockData{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
