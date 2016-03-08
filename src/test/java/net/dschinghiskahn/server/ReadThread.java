package net.dschinghiskahn.server;

import java.io.DataInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

@SuppressWarnings("PMD")
public class ReadThread extends Thread {

    private final DataInputStream inputStream;
    private final ServerTest parent;

    public ReadThread(DataInputStream inputStream, ServerTest parent) {
        super();
        this.inputStream = inputStream;
        this.parent = parent;
    }

    @Override
    public void run() {
        while (true) {
            try {
                parent.setRead(inputStream.readUTF());
            } catch (IOException e) {
                Logger.getRootLogger().error("Error reading data!", e);
            }
        }
    }
}
