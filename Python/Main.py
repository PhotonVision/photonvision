import http.server, socketserver , os

def RunServer():
    WebDir = os.path.join(os.path.dirname(__file__), '../Site')
    os.chdir(WebDir)
    Handler = http.server.SimpleHTTPRequestHandler
    httpd = socketserver.TCPServer(("", 80), Handler)
    httpd.serve_forever()


RunServer()