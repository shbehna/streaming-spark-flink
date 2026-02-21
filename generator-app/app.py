import json
import random
import time
from flask import Flask, render_template, request, jsonify, Response
from datetime import datetime
from kafka import KafkaProducer

app = Flask(__name__)

KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
KAFKA_TOPIC = 'stock-data'

# Initialize Kafka producer
producer = KafkaProducer(
    bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

print(f"Kafka producer initialized for topic '{KAFKA_TOPIC}' on {KAFKA_BOOTSTRAP_SERVERS}")


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/send', methods=['POST'])
def send_data():
    try:
        data = request.get_json()
        stock_symbol = data.get('symbol', '').upper()
        price = float(data.get('price', 0))
        
        if not stock_symbol or price <= 0:
            return jsonify({'status': 'error', 'message': 'Invalid input'}), 400
        
        stock_data = {
            'symbol': stock_symbol,
            'price': price,
            'timestamp': datetime.now().isoformat()
        }
        
        # Send to Kafka
        producer.send(KAFKA_TOPIC, value=stock_data)
        producer.flush()
        
        print(f"Sent to Kafka: {stock_data}")
        return jsonify({'status': 'success', 'data': stock_data})
    
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/send_batch', methods=['POST'])
def send_batch():
    try:
        data = request.get_json()
        stock_symbol = data.get('symbol', '').upper()
        min_price = float(data.get('minPrice', 0))
        max_price = float(data.get('maxPrice', 0))
        num_events = int(data.get('numEvents', 1))
        
        if not stock_symbol:
            return jsonify({'status': 'error', 'message': 'Invalid stock symbol'}), 400
        
        if min_price <= 0 or max_price <= 0 or min_price >= max_price:
            return jsonify({'status': 'error', 'message': 'Invalid price range'}), 400
        
        if num_events <= 0 or num_events > 10000:
            return jsonify({'status': 'error', 'message': 'Number of events must be between 1 and 10000'}), 400
        
        def generate_events():
            for i in range(num_events):
                price = round(random.uniform(min_price, max_price), 2)
                
                stock_data = {
                    'symbol': stock_symbol,
                    'price': price,
                    'timestamp': datetime.now().isoformat()
                }
                
                # Send to Kafka
                producer.send(KAFKA_TOPIC, value=stock_data)
                
                print(f"Sent to Kafka: {stock_data}")
                yield f"data: {json.dumps({'event': stock_data, 'index': i+1, 'total': num_events})}\n\n"
                
                # 1 second interval between events
                if i < num_events - 1:
                    time.sleep(1.0)
            
            # Flush all messages
            producer.flush()
            yield f"data: {json.dumps({'status': 'complete', 'count': num_events})}\n\n"
        
        return Response(generate_events(), mimetype='text/event-stream')
    
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000, use_reloader=False)
