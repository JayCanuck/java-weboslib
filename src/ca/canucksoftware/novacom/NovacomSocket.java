
package ca.canucksoftware.novacom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class NovacomSocket extends Socket {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 6968;
    private boolean packetMode;
    private InputStream input;
    private OutputStream output;
    private NovacomPacket packet;

    public NovacomSocket() throws UnknownHostException, IOException {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }
    public NovacomSocket(String host) throws UnknownHostException, IOException {
        this(host, DEFAULT_PORT);
    }
    public NovacomSocket(int port) throws UnknownHostException, IOException {
        this(DEFAULT_HOST, port);
    }
    public NovacomSocket(String host, int port) throws UnknownHostException, IOException {
        super(host, port);
        input = getInputStream();
        output = getOutputStream();
        packetMode = false;
        packet = new NovacomPacket(input, output);
    }

    public void setPacketMode(boolean value) { packetMode = value; }
    public boolean isPacketMode() { return packetMode; }

    public int read() throws IOException {
        int result;
        if(!packetMode) {
            result = input.read();
        } else {
            result = packet.read();
        }
        return result;
    }

    public int read(byte[] buffer) throws IOException {
        int result;
        if(!packetMode) {
            result = input.read(buffer);
        } else {
            result = packet.read(buffer);
        }
        return result;
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
        if(!packetMode) {
            output.write(data, offset, length);
        } else {
            packet.write(data, offset, length);
        }
    }

    public void write(String text) throws IOException {
        if(!packetMode) {
            output.write(text.getBytes("US-ASCII"));
        } else {
            packet.write(text.getBytes("US-ASCII"));
        }
    }

    public void flush() throws IOException {
        output.flush();
    }

    public int getExitCode() throws NovacomException {
        int exitCode = 0;
        if(packetMode) {
            try {
                while(true) {
                    int val = packet.readPacket();
                    if(val < 0) {
                        throw new Exception();
                    }
                }
            } catch (Exception e) {
                ArrayList<NovacomPacket.OOB> oobList = packet.getOOBList();
                for(int i=0; i<oobList.size(); i++) {
                    NovacomPacket.OOB curr = oobList.get(i);
                    int messageType = curr.getMessageType();
                    if(messageType==2) {
                        return curr.getMessagePayload();
                    }
                }
                throw new NovacomException("No return code found in socket stream");
            }
        }
        return exitCode;
    }

    public void closeInputOutput() throws IOException {
        if(packetMode) {
            output.flush();
            try {
                packet.writeOOBClose(1);
            } catch(IOException e) {}
            try {
                packet.writeOOBClose(2);
            } catch(IOException e) {}
            try {
                packet.writeOOBClose(0);
            } catch(IOException e) {}
            output.flush();
        }
    }
}
