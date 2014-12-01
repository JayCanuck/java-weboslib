
package ca.canuckcoding.novacom;

import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

public class NovacomLunaSend extends Thread {
    private NovacomDevice device;
    private NovacomSocket socket;
    private String address;
    private String params;
    private ArrayList<JSONObject> response;
    private boolean returnVal;
    private IOException ioException;
    private NovacomException ncException;
    private boolean appinstaller;

    public NovacomLunaSend(NovacomDevice device, String address, String params)
            throws IOException {
        this.device = device;
        this.address = address;
        this.params = params;
        appinstaller = false;
        socket = new NovacomSocket(this.device.getPort());
        ioException = null;
        ncException = null;
        if(address.startsWith("palm://com.palm.appinstaller") &&
                params.contains("subscribe")) {
            appinstaller = true;
        }
        response = null;
        returnVal = true;
    }

    private String formatArg(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace(" ", "\\ ");
    }

    @Override
    public void run() {
        StringBuffer command = new StringBuffer();
        command.append("run file:///usr/bin/luna-send ");
        if(appinstaller) {
            System.out.println("Executing appinstaller command: " + address + " " + params);
            command.append("-i ");
        } else {
            command.append("-n 1 ");
        }
        command.append(address);
        command.append(" ");
        command.append(formatArg(params));
        try {
            socket.write(command.toString() + "\n");
            String reply = socket.readline();
            socket.setPacketMode(true);
            if(reply==null) {
                ncException = new NovacomException("No data to read from socket");
            } else if(reply.startsWith("ok")) {
                response = collectResponse();
            } else {
                ncException = new NovacomException(reply);
            }
        } catch(IOException e) {
            System.err.println(e.getMessage());
            ioException = e;
        }
    }

    private ArrayList<JSONObject> collectResponse() throws IOException {
        ArrayList<JSONObject> results = new ArrayList<JSONObject>();
        String line = socket.readline();
        while(line!=null) {
            line = line.trim();
            if(line.length()>0) {
                try {
                    int openBrace = line.indexOf("{");
                    int closeBrace = line.lastIndexOf("}");
                    if(closeBrace>openBrace && openBrace>=0) {
                        line = line.substring(openBrace, closeBrace+1);
                        JSONObject curr = new JSONObject(line);
                        if(curr.has("errorCode") || curr.has("errorText")) {
                            returnVal = false;
                        }
                        if(appinstaller) {
                            System.out.println("\t" + line);
                        }
                        results.add(curr);
                    }
                } catch(JSONException e) {}
                line = line.toLowerCase();
                if(line.contains("fail")) {
                    returnVal = false;
                    break;
                } else if(line.contains("success")) {
                    break;
                }
            }
            line = socket.readline();
        }
        return results;
    }

    public void waitFor() throws InterruptedException, IOException, NovacomException {
        join();
        socket.flush();
        socket.closeInputOutput();
        socket.close();
        if(ioException!=null) {
            throw ioException;
        }
        if(ncException!=null) {
            throw ncException;
        }
    }

    public ArrayList<JSONObject> getResponse() {
        return response;
    }

    public boolean returnValue() {
        return returnVal;
    }
}
