import type { IncomingWebsocketData } from "@/types/WebsocketDataTypes";
import type { MessageFns } from "@/types/PhotonMessage";
import { OutgoingDashboardEvent } from "@/types/PhotonMessage";

/**
 * {@link WebSocket} wrapper class that automatically reconnects to the provided host address if the connection was closed by the remote host or a connection failure.
 * Data sent and received by the Websocket is automatically encoded and decoded using msgpack.
 */
export class AutoReconnectingWebsocket {
  private readonly serverAddress: string | URL;
  private websocket: WebSocket | null | undefined;

  private readonly onConnect: () => void;
  private readonly onData: (data: IncomingWebsocketData) => void;
  private readonly onDisconnect: () => void;

  /**
   * Create an AutoReconnectingWebsocket
   *
   * @param serverAddress address of the websocket
   * @param onConnect action to run on websocket connection (when the websocket changes to the OPEN state)
   * @param onData decoded websocket message data consumer. The data is automatically decoded by msgpack.
   * @param onDisconnect action to run on websocket disconnection (when the websocket changes to the CLOSED state)
   */
  constructor(
    serverAddress: string | URL,
    onConnect: () => void,
    onData: (data: IncomingWebsocketData) => void,
    onDisconnect: () => void
  ) {
    this.serverAddress = serverAddress;

    this.onConnect = onConnect;
    this.onData = onData;
    this.onDisconnect = onDisconnect;

    this.initializeWebsocket();
  }

  /**
   * Send data over the websocket. This is a no-op if the websocket is not in the OPEN state.
   *
   * @param data data to send
   * @param fns protobuf functions for encoding
   * @see isConnected
   *
   */
  send<T>(data: T, fns: Pick<MessageFns<T>, 'encode'>): void {
    if (!this.isConnected()) return;

    const bin = fns.encode(data).finish();
    this.websocket?.send(bin);
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
    this.websocket = new WebSocket(this.serverAddress);
    this.websocket.binaryType = "arraybuffer";

    this.websocket.onopen = () => {
      console.debug("[WebSocket] Websocket Open");
      this.onConnect();
    };
    this.websocket.onmessage = (event: MessageEvent) => {
      // Decode from OutgoingDashboardEvent
      const message = OutgoingDashboardEvent.decode(new Uint8Array(event.data));

      this.onData(message as IncomingWebsocketData);
    };
    this.websocket.onclose = (event: CloseEvent) => {
      this.onDisconnect();

      this.websocket = null;

      console.info("[WebSocket] The WebSocket was closed. Will reattempt in 500 milliseconds.", event.reason);
      setTimeout(this.initializeWebsocket.bind(this), 500);
    };
    this.websocket.onerror = () => {
      this.websocket?.close();
    };

    console.debug(`[WebSocket] Attempting to initialize Websocket connection to ${this.serverAddress}`);
  }
}
