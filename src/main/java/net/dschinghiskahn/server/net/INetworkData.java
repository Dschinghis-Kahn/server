package net.dschinghiskahn.server.net;

import java.io.IOException;
import java.net.InetSocketAddress;

import net.dschinghiskahn.server.Server;

/**
 * Represents a connection established with the {@link Server}.
 */
public interface INetworkData {

    /**
     * Returns the sender or origin of the connection.
     * 
     * @return The sender or origin of the connection.
     * @throws IOException
     *             Thrown on stream errors.
     */
    InetSocketAddress getSender() throws IOException;

    /**
     * Returns the receiver or target of the connection.
     * 
     * @return The receiver or target of the connection.
     * @throws IOException
     *             Thrown on stream errors.
     */
    InetSocketAddress getReceiver() throws IOException;
}
