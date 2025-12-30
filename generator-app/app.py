import socket
import json
from flask import Flask, render_template, request, jsonify
from datetime import datetime
import threading

app = Flask(__name__)

SOCKET_HOST = 'localhost'
SOCKET_PORT = 9999

server_socket = None
client_connections = []
server_running = False


def start_socket_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind((SOCKET_HOST, SOCKET_PORT))
    server_socket.listen(5)
    server_running = True
    print(f"Socket server started on {SOCKET_HOST}:{SOCKET_PORT}")
    
    def accept_connections():
        while server_running:
            try:
                server_socket.settimeout(1.0)
                client_socket, address = server_socket.accept()
                client_connections.append(client_socket)
                print(f"Client connected: {address}")
            except socket.timeout:
                continue
            except Exception as e:
                if server_running:
                    print(f"Error accepting connection: {e}")
                break
    
    thread = threading.Thread(target=accept_connections, daemon=True)
    thread.start()


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
        
        json_data = json.dumps(stock_data) + '\n'
        
        disconnected = []
        for client in client_connections:
            try:
                client.sendall(json_data.encode('utf-8'))
            except Exception as e:
                print(f"Error sending to client: {e}")
                disconnected.append(client)
        
        for client in disconnected:
            client_connections.remove(client)
            client.close()
        
        print(f"Sent: {stock_data}")
        return jsonify({'status': 'success', 'data': stock_data})
    
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


if __name__ == '__main__':
    start_socket_server()
    
    app.run(debug=True, host='0.0.0.0', port=5000, use_reloader=False)
