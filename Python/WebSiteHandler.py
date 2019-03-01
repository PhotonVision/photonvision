import http.server, socketserver , os
import websockets, asyncio # pip install websockets
import multiprocessing

def RunServer():
    WebDir = os.path.join(os.path.dirname(__file__), '../Site')
    os.chdir(WebDir)
    Handler = http.server.SimpleHTTPRequestHandler
    with socketserver.TCPServer(("", 80), Handler) as httpd:
        print('server has started')
        httpd.serve_forever()

async def WebSocketHandler(socket,path):
    print('test')
    data = await socket.recv()
    print (data)

def test():
    print('socket started')
    socket = websockets.serve(WebSocketHandler,'ws://localhost',8765)
    print(socket)

def RunAll():
    HTMLProcess = multiprocessing.Process(target=RunServer)
    SocketProcess = multiprocessing.Process(target= test)
    HTMLProcess.start()
    SocketProcess.start()
