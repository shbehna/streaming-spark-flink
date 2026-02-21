import json
import threading
from flask import Flask, render_template, Response
from kafka import KafkaConsumer
from collections import deque
import time

app = Flask(__name__)

KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
KAFKA_TOPICS = {
    'flink-alerts': 'Flink',
    'flink-windowed-alerts': 'Flink Windowed',
    'spark-alerts': 'Spark',
    'spark-windowed-alerts': 'Spark Windowed'
}

# Store recent alerts in memory (last 100 per topic)
alerts_buffer = {topic: deque(maxlen=100) for topic in KAFKA_TOPICS.keys()}
alerts_lock = threading.Lock()

# Global consumer threads
consumer_threads = []


def consume_alerts(topic, processor_name):
    """Consume alerts from a Kafka topic and store them in the buffer."""
    try:
        consumer = KafkaConsumer(
            topic,
            bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
            value_deserializer=lambda m: m.decode('utf-8'),
            auto_offset_reset='latest',
            enable_auto_commit=True,
            group_id=f'alerts-viewer-{topic}'
        )
        
        print(f"Started consuming from topic '{topic}' ({processor_name})")
        
        for message in consumer:
            alert_data = {
                'processor': processor_name,
                'topic': topic,
                'alert': message.value,
                'timestamp': time.time()
            }
            
            with alerts_lock:
                alerts_buffer[topic].append(alert_data)
            
            print(f"[{processor_name}] Received alert")
    
    except Exception as e:
        print(f"Error consuming from {topic}: {e}")


def start_consumers():
    """Start consumer threads for all topics."""
    for topic, processor_name in KAFKA_TOPICS.items():
        thread = threading.Thread(
            target=consume_alerts,
            args=(topic, processor_name),
            daemon=True
        )
        thread.start()
        consumer_threads.append(thread)
        print(f"Started consumer thread for {topic}")


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/stream')
def stream():
    """Server-Sent Events endpoint to stream alerts to the browser."""
    def generate():
        last_sent = {topic: 0 for topic in KAFKA_TOPICS.keys()}
        
        while True:
            alerts_to_send = []
            
            with alerts_lock:
                for topic in KAFKA_TOPICS.keys():
                    buffer = alerts_buffer[topic]
                    buffer_list = list(buffer)
                    
                    # Send any new alerts we haven't sent yet
                    for i in range(last_sent[topic], len(buffer_list)):
                        alerts_to_send.append(buffer_list[i])
                    
                    last_sent[topic] = len(buffer_list)
            
            # Send all new alerts
            for alert_data in alerts_to_send:
                yield f"data: {json.dumps(alert_data)}\n\n"
            
            # Send heartbeat
            if not alerts_to_send:
                yield f": heartbeat\n\n"
            
            time.sleep(0.5)
    
    return Response(generate(), mimetype='text/event-stream')


@app.route('/history')
def history():
    """Get recent alerts history."""
    all_alerts = []
    
    with alerts_lock:
        for topic in KAFKA_TOPICS.keys():
            all_alerts.extend(list(alerts_buffer[topic]))
    
    # Sort by timestamp (most recent first)
    all_alerts.sort(key=lambda x: x['timestamp'], reverse=True)
    
    return json.dumps(all_alerts[:50])


if __name__ == '__main__':
    print("Starting Alerts Viewer Application...")
    print(f"Kafka Bootstrap Servers: {KAFKA_BOOTSTRAP_SERVERS}")
    print(f"Monitoring topics: {list(KAFKA_TOPICS.keys())}")
    
    # Start Kafka consumers in background threads
    start_consumers()
    
    # Start Flask app
    app.run(debug=False, host='0.0.0.0', port=5001, use_reloader=False, threaded=True)
