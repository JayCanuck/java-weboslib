
package ca.canuckcoding.adb;

import ca.canuckcoding.novacom.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdbSocket extends Socket {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 5037;
    private DataInputStream input;
    private DataOutputStream output;

    public AdbSocket() throws UnknownHostException, IOException {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }
    public AdbSocket(String host) throws UnknownHostException, IOException {
        this(host, DEFAULT_PORT);
    }
    public AdbSocket(int port) throws UnknownHostException, IOException {
        this(DEFAULT_HOST, port);
    }
    public AdbSocket(String host, int port) throws UnknownHostException, IOException {
        super(host, port);
        input = new DataInputStream(getInputStream());
        output = new DataOutputStream(getOutputStream());
    }
    
    public String getHost() {
        return getInetAddress().getHostAddress();
    }
    
    public int read() throws IOException {
        return input.read();
    }
    
    public int readInt() throws IOException {
        return input.readInt();
    }

    public int read(byte[] buffer) throws IOException {
        return input.read(buffer);
    }
    
    public String read(int length) throws IOException {
        byte[] buff = new byte[length];
        input.read(buff);
        return new String(buff, "UTF-8");
    }
    
    public int readChunk(byte[] buffer) throws IOException {
        int n = -1;
        if(okay()) {
            n = Integer.reverseBytes(readInt());
            input.read(buffer, 0, n);
        } else {
            try {
                System.err.println(read(Integer.reverseBytes(readInt())));
            } catch(Exception e) {}
        }
        return n;
    }
    
    public boolean okay() throws IOException {
        String status = read(4);
        return (status.equalsIgnoreCase("OKAY") || status.equalsIgnoreCase("DATA"));
    }

    public String readline() throws IOException {
        int character = -1;
        StringBuffer sb = new StringBuffer();
        while((character = read()) != -1) {
            if(((char) character) == '\n') {
                break;
            }
            sb.append((char) character);
        }
        String result = sb.toString();
        if((character == -1) && result.length()==0) {
            result = null;
        }
        return result;
    }

    public void write(byte[] data, int offset, int length) throws IOException {
        output.write(data, offset, length);
    }

    public void write(String text) throws IOException {
        output.write(text.getBytes("UTF-8"));
    }
    
    public void writeInt(int intVal) throws IOException {
        output.writeInt(intVal);
    }
    
    public void writeChunk(byte[] buffer, int offset, int length) throws IOException {
        output.writeBytes("DATA");
        output.writeInt(Integer.reverseBytes(length));
        output.write(buffer, offset, length);
    }
    
    public boolean sendCommand(String command) throws IOException {
        write(String.format("%04x", command.length()));
        write(command);
        return okay();
    }
    
    public boolean sendSyncAction(String action, String command) throws IOException {
        boolean ret = sendCommand("sync:");
        if(ret) {
            write(action);
            writeInt(Integer.reverseBytes(command.length()));
            write(command);
        }
        return ret;
    }
    
    public void sendStatus(String status, int detail) throws IOException {
        output.writeBytes(status);
        output.writeInt(Integer.reverseBytes(detail));
    }

    public void flush() throws IOException {
        output.flush();
    }
}
