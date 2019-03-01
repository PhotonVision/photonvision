import http.server, socketserver, os
import websockets, asyncio
import multiprocessing

HttpServerPort = 80
SocketServerPort = 8765


def run_server():
    web_dir = os.path.join(os.path.dirname(__file__), '../Site')
    os.chdir(web_dir)
    handler = http.server.SimpleHTTPRequestHandler
    with socketserver.TCPServer(("", HttpServerPort), handler) as httpd:
        print('server has started')
        httpd.serve_forever()


async def web_socket_handler(socket, path):
    print('test')
    data = await socket.recv()
    print(data)


def test():
    print('socket started')
    socket = websockets.serve(web_socket_handler, 'ws://localhost', SocketServerPort)
    print(socket)


def run_all():
    html_process = multiprocessing.Process(target=run_server)
    socket_process = multiprocessing.Process(target=test)
    html_process.start()
    socket_process.start()

