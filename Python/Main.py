import http.server, socketserver , os
import websockets, asyncio # pip install websockets

def RunServer():
    WebDir = os.path.join(os.path.dirname(__file__), '../Site')
    os.chdir(WebDir)
    Handler = http.server.SimpleHTTPRequestHandler
    httpd = socketserver.TCPServer(("", 80), Handler)
    print('server has started')
    httpd.serve_forever()


# def WebSocketHandler(socket,path):
#     data = await websocket.recv()
#     print (data)

#start_socket = websockets.Serve(WebSocketHandler,'localhost',8765)
RunServer()