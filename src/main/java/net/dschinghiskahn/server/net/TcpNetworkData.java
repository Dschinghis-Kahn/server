package net.dschinghiskahn.server.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

/**
 * Represents the TCP connection the server established.
 */
public class TcpNetworkData implements INetworkData {

    private final SocketChannel channel;
    private final InetSocketAddress sender;
    private final InetSocketAddress receiver;

    /**
     * Creates a TCP connection object with the given {@link SocketChannel}.
     * 
     * @param channel
     *            The {@link SocketChannel} to create the {@link InputStream}
     *            and {@link OutputStream}.
     * @throws IOException
     */
    public TcpNetworkData(SocketChannel channel) throws IOException {
        this.channel = channel;
        this.sender = (InetSocketAddress) channel.getRemoteAddress();
        this.receiver = (InetSocketAddress) channel.getLocalAddress();
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
     * Returns the {@link InputStream} for this connection.
     * 
     * @return The {@link InputStream} for this connection.
     */
    public InputStream getTcpInputStream() {
        return Channels.newInputStream(wrapChannel(channel));
    }

    /**
     * Returns the {@link OutputStream} for this connection.
     * 
     * @return The {@link OutputStream} for this connection.
     */
    public OutputStream getTcpOutputStream() {
        return Channels.newOutputStream(wrapChannel(channel));
    }

    /**
     * Wraps a {@link ByteChannel} in order to provide a separate object
     * instance for synchronization. This is a workaround for the nasty
     * simultaneous read/write blocking bug.
     */
    private ByteChannel wrapChannel(final ByteChannel channel) {
        return new ByteChannel() {

            /**
             * Writes to the wrapped {@link ByteChannel}.
             * 
             * @param src
             *            The source {@link ByteBuffer}.
             * @throws IOException
             *             Thrown on channel write errors.
             */
            @Override
            public int write(ByteBuffer src) throws IOException {
                return channel.write(src);
            }

            /**
             * Reads from the wrapped {@link ByteChannel}.
             * 
             * @param dst
             *            The destination {@link ByteBuffer}.
             * @throws IOException
             *             Thrown on channel read errors.
             */
            @Override
            public int read(ByteBuffer dst) throws IOException {
                return channel.read(dst);
            }

            /**
             * Returns true if the wrapped channel is open, false otherwise.
             * 
             * @retrun True if the wrapped channel is open, false otherwise.
             */
            @Override
            public boolean isOpen() {
                return channel.isOpen();
            }

            /**
             * Closed the wrapped channel.
             * 
             * @throws IOException
             *             Thrown on channel read errors.
             */
            @Override
            public void close() throws IOException {
                channel.close();
            }
        };
    }

    /**
     * Returns the {@link TcpNetworkData} as string object.
     * 
     * @return The {@link TcpNetworkData} as string object.
     */
    @Override
    public String toString() {
        return "TcpNetworkData [sender=" + sender + ", receiver=" + receiver + "]";
    }

}
