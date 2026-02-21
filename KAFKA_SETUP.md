# Kafka Setup Guide

This project has been migrated from socket-based streaming to **Apache Kafka** for better reliability, durability, and scalability.

## Prerequisites

- Docker Desktop installed and running
- Java (for Spark and Flink apps)
- Python 3.x (for generator app)
- Maven (for building Java apps)

## Quick Start

### 1. Start Kafka

From the project root directory:

```bash
docker-compose up -d
```

This starts:
- **Zookeeper** on port 2181
- **Kafka** on port 9092

Verify Kafka is running:
```bash
docker ps
```

You should see both `kafka` and `zookeeper` containers running.

### 2. Install Generator App Dependencies

```bash
cd generator-app
pip install -r requirements.txt
```

### 3. Start the Generator App

```bash
python app.py
```

Access the web UI at: http://localhost:5000

### 4. Build and Run Spark/Flink Apps

#### Spark App (Stateful)
```bash
cd spark-app
mvn clean package
.\run.ps1
```

#### Spark Windowed App
```bash
cd spark-windowed-app
mvn clean package
.\run.ps1
```

#### Flink App (Stateful)
```bash
cd flink-app
mvn clean package
.\run.ps1
```

#### Flink Windowed App
```bash
cd flink-windowed-app
mvn clean package
.\run.ps1
```

## Configuration

All applications are configured to connect to:
- **Kafka Bootstrap Servers**: `localhost:9092`
- **Kafka Topic**: `stock-data`

### Kafka Topic

The topic `stock-data` is auto-created when the first message is sent. If you want to manually create it:

```bash
docker exec -it kafka kafka-topics --create --topic stock-data --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

To list topics:
```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

To view messages (for debugging):
```bash
docker exec -it kafka kafka-console-consumer --topic stock-data --from-beginning --bootstrap-server localhost:9092
```

## Benefits of Kafka vs Sockets

✅ **Durability**: Messages are persisted to disk  
✅ **Fault Tolerance**: Automatic recovery from failures  
✅ **Replayability**: Can replay historical data  
✅ **Decoupling**: Producer and consumers operate independently  
✅ **Scalability**: Horizontal scaling with partitions  
✅ **Multiple Consumers**: Both Spark and Flink can consume simultaneously  

## Stopping Kafka

```bash
docker-compose down
```

To remove volumes (deletes all data):
```bash
docker-compose down -v
```

## Troubleshooting

### Kafka Connection Errors

If you see connection errors, ensure:
1. Docker containers are running: `docker ps`
2. Port 9092 is not in use by another process
3. Docker Desktop is running

### Generator App Can't Connect to Kafka

```bash
pip install --upgrade kafka-python
```

### Maven Build Errors

Clean and rebuild:
```bash
mvn clean install -U
```
