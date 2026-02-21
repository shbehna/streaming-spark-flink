package info.hikaridata;

public class Alert {
    private String symbol;
    private double currentPrice;
    private double previousPrice;
    private double changePercent;
    private String timestamp;

    public Alert() {
    }

    public Alert(String symbol, double currentPrice, double previousPrice, double changePercent, String timestamp) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.previousPrice = previousPrice;
        this.changePercent = changePercent;
        this.timestamp = timestamp;
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

    public double getPreviousPrice() {
        return previousPrice;
    }

    public void setPreviousPrice(double previousPrice) {
        this.previousPrice = previousPrice;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
