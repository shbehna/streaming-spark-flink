# Stock Alerts Viewer

A real-time web application that monitors and displays stock price alerts from Flink and Spark stream processors.

## Overview

This application consumes alerts from 4 Kafka topics:
- `flink-alerts` - Alerts from Flink processor (individual price changes)
- `flink-windowed-alerts` - Alerts from Flink windowed processor (average price changes per window)
- `spark-alerts` - Alerts from Spark processor (individual price changes)
- `spark-windowed-alerts` - Alerts from Spark windowed processor (average price changes per window)

## Features

- **Real-time Streaming**: Uses Server-Sent Events (SSE) to push alerts to the browser in real-time
- **Multi-Processor View**: Displays alerts from all 4 processors in a unified dashboard
- **Alert Statistics**: Shows total alert counts for each processor
- **Alert History**: Displays the last 50 alerts per processor
- **Interactive Controls**: Pause/resume streaming and clear alerts
- **Auto-Reconnect**: Automatically reconnects if connection is lost

## Prerequisites

- Python 3.7+
- Kafka running on localhost:9092
- At least one of the processor applications running and sending alerts

## Installation

1. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

2. Or use the provided script:
   ```bash
   chmod +x run.sh
   ./run.sh
   ```

## Usage

1. Start the application:
   ```bash
   python app.py
   ```

2. Open your browser and navigate to:
   ```
   http://localhost:5001
   ```

3. The dashboard will automatically connect and start displaying alerts as they arrive

## How It Works

1. **Backend**: Flask app runs Kafka consumers in background threads for each of the 4 topics
2. **Buffering**: Recent alerts are stored in memory (last 100 per topic)
3. **Streaming**: SSE endpoint streams new alerts to connected browsers
4. **Frontend**: JavaScript consumes the SSE stream and dynamically updates the UI

## Architecture

```
┌─────────────────┐
│ Flink App       │─┐
└─────────────────┘ │
┌─────────────────┐ │    ┌──────────────┐    ┌──────────────┐
│ Flink Windowed  │─┼───▶│ Kafka Topics │───▶│ Alerts       │
└─────────────────┘ │    └──────────────┘    │ Viewer App   │
┌─────────────────┐ │                        │ (Port 5001)  │
│ Spark App       │─┤                        └──────────────┘
└─────────────────┘ │                               │
┌─────────────────┐ │                               ▼
│ Spark Windowed  │─┘                        ┌──────────────┐
└─────────────────┘                          │   Browser    │
                                             └──────────────┘
```

## Configuration

Edit `app.py` to customize:
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka server address (default: localhost:9092)
- `KAFKA_TOPICS`: Map of topic names to processor display names
- Alert buffer size (default: 100 per topic)
- Port number (default: 5001)

## Troubleshooting

### No alerts appearing
- Ensure Kafka is running
- Verify at least one processor application is running
- Check that processors are sending data to the correct topics
- Look for error messages in the console

### Connection lost
- Check Kafka connectivity
- Verify the Flask app is running
- The app will automatically attempt to reconnect

## Complete Setup Instructions

To run the full system:

1. Start Kafka:
   ```bash
   docker-compose up -d
   ```

2. Start the stock generator:
   ```bash
   cd generator-app
   ./run.sh
   # Open browser to http://localhost:5000
   ```

3. Start one or more processors:
   ```bash
   # Flink
   cd flink-app
   ./run.sh
   
   # Flink Windowed
   cd flink-windowed-app
   ./run.sh
   
   # Spark
   cd spark-app
   ./run.sh
   
   # Spark Windowed
   cd spark-windowed-app
   ./run.sh
   ```

4. Start the alerts viewer:
   ```bash
   cd alerts-viewer-app
   ./run.sh
   # Open browser to http://localhost:5001
   ```

5. Generate stock data using the generator web UI to trigger alerts

## License

MIT
