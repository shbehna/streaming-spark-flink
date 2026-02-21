package info.hikaridata;

public class WindowedAlert {
    private String symbol;
    private double currentPrice;
    private Double previousPrice;
    private Double changePercent;
    private String windowStart;
    private String windowEnd;
    private long dataPoints;

    public WindowedAlert() {
    }

    public WindowedAlert(String symbol, double currentPrice, Double previousPrice, Double changePercent, 
                         String windowStart, String windowEnd, long dataPoints) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.previousPrice = previousPrice;
        this.changePercent = changePercent;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.dataPoints = dataPoints;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Double getPreviousPrice() {
        return previousPrice;
    }

    public void setPreviousPrice(Double previousPrice) {
        this.previousPrice = previousPrice;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
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

    public long getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(long dataPoints) {
        this.dataPoints = dataPoints;
    }
}
