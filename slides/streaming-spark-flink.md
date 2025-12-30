---
marp: true
theme: gaia
class: invert
---

# Real-Time Streaming Showdown

Apache Spark and Apache Flink

---

## What does Stream Processing enable ?

- **Real-time data processing**
- **Continuous computation**
- **Low latency**

---

## Use cases

- **Real-time analytics**
- **Event-driven applications**
- **Sensor data processing (IoT)**
- **Fraud detection**

---

## Expected features of a streaming platform

- Sources
- Aggregations
- Windowing
- Watermarks
- Managing time
- Sinks

---

## Pitfalls of streaming apps

- Time management
- State management
- "Exactly-once" delivery

---

## Do you really need "real-time"

- Extra complexity
- Is there a business case for it ?
- Consider hybrid streaming (or slow streaming)

---

## Apache Spark Streaming

- **Micro-batch processing** - Divides stream into small batches
- **Part of Apache Spark ecosystem** - Unified API with batch processing
- **Key features**:
  - Integration with Spark SQL, MLlib, GraphX
  - Fault-tolerant stateful processing
  - Support for multiple sources (Kafka, Flume, Kinesis)
  - Exactly-once semantics with structured streaming

```python
# Simple Spark Streaming example
stream = spark.readStream.format("kafka")...
query = stream.writeStream.format("console").start()
```

---

## Apache Flink

- **True streaming** - Processes events one at a time
- **Event time processing** - Handles out-of-order events
- **Key features**:
  - Low latency (milliseconds)
  - Advanced windowing and state management
  - Exactly-once processing guarantees
  - Strong support for complex event processing

```python
# Simple Flink example
env = StreamExecutionEnvironment.get_execution_environment()
stream = env.add_source(FlinkKafkaConsumer(...))
stream.map(lambda x: x.upper()).print()
```
