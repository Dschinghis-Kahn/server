package net.dschinghiskahn.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.dschinghiskahn.objectdecoupler.IObjectReceiver;
import net.dschinghiskahn.server.net.INetworkData;
import net.dschinghiskahn.server.net.TcpNetworkData;
import net.dschinghiskahn.server.net.UdpNetworkData;

public class ServerTest implements IObjectReceiver<INetworkData> {

	public static final int PORT = 50000;
	public static final String HOSTNAME = "localhost";
	private Server server;
	private INetworkData networkData;
	private String read;

	@Override
	public void receiveObject(INetworkData networkData) {
		this.networkData = networkData;
	}

	public String getRead() {
		return read;
	}

	public void setRead(String read) {
		this.read = read;
	}

	@Before
	public void before() throws IOException, NoSuchMethodException {
		setRead(null);
		server = new Server(HOSTNAME, PORT);
		server.registerSocketReceiver(this);
	}

	@After
	public void after() {
		if (server != null) {
			try {
				server.shutdown();
			} catch (IOException e) {
				System.err.println(getClass().getSimpleName()+" - Server threw an exception during shutdown!\n" + e.getLocalizedMessage());
			}
		}
	}

	@Test(timeout = 1000)
	public void stop() throws IOException {
		System.out.println(getClass().getSimpleName()+" - Running test: stop()");
		server.shutdown();
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			Assert.assertFalse(thread.getName().equals("ServerWorker0"));
		}
	}

	@Test(timeout = 1000)
	public void connection() {
		System.out.println(getClass().getSimpleName()+" - Running test: connection()");
		boolean result = true;
		Socket socket = null;
		try {
			socket = new Socket(HOSTNAME, PORT);
		} catch (UnknownHostException e) {
			result = false;
		} catch (IOException e) {
			result = false;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// Intentionally left empty.
				}
			}
		}
		Assert.assertTrue(result);
	}

	@Test(timeout = 5000)
	public void manyConnections() {
		System.out.println(getClass().getSimpleName()+" - Running test: manyConnections()");
		boolean result = true;
		Socket socket = null;
		try {
			for (int i = 0; i < 1000; i++) {
				socket = new Socket(HOSTNAME, PORT);
			}
		} catch (UnknownHostException e) {
			result = false;
		} catch (IOException e) {
			result = false;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// Intentionally left empty.
				}
			}
		}
		Assert.assertTrue(result);
	}

	@Test(timeout = 3000)
	public void wrongPort() {
		System.out.println(getClass().getSimpleName()+" - Running test: wrongPort()");
		Socket socket = null;
		try {
			socket = new Socket(HOSTNAME, PORT + 1);
		} catch (IOException e) {
			Assert.assertTrue(true);
			return;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// Intentionally left empty.
				}
			}
		}
		Assert.assertTrue(false);
	}

	@Test(timeout = 1000)
	public void wrongUrl() {
		System.out.println(getClass().getSimpleName()+" - Running test: wrongUrl()");
		Socket socket = null;
		try {
			socket = new Socket("192.168.1.", PORT);
		} catch (UnknownHostException e) {
			Assert.assertTrue(true);
			return;
		} catch (IOException e) {
			Assert.assertTrue(false);
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// Intentionally left empty.
				}
			}
		}
		Assert.assertTrue(false);
	}

	@Test(timeout = 1000)
	public void udpConnectionType() throws InterruptedException, IOException {
		System.out.println(getClass().getSimpleName()+" - Running test: udpConnectionType()");
		DatagramSocket socket = new DatagramSocket();
		DatagramPacket packet = new DatagramPacket("test".getBytes(), 4, InetAddress.getByName(HOSTNAME), PORT);
		socket.send(packet);

		while (networkData == null) {
			Thread.sleep(1);
		}

		Assert.assertTrue(networkData instanceof UdpNetworkData);
		Assert.assertFalse(networkData instanceof TcpNetworkData);

		socket.close();
	}

	@Test(timeout = 1000)
	public void tcpConnectionType() throws InterruptedException, IOException {
		System.out.println(getClass().getSimpleName()+" - Running test: tcpConnectionType()");
		Socket socket = new Socket(HOSTNAME, PORT);

		while (networkData == null) {
			Thread.sleep(1);
		}

		Assert.assertTrue(networkData instanceof TcpNetworkData);
		Assert.assertFalse(networkData instanceof UdpNetworkData);

		socket.close();
	}

	@Test(timeout = 5000)
	public void shutdown() throws IOException {
		System.out.println(getClass().getSimpleName()+" - Running test: shutdown()");
		server.shutdown();
		server = new Server(HOSTNAME, PORT, 100);
		server.shutdown();
	}

	@Test(timeout = 1000)
	public void basicTcpTransfer() throws IOException, InterruptedException {
		System.out.println(getClass().getSimpleName()+" - Running test: basicTcpTransfer()");
		Socket socket = new Socket(HOSTNAME, PORT);
		OutputStream outputStream = socket.getOutputStream();
		outputStream.write(15);
		outputStream.flush();

		while (networkData == null) {
			Thread.sleep(1);
		}

		Assert.assertTrue(networkData instanceof TcpNetworkData);

		InputStream inputStream = ((TcpNetworkData) networkData).getTcpInputStream();
		int result = inputStream.read();
		Assert.assertEquals(15, result);

		socket.close();
	}

	@Test(timeout = 1000)
	public void dataTcpTransfer() throws IOException, InterruptedException {
		System.out.println(getClass().getSimpleName()+" - Running test: dataTcpTransfer()");
		Socket socket = new Socket(HOSTNAME, PORT);
		DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
		outputStream.write(15);
		outputStream.flush();

		while (networkData == null) {
			Thread.sleep(1);
		}

		Assert.assertTrue(networkData instanceof TcpNetworkData);

		DataInputStream inputStream = new DataInputStream(((TcpNetworkData) networkData).getTcpInputStream());
		int result = inputStream.read();

		Assert.assertTrue(result == 15);

		socket.close();
	}

	@Test(timeout = 1000)
	public void objectTcpTransfer() throws IOException, InterruptedException, ClassNotFoundException {
		System.out.println(getClass().getSimpleName()+" - Running test: objectTcpTransfer()");
		Socket socket = new Socket(HOSTNAME, PORT);
		ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
		outputStream.writeObject("15");
		outputStream.flush();

		while (networkData == null) {
			Thread.sleep(1);
		}

		Assert.assertTrue(networkData instanceof TcpNetworkData);

		ObjectInputStream inputStream = new ObjectInputStream(((TcpNetworkData) networkData).getTcpInputStream());
		String result = (String) inputStream.readObject();

		Assert.assertTrue("15".equals(result));

		socket.close();
	}

	@Test(timeout = 1000)
	public void simultaneousReadWrite() throws IOException, InterruptedException {
		System.out.println(getClass().getSimpleName()+" - Running test: simultaneousReadWrite()");
		Socket socket = new Socket(HOSTNAME, PORT);
		OutputStream outputStream = socket.getOutputStream();
		InputStream inputStream = socket.getInputStream();

		DataOutputStream localOutput = new DataOutputStream(outputStream);
		DataInputStream localInput = new DataInputStream(inputStream);

		new ReadThread(localInput, this).start();

		while (networkData == null) {
			Thread.sleep(10);
		}

		DataOutputStream remoteOutput = new DataOutputStream(((TcpNetworkData) networkData).getTcpOutputStream());
		DataInputStream remoteInput = new DataInputStream(((TcpNetworkData) networkData).getTcpInputStream());

		new ReadThread(remoteInput, this).start();

		localOutput.writeUTF("local_test");

		while (getRead() == null) {
			Thread.sleep(10);
		}
		Assert.assertTrue(getRead().equals("local_test"));

		setRead(null);
		remoteOutput.writeUTF("remote_test");

		while (getRead() == null) {
			Thread.sleep(10);
		}
		Assert.assertTrue(getRead().equals("remote_test"));

		socket.close();
	}

}
