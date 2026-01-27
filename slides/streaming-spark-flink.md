---
marp: true
theme: gaia
---

<style>
section.centered-image {
  display: flex;
  flex-direction: column;
  justify-content: center;
}
section.centered-image p {
  text-align: center;
}
section.bottom-right {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
}
section.bottom-right .content {
  text-align: left;
  margin-bottom: 20px;
  align-self: flex-end;
  font-size: 0.6em;
}
section.bottom-right .content p {
  margin: 5px 0;
}
section.comparison {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  padding: 40px 20px;
  align-content: start;
}
section.comparison h2 {
  grid-column: 1 / -1;
}
section.comparison .framework {
  flex: 1;
  text-align: left;
  padding: 0 30px;
}
section.comparison .framework img {
  height: 150px;
  margin-bottom: 5px;
}
section.comparison .framework ul,
section.comparison .framework li {
  font-size: 0.9em;
}
</style>

<!-- _class: bottom-right -->

# Real-Time Streaming Showdown

![bg w:400](./img/spark-logo.png)
![bg w:400](./img/flink-logo.png)

<div class="content">

**Sherif Behna**
Data Architect
Hikari Data inc.

</div>

---

## What is stream processing ?

- Stream : Continuous sequence of events over time
- Stream processing : Processing these events to get insights

---

## Typical use cases

- **Real-time analytics**
- **Event-driven applications**
- **Sensor data processing (IoT)**
- **Business monitoring and alerting**
- **Anomaly and fraud detection**

---

## Simple example

![bg center w:1080](./img/use-case.png)

---

<!-- _class: centered-image -->

## Simple example (visual)

![w:1020](./img/state-management.png)

---

## Apache Spark

- **Micro-batch processing** - Divides stream into small batches
- **APIs**:
  - DStream API (legacy)
  - Structured Streaming (DataFrame API)
  - Spark SQL integration
- **Languages**: Scala, Java, Python, R, SQL
- **Additional libraries**: MLlib, GraphX, Spark Connect, Pandas Spark
- **Mature ecosystem**

---

<!-- _class: centered-image -->

## Spark Architecture

![w:720](./img/spark-arch.png)

---

## Apache Flink

- **Designed for real-time stream processing**
- **Processes events one at a time (true streaming)**
- **APIs**:
  - DataStream API (low-level, event-by-event processing)
  - Table API (relational operations)
  - Flink SQL
- **Languages**: Scala, Java, Python (PyFlink), SQL
- **Additional libraries**: Flink CDC, Flink ML, Flink CEP, Flink Agents
- **Mature ecosystem (less than Spark but mature enough)**

---

<!-- _class: centered-image -->

## Flink Architecture

![w:720](./img/flink-arch.png)

---

<!-- _class: centered-image -->

## Micro-batch vs continuous streaming

![w:720](./img/micro-batch.png)

(mettre les logos dans l'image)

---

## Expected features of a streaming platform

- Supports multiple sources and sinks
- Mapping / transformation
- Filtering
- Grouping
- Aggregation
- Windowing (tumbling / sliding)
- Watermarks
- Scalability (distributed computing)

(mettre tableau côte à côte)

---

## Performance

(ajouted truc de IEEE)

---

## Stateful vs stateless operators

| Stateless | Stateful |
|-----------|----------|
| Transformations (map or flatMap) | Aggregations (sum, count, avg, custom) |
| Filters | Grouping |
| Projections (select) | Windows (tumbling and sliding) |
| Stream to static join | Stream to stream join |
| Partitioning | Sessionalizing |

---

## Pitfalls of streaming apps

- Time management (event time vs processing time)
- State management
- Delivery semantics (at-most once, at-least once, exactly once)
- Fault tolerance
- Backpressure

(Comparer spark et flink)

---

## Do you really need "real-time"

- Real-time means milliseconds to a few seconds (max 5s end-to-end)
- Extra complexity
- Is there a business case for it ?

---

<!-- _class: comparison -->

## And the winner is...

<div class="framework">

![spark-logo](./img/spark-logo.png)

- Mature and stable ecosystem
- Ease of use
- Data engineering / ML use cases

</div>

<div class="framework">

![flink-logo](./img/flink-logo.png)

- Low latency and predictable performance
- Responsive event-driven apps
- Robust state management and fault tolerence

</div>
