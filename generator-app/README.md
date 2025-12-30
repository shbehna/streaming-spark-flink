# Stock Ticker Generator

This is a simple web-based stock ticker data generator that sends stock price data to a TCP socket.

## Features

- Clean web UI for entering stock symbol and price
- Real-time data transmission via TCP socket (port 9999)
- JSON serialization of stock data
- History of recent submissions

## Running the Application

1. Make sure you have the dependencies installed:
   ```bash
   pip install Flask Werkzeug
   ```

2. Start the generator:
   ```bash
   python app.py
   ```

3. Open your browser and navigate to:
   ```
   http://localhost:5000
   ```

4. Enter a stock symbol (e.g., AAPL, GOOGL, MSFT) and price, then click "Send Stock Data"

## Data Format

The generator sends JSON data in the following format:
```json
{
  "symbol": "AAPL",
  "price": 150.50,
  "timestamp": "2025-12-29T10:30:45.123456"
}
```

## Socket Configuration

- Host: localhost
- Port: 9999
- Protocol: TCP
- Format: JSON (one record per line)
