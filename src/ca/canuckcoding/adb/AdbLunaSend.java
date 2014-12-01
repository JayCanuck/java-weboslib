
package ca.canuckcoding.adb;

import ca.canuckcoding.novacom.*;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

public class AdbLunaSend extends Thread {
    private AdbDevice device;
    private AdbSocket socket;
    private String address;
    private String params;
    private ArrayList<JSONObject> response;
    private boolean returnVal;
    private IOException ioException;
    private AdbException adbException;
    private boolean appinstaller;

    public AdbLunaSend(AdbDevice device, String address, String params)
            throws IOException {
        this.device = device;
        this.address = address;
        this.params = params;
        appinstaller = false;
        socket = new AdbSocket(this.device.getHost(), this.device.getPort());
        ioException = null;
        adbException = null;
        if(address.startsWith("palm://com.palm.appinstaller") &&
                params.contains("subscribe")) {
            appinstaller = true;
        }
        response = null;
        returnVal = true;
    }

    private String formatArg(String s) {
        return s.replace("\\", "\\\\").replace("'", "\'");
    }

    @Override
    public void run() {
        StringBuffer command = new StringBuffer();
        command.append("shell:/usr/bin/luna-send ");
        if(appinstaller) {
            System.out.println("Executing appinstaller command: " + address + " " + params);
            command.append("-i ");
        } else {
            command.append("-n 1 ");
        }
        command.append(address);
        command.append(" '");
        command.append(formatArg(params));
        command.append("'");
        try {
            if(socket.sendCommand("host:transport:" + device.getId())) {
                if(socket.sendCommand(command.toString())) {
                    response = collectResponse();
                } else {
                    adbException = new AdbException("Adb command failure");
                }
            } else {
                adbException = new AdbException("Adb failure: unable to connect to device");
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
                if(appinstaller && line.contains("fail")) {
                    returnVal = false;
                    break;
                } else if(appinstaller && line.contains("success")) {
                    returnVal = true;
                    break;
                }
            }
            line = socket.readline();
        }
        return results;
    }

    public void waitFor() throws InterruptedException, IOException, AdbException {
        join();
        socket.flush();
        socket.close();
        if(ioException!=null) {
            throw ioException;
        }
        if(adbException!=null) {
            throw adbException;
        }
    }

    public ArrayList<JSONObject> getResponse() {
        return response;
    }

    public boolean returnValue() {
        return returnVal;
    }
}
