package info.hikaridata;

import java.io.Serializable;

/**
 * Intermediate row representing a completed window aggregate
 * (symbol, window start/end, average price, data point count).
 * Used to bridge from the DataFrame windowed aggregation to
 * the stateful mapGroupsWithState that compares consecutive windows.
 */
public class WindowAggregateRow implements Serializable {
    
    private String symbol;
    private String windowStart;
    private String windowEnd;
    private double avgPrice;
    private long count;
    
    public WindowAggregateRow() {
    }
    
    public WindowAggregateRow(String symbol, String windowStart, String windowEnd,
                              double avgPrice, long count) {
        this.symbol = symbol;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.avgPrice = avgPrice;
        this.count = count;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getWindowStart() {
        return windowStart;
    }
    
    public void setWindowStart(String windowStart) {
        this.windowStart = windowStart;
    }
    
    public String getWindowEnd() {
        return windowEnd;
    }
    
    public void setWindowEnd(String windowEnd) {
        this.windowEnd = windowEnd;
    }
    
    public double getAvgPrice() {
        return avgPrice;
    }
    
    public void setAvgPrice(double avgPrice) {
        this.avgPrice = avgPrice;
    }
    
    public long getCount() {
        return count;
    }
    
    public void setCount(long count) {
        this.count = count;
    }
}
