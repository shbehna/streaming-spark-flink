package info.hikaridata;

import java.io.Serializable;

/**
 * State class to keep track of the previous window's average price for each stock symbol
 */
public class WindowedStockState implements Serializable {
    
    private Double previousAveragePrice;
    private String previousWindowEnd;
    
    public WindowedStockState() {
    }
    
    public WindowedStockState(Double previousAveragePrice, String previousWindowEnd) {
        this.previousAveragePrice = previousAveragePrice;
        this.previousWindowEnd = previousWindowEnd;
    }
    
    public Double getPreviousAveragePrice() {
        return previousAveragePrice;
    }
    
    public void setPreviousAveragePrice(Double previousAveragePrice) {
        this.previousAveragePrice = previousAveragePrice;
    }
    
    public String getPreviousWindowEnd() {
        return previousWindowEnd;
    }
    
    public void setPreviousWindowEnd(String previousWindowEnd) {
        this.previousWindowEnd = previousWindowEnd;
    }
}
