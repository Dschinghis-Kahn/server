package net.dschinghiskahn.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import net.dschinghiskahn.objectdecoupler.IObjectReceiver;
import net.dschinghiskahn.objectdecoupler.ObjectDecoupler;
import net.dschinghiskahn.server.net.INetworkData;
import net.dschinghiskahn.server.net.TcpNetworkData;
import net.dschinghiskahn.server.net.UdpNetworkData;
import net.dschinghiskahn.worker.AbstractWorker;

/**
 * The server listens to a given hostname and port and can handle TCP as well as
 * UDP connections.
 */
public class Server {

	public static final String CONNECTION_ACCEPT = "CONNECTION_ACCEPTED";
	private final ObjectDecoupler<INetworkData> connectionQueue;
	private final ServerSocketChannel tcpChannel;
	private final List<WorkerThread> threads;
	private final DatagramChannel udpChannel;
	private final Selector socketSelector;
	private boolean isServerRunning;

	/**
	 * Creates a new Server listening on the given port.
	 * 
	 * @param port The port to listen on.
	 * @throws IOException
	 */
	public Server(int port) throws IOException {
		this(null, port, 1, false);
	}

	/**
	 * Creates a new Server listening on the given port.
	 * 
	 * @param url  The interface/ip to listen on (null refers to the wildcard
	 *             address).
	 * @param port The port to listen on.
	 * @throws IOException
	 */
	public Server(String url, int port) throws IOException {
		this(url, port, 1, false);
	}

	/**
	 * Creates a new Server listening on the given port.
	 * 
	 * @param url        The interface/ip to listen on (null refers to the wildcard
	 *                   address).
	 * @param port       The port to listen on.
	 * @param numThreads Number of worker threads to start for connection handling.
	 * @throws IOException
	 */
	public Server(String url, int port, int numThreads) throws IOException {
		this(url, port, numThreads, false);
	}

	/**
	 * Creates a new Server listening on the given port.
	 * 
	 * @param url        The interface/ip to listen on (null refers to the wildcard
	 *                   address).
	 * @param port       The port to listen on.
	 * @param numThreads Number of worker threads to start for connection handling.
	 * @param isDaemon   Controls if the threads are started in daemon mode.
	 * @throws IOException
	 */
	public Server(String url, int port, int numThreads, boolean isDaemon) throws IOException {
		connectionQueue = new ObjectDecoupler<INetworkData>();
		threads = new ArrayList<Server.WorkerThread>();
		tcpChannel = ServerSocketChannel.open();
		if (url == null) {
			tcpChannel.socket().bind(new InetSocketAddress(port));
		} else {
			tcpChannel.socket().bind(new InetSocketAddress(url, port));
		}
		tcpChannel.configureBlocking(false);

		udpChannel = DatagramChannel.open();
		if (url == null) {
			udpChannel.socket().bind(new InetSocketAddress(port));
		} else {
			udpChannel.socket().bind(new InetSocketAddress(url, port));
		}
		udpChannel.configureBlocking(false);

		socketSelector = Selector.open();
		tcpChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		udpChannel.register(socketSelector, SelectionKey.OP_READ);

		WorkerThread thread;
		for (int i = 0; i < numThreads; i++) {
			thread = new WorkerThread(isDaemon); // NOPMD
			thread.start();
			threads.add(thread);
		}
		isServerRunning = true;
		hookServerStarted(url, port, numThreads, isDaemon);
	}

	protected void hookServerStarted(String url, int port, int numThreads, boolean isDaemon) {
	}

	/**
	 * Shuts down the server. It may take some time until all worker threads are
	 * closed.
	 * 
	 * @throws IOException
	 */
	public void shutdown() throws IOException {
		isServerRunning = false;

		for (int i = 0; i < threads.size(); i++) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// Intentionally left empty
			}
			socketSelector.wakeup();
		}

		for (WorkerThread workerThread : threads) {
			workerThread.stop();
		}

		if (socketSelector.isOpen()) {
			socketSelector.close();
		}
		if (tcpChannel.isOpen()) {
			tcpChannel.close();
		}
		if (udpChannel.isOpen()) {
			udpChannel.close();
		}

		connectionQueue.stop();
		hookServerStopped();
	}

	protected void hookServerStopped() {
	}

	/**
	 * A worker object to accept incoming connections.
	 */
	private class WorkerThread extends AbstractWorker<Object> {

		/**
		 * Creates a new worker accepting connections.
		 */
		WorkerThread(boolean isDaemon) {
			super("ServerWorker", isDaemon);
		}

		@Override
		protected void doWork(Object item) {
			try {
				socketSelector.select();
				for (SelectionKey key : socketSelector.selectedKeys()) {
					if (key.isAcceptable() && key.channel() == tcpChannel) {
						acceptTcpConnection();
					} else if (key.isReadable() && key.channel() == udpChannel && key.channel().isOpen()) {
						acceptUdpConnection();
					}
				}
			} catch (Exception e) {
				hookExceptionDuringWork(e);
			}
		}

		protected void hookExceptionDuringWork(Exception e) {
		}

		private void acceptUdpConnection() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
			synchronized (Server.class) {
				udpChannel.receive(buffer);
			}
			if (buffer.flip().hasRemaining()) {
				UdpNetworkData networkData = new UdpNetworkData(buffer,
						(InetSocketAddress) udpChannel.getRemoteAddress(),
						(InetSocketAddress) udpChannel.getLocalAddress());
				connectionQueue.add(networkData);
				hookPostUdpConnectionAccepted(networkData);
			}
		}

		protected void hookPostUdpConnectionAccepted(UdpNetworkData networkData) {
		}

		private void acceptTcpConnection() throws IOException {
			SocketChannel channel = tcpChannel.accept();
			if (channel != null) {
				TcpNetworkData networkData = new TcpNetworkData(channel);
				connectionQueue.add(networkData);
				hookPostTcpConnectionAccepted(networkData);
			}
		}

		protected void hookPostTcpConnectionAccepted(TcpNetworkData networkData) {
		}

		@Override
		protected Object getWork() {
			return null;
		}

		@Override
		protected boolean isWorkAvailable() {
			return isServerRunning;
		}

		@Override
		protected Long getSuspendTime() {
			return null;
		}

	}

	/**
	 * Registers an {@link IObjectReceiver}. The receiver will receive all incoming
	 * {@link INetworkData} objects.
	 * 
	 * @param receiver The receiver to register with the {@link Server}.
	 */
	public void registerSocketReceiver(IObjectReceiver<INetworkData> receiver) {
		connectionQueue.registerObjectReceiver(receiver);
	}

	/**
	 * Unregisters an {@link IObjectReceiver}. The receiver will no longer receive
	 * {@link INetworkData} objects. If no receiver is registered connections will
	 * be accepted and "cached" until a receiver is registered.
	 * 
	 * @param receiver The receiver to unregister.
	 */
	public void unregisterSocketReceiver(IObjectReceiver<INetworkData> receiver) {
		connectionQueue.unregisterObjectReceiver(receiver);
	}
}
