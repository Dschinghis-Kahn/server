package net.dschinghiskahn.server.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Represents the UDP connection the server established.
 */
public class UdpNetworkData implements INetworkData {

    private final ByteBuffer data;
    private final InetSocketAddress sender;
    private final InetSocketAddress receiver;

    /**
     * Creates a UDP connection object.
     * 
     * @param data
     *            The data that was received.
     * @param sender
     *            The senders location.
     * @param receiver
     *            The receivers location.
     */
    public UdpNetworkData(ByteBuffer data, InetSocketAddress sender, InetSocketAddress receiver) {
        this.data = data;
        this.sender = sender;
        this.receiver = receiver;
    }

    /**
     * Returns the UDP data that was received.
     * 
     * @return The UDP data that was received.
     */
    public ByteBuffer getUdpData() {
        return data;
    }

    /**
     * Returns the {@link InetSocketAddress} of the sender.
     * 
     * @return The {@link InetSocketAddress} of the sender
     */
    @Override
    public InetSocketAddress getSender() {
        return sender;
    }

    /**
     * Returns the {@link InetSocketAddress} of the receiver.
     * 
     * @return The {@link InetSocketAddress} of the receiver
     */
    @Override
    public InetSocketAddress getReceiver() {
        return receiver;
    }

    /**
     * Returns the {@link UdpNetworkData} as string object.
     * 
     * @return The {@link UdpNetworkData} as string object.
     */
    @Override
    public String toString() {
        return "UdpNetworkData [sender=" + sender + ", receiver=" + receiver + "]";
    }

}
