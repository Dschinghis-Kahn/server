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

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.dschinghiskahn.objectdecoupler.IObjectReceiver;
import net.dschinghiskahn.server.net.INetworkData;
import net.dschinghiskahn.server.net.TcpNetworkData;
import net.dschinghiskahn.server.net.UdpNetworkData;

@SuppressWarnings({ "resource", "PMD" })
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

    @BeforeClass
    public static void init() {
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getRootLogger().removeAllAppenders();
        ConsoleAppender appender = new ConsoleAppender();
        appender.setLayout(new PatternLayout("%d %-5p: %m%n"));
        appender.activateOptions();
        Logger.getRootLogger().addAppender(appender);
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
            server.shutdown();
        }
    }

    @Test(timeout = 1000)
    public void stop() {
        Logger.getLogger(getClass()).info("Running test: stop()");
        server.shutdown();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            Assert.assertFalse(thread.getName().equals("ServerWorker0"));
        }
    }

    @Test(timeout = 1000)
    public void connection() {
        Logger.getLogger(getClass()).info("Running test: connection()");
        boolean result = true;
        try {
            new Socket(HOSTNAME, PORT);
        } catch (UnknownHostException e) {
            result = false;
        } catch (IOException e) {
            result = false;
        }
        Assert.assertTrue(result);
    }

    @Test(timeout = 5000)
    public void manyConnections() {
        Logger.getLogger(getClass()).info("Running test: manyConnections()");
        boolean result = true;
        try {
            for (int i = 0; i < 1000; i++) {
                new Socket(HOSTNAME, PORT);
            }
        } catch (UnknownHostException e) {
            result = false;
        } catch (IOException e) {
            result = false;
        }
        Assert.assertTrue(result);
    }

    @Test(timeout = 1000)
    public void wrongPort() {
        Logger.getLogger(getClass()).info("Running test: wrongPort()");
        try {
            new Socket(HOSTNAME, PORT + 1);
        } catch (IOException e) {
            Assert.assertTrue(true);
            return;
        }
        Assert.assertTrue(false);
    }

    @Test(timeout = 1000)
    public void wrongUrl() {
        Logger.getLogger(getClass()).info("Running test: wrongUrl()");
        try {
            new Socket("192.168.1.", PORT);
        } catch (UnknownHostException e) {
            Assert.assertTrue(true);
            return;
        } catch (IOException e) {
            Assert.assertTrue(false);
        }
        Assert.assertTrue(false);
    }

    @Test(timeout = 1000)
    public void udpConnectionType() throws InterruptedException, IOException {
        Logger.getLogger(getClass()).info("Running test: udpConnectionType()");
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket("test".getBytes(), 4, InetAddress.getByName(HOSTNAME), PORT);
        socket.send(packet);

        while (networkData == null) {
            Thread.sleep(1);
        }

        Assert.assertTrue(networkData instanceof UdpNetworkData);
        Assert.assertFalse(networkData instanceof TcpNetworkData);
    }

    @Test(timeout = 1000)
    public void tcpConnectionType() throws InterruptedException, IOException {
        Logger.getLogger(getClass()).info("Running test: tcpConnectionType()");
        new Socket(HOSTNAME, PORT);

        while (networkData == null) {
            Thread.sleep(1);
        }

        Assert.assertTrue(networkData instanceof TcpNetworkData);
        Assert.assertFalse(networkData instanceof UdpNetworkData);
    }

    @Test(timeout = 5000)
    public void shutdown() throws IOException {
        Logger.getLogger(getClass()).info("Running test: shutdown()");
        server.shutdown();
        server = new Server(HOSTNAME, PORT, 100);
        server.shutdown();
    }

    @Test(timeout = 1000)
    public void basicTcpTransfer() throws IOException, InterruptedException {
        Logger.getLogger(getClass()).info("Running test: basicTcpTransfer()");
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
    }

    @Test(timeout = 1000)
    public void dataTcpTransfer() throws IOException, InterruptedException {
        Logger.getLogger(getClass()).info("Running test: dataTcpTransfer()");
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
    }

    @Test(timeout = 1000)
    public void objectTcpTransfer() throws IOException, InterruptedException, ClassNotFoundException {
        Logger.getLogger(getClass()).info("Running test: objectTcpTransfer()");
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
    }

    @Test(timeout = 1000)
    public void simultaneousReadWrite() throws IOException, InterruptedException {
        Logger.getLogger(getClass()).info("Running test: simultaneousReadWrite()");
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
    }

}
