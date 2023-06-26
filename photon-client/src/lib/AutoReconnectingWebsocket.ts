/**
 * WebSocket class that automatically reconnects to the provided host address if the connection was closed.
 */
export class AutoReconnectingWebsocket {
  private readonly serverAddress: string | URL
  private websocket: WebSocket | null | undefined

  private readonly onConnect: () => void
  private readonly onData: (data: MessageEvent) => void
  private readonly onDisconnect: () => void

  /**
   * Create an AutoReconnectingWebsocket
   *
   * @param serverAddress address of the websocket
   * @param onConnect action to run on websocket connection (when the websocket changes to the OPEN state)
   * @param onData websocket message consumer
   * @param onDisconnect action to run on websocket disconnection (when the websocket changes to the CLOSED state)
   */
  constructor(serverAddress: string | URL, onConnect: () => void, onData: (data: MessageEvent) => void, onDisconnect: () => void) {
    this.serverAddress = serverAddress;

    this.onConnect = onConnect
    this.onData = onData
    this.onDisconnect = onDisconnect

    this.initializeWebsocket();
  }

  /**
   * Check if the WebSocket is OPEN and connected
   */
  isConnected(): boolean {
    return this.websocket === null || this.websocket === undefined
      ? false
      : this.websocket.readyState === WebSocket.OPEN;
  }

  /**
   * Handles the creation of the websocket and the binding of the action consumers.
   *
   * @private
   */
  private initializeWebsocket() {
    this.websocket = new WebSocket(this.serverAddress)
    this.websocket.binaryType = "arraybuffer"

    this.websocket.onopen = () => {
      console.debug("[WebSocket] Websocket Open");
      this.onConnect();
    }
    this.websocket.onmessage = this.onData
    this.websocket.onclose = (event: CloseEvent) => {
      this.onDisconnect()

      this.websocket = null

      console.info("[WebSocket] The WebSocket was closed. Reconnect attempt will be attempted in 500 millisecond.", event.reason);
      setTimeout(this.initializeWebsocket, 500);
    }
    this.websocket.onerror = (event: Event) => {
      console.error("[WebSocket] Websocket Error Occurred", event.toString())
      this.websocket?.close()
    }

    console.debug("[WebSocket] Initializing Websocket")
  }
}
