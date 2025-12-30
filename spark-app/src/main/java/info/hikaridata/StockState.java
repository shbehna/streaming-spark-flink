package info.hikaridata;

import java.io.Serializable;

public class StockState implements Serializable {
    
    private Double lastPrice;
    private String lastTimestamp;
    
    public StockState() {
    }
    
    public StockState(Double lastPrice, String lastTimestamp) {
        this.lastPrice = lastPrice;
        this.lastTimestamp = lastTimestamp;
    }
    
    public Double getLastPrice() {
        return lastPrice;
    }
    
    public void setLastPrice(Double lastPrice) {
        this.lastPrice = lastPrice;
    }
    
    public String getLastTimestamp() {
        return lastTimestamp;
    }
    
    public void setLastTimestamp(String lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}
