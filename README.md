# server
A universal TCP/UDP server implementation.

##Features
- Easy to use
- Can handle TCP and UDP connections
- Is scalable


##Example

```Java
public class CoreServer implements IObjectReceiver<INetworkData> {

    public CoreServer() throws IOException {
        /*
         * Creates a Server object with 10 threads accepting incoming
         * connections.
         */
        Server server = new Server("192.168.0.4", 10000, 10, false);
        server.registerSocketReceiver(this);
    }

    @Override
    public void receiveObject(INetworkData networkData) {
        if (networkData instanceof TcpNetworkData) {
            // A TCP connection was recevied
            TcpNetworkData tcpNetworkData = (TcpNetworkData) networkData;
            OutputStream output = tcpNetworkData.getTcpOutputStream();
            InputStream input = tcpNetworkData.getTcpInputStream();

            // Do something with the input and output streams.
        } else {
            // A UDP connection was recevied
            UdpNetworkData udpNetworkData = (UdpNetworkData) networkData;
            ByteBuffer packet = udpNetworkData.getUdpData();

            // Do something with the data packet.
        }

    }
}
```
