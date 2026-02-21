# Streaming Stock Price Alerts - Flink & Spark

A comprehensive demonstration of real-time stream processing using Apache Flink and Apache Spark, featuring stock price monitoring with alert generation and a real-time web dashboard.

## 🏗️ Architecture

```
┌─────────────────┐                                    ┌──────────────────┐
│  Generator App  │                                    │   Kafka Topics   │
│  (Port 5000)    │────▶ stock-data ───────────────┬──▶│  flink-alerts    │
└─────────────────┘                                 │   │  spark-alerts    │
       │                                            │   │  flink-windowed  │
       │ Generate stock                             │   │  spark-windowed  │
       │ price events                               │   └──────────────────┘
       ▼                                            │            │
┌─────────────────┐                                 │            │
│  Kafka Broker   │                                 │            ▼
│  localhost:9092 │                                 │   ┌──────────────────┐
└─────────────────┘                                 │   │ Alerts Viewer    │
       │                                            │   │   (Port 5001)    │
       │ Consume stock-data                         │   └──────────────────┘
       ▼                                            │            │
┌──────────────────────────────────────────────────┐   │   Real-time Web UI
│              Stream Processors                   │   │   displaying alerts
│                                                  │   │
│  ┌────────────────┐  ┌──────────────────────┐  │   │
│  │  Flink App     │  │  Flink Windowed App  │  │───┘
│  │  (Point-in-    │  │  (10-sec tumbling    │  │
│  │   time alerts) │  │   window alerts)     │  │
│  └────────────────┘  └──────────────────────┘  │
│                                                  │
│  ┌────────────────┐  ┌──────────────────────┐  │
│  │  Spark App     │  │  Spark Windowed App  │  │───┐
│  │  (Point-in-    │  │  (10-sec tumbling    │  │   │
│  │   time alerts) │  │   window alerts)     │  │   │
│  └────────────────┘  └──────────────────────┘  │   │
└──────────────────────────────────────────────────┘   │
                                                       │
        All produce alerts to Kafka ───────────────────┘
```

## 📦 Components

### 1. Generator App (Python/Flask)
- **Location**: `generator-app/`
- **Port**: 5000
- **Purpose**: Web UI to generate stock price events and send them to Kafka
- **Features**:
  - Send single stock price events
  - Send batch events with configurable price ranges
  - Real-time event history

### 2. Stream Processors

#### Flink App
- **Location**: `flink-app/`
- **Type**: Point-in-time processing
- **Alert Trigger**: When price changes by ≥5% between consecutive events
- **Output**: Kafka topic `flink-alerts`

#### Flink Windowed App
- **Location**: `flink-windowed-app/`
- **Type**: Windowed processing (10-second tumbling windows)
- **Alert Trigger**: When average price changes by ≥5% between consecutive windows
- **Output**: Kafka topic `flink-windowed-alerts`

#### Spark App
- **Location**: `spark-app/`
- **Type**: Point-in-time processing with stateful operations
- **Alert Trigger**: When price changes by ≥5% between consecutive events
- **Output**: Kafka topic `spark-alerts`

#### Spark Windowed App
- **Location**: `spark-windowed-app/`
- **Type**: Windowed processing (10-second tumbling windows)
- **Alert Trigger**: When average price changes by ≥5% between consecutive windows
- **Output**: Kafka topic `spark-windowed-alerts`

### 3. Alerts Viewer App (Python/Flask)
- **Location**: `alerts-viewer-app/`
- **Port**: 5001
- **Purpose**: Real-time web dashboard to monitor alerts from all processors
- **Features**:
  - Live streaming of alerts using Server-Sent Events (SSE)
  - Separate panels for each processor
  - Alert statistics and counts
  - Pause/resume and clear functionality
  - Automatic reconnection

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose (for Kafka)
- Python 3.7+ (for Flask apps)
- Java 21+ (for Spark apps)
- Java 25 (for Flink apps)
- Maven (for building Java apps)

### Step 1: Start Kafka
```bash
docker-compose up -d
```

This starts Zookeeper and Kafka on `localhost:9092`.

### Step 2: Start the Generator App
```bash
cd generator-app
pip install -r requirements.txt
python app.py
```

Open browser: http://localhost:5000

### Step 3: Start Stream Processors

You can start any combination of the 4 processors:

**Flink App:**
```bash
cd flink-app
./run.sh
```

**Flink Windowed App:**
```bash
cd flink-windowed-app
./run.sh
```

**Spark App:**
```bash
cd spark-app
./run.sh
```

**Spark Windowed App:**
```bash
cd spark-windowed-app
./run.sh
```

### Step 4: Start the Alerts Viewer
```bash
cd alerts-viewer-app
pip install -r requirements.txt
python app.py
```

Open browser: http://localhost:5001

### Step 5: Generate Stock Data
1. Go to the Generator UI at http://localhost:5000
2. Use "Multiple events" tab
3. Enter:
   - Symbol: AAPL
   - Min Price: 100
   - Max Price: 200
   - Number of Events: 100
4. Click "Send Batch Events"
5. Watch alerts appear in the Alerts Viewer at http://localhost:5001

## 🎯 How It Works

### Alert Generation Logic

All processors monitor stock prices and generate alerts when significant price changes occur:

1. **Point-in-time processors** (Flink & Spark):
   - Track the last price for each stock symbol using keyed state
   - Compare each new price with the previous price
   - Generate alert if absolute percent change ≥ 5%

2. **Windowed processors** (Flink Windowed & Spark Windowed):
   - Collect all events in 10-second tumbling windows
   - Calculate average price for the window
   - Compare with previous window's average
   - Generate alert if absolute percent change ≥ 5%

### Data Flow

1. **Stock Data**: Generator → Kafka (`stock-data` topic) → Processors
2. **Alerts**: Processors → Kafka (4 alert topics) → Alerts Viewer → Browser

### State Management

- **Flink**: Uses Flink's built-in keyed state (ValueState)
- **Spark**: Uses Spark Structured Streaming's mapGroupsWithState

## 📊 Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `stock-data` | Generator App | All 4 Processors | Stock price events |
| `flink-alerts` | Flink App | Alerts Viewer | Point-in-time alerts |
| `flink-windowed-alerts` | Flink Windowed App | Alerts Viewer | Windowed alerts |
| `spark-alerts` | Spark App | Alerts Viewer | Point-in-time alerts |
| `spark-windowed-alerts` | Spark Windowed App | Alerts Viewer | Windowed alerts |

## 🔧 Configuration

### Generator App
- Edit `generator-app/app.py`:
  - Port: Line 108 (`port=5000`)
  - Kafka servers: Line 10

### Processors
- Edit each `StockProcessor.java`:
  - Kafka servers: `KAFKA_BOOTSTRAP_SERVERS`
  - Alert threshold: `ALERT_THRESHOLD` (default: 0.05 = 5%)
  - Window size: `WINDOW_SIZE_SECONDS` or `WINDOW_DURATION` (windowed apps only)

### Alerts Viewer
- Edit `alerts-viewer-app/app.py`:
  - Port: Line 130 (`port=5001`)
  - Kafka servers: Line 11
  - Buffer size: Line 19 (`maxlen=100`)

## 📝 Building the Java Apps

Each processor app includes a Maven build:

```bash
cd <app-directory>
mvn clean package
```

The `run.sh` script automatically handles building and running.

## 🛑 Stopping Everything

1. Stop all Python apps: Press `Ctrl+C` in their terminals
2. Stop Java apps: Press `Ctrl+C` in their terminals
3. Stop Kafka:
   ```bash
   docker-compose down
   ```

## 🔍 Monitoring

### Check Kafka Topics
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### View Kafka Messages
```bash
# View stock data
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic stock-data --from-beginning

# View alerts from Flink
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic flink-alerts --from-beginning
```

## 🎨 Alert Format

Alerts are formatted as ASCII art boxes:

```
╔═══════════════════════════════════════════════════════════════════════════╗
║                              PRICE ALERT                                  ║
╠═══════════════════════════════════════════════════════════════════════════╣
║  Symbol:         AAPL                                                     ║
║  Event Time:     2026-02-21T10:30:45.123                                 ║
║  Current Price:  $157.50                                                  ║
║  Previous Price: $150.00                                                  ║
║  Change:         +5.00%                                                   ║
╚═══════════════════════════════════════════════════════════════════════════╝
```

## 🤝 Contributing

This is a demonstration project. Feel free to:
- Add more processors
- Modify alert thresholds
- Enhance the web UIs
- Add more sophisticated windowing strategies

## 📄 License

MIT

## 🎓 Learning Objectives

This project demonstrates:
- Real-time stream processing with Flink and Spark
- Kafka as a message broker
- Stateful stream processing
- Windowed aggregations
- Kafka Sink/Source connectors
- Server-Sent Events (SSE) for real-time web updates
- Flask web applications
- Multi-component distributed systems
