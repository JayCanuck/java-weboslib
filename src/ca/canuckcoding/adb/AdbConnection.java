
package ca.canuckcoding.adb;

import ca.canuckcoding.novacom.*;
import ca.canuckcoding.utils.TextStreamConsumer;
import ca.canuckcoding.webos.WebOSConnection;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.swing.JOptionPane;
import org.json.JSONObject;

public class AdbConnection extends WebOSConnection {
    private AdbDevice device;
    private boolean loggedIn;
    public AdbConnection(AdbDevice nd) {
        super(nd);
        device = nd;
        loggedIn = false;
    }

    @Override
    public boolean isConnected() {
        boolean result = false;
        try {
            result = device.isConnected();
        } catch(Exception e) {}
        return result;
    }

    @Override
    public boolean sendFile(File src, String dest) {
        boolean result = true;
        try {
            AdbCommand nCmd = new AdbCommand(device, "SEND", dest, new String[]{});
            nCmd.stdinFile(src);
            nCmd.start();
            nCmd.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    @Override
    public boolean receiveFile(String src, File dest) {
        boolean result = true;
        try {
            AdbCommand nCmd = new AdbCommand(device, "RECV", src, new String[]{});
            nCmd.stdoutFile(dest);
            nCmd.start();
            nCmd.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    @Override
    public String runProgram(String app, String[] params) throws AdbException {
        String output = null;
        try {
            AdbCommand nCmd = new AdbCommand(device, "shell", app, params);
            nCmd.start();
            nCmd.waitFor();
            output = nCmd.getResponse();
            if(nCmd.getExitCode()!=0) {
                throw new AdbException(output);
            }
        } catch (IOException e) {
            throw new AdbException(e);
        } catch (InterruptedException e) {
            throw new AdbException(e);
        }
        return output;
    }

    @Override
    public List<JSONObject> lunaSend(String address, JSONObject params)
            throws AdbException {
        List results = null;
        try {
            AdbLunaSend nls = new AdbLunaSend(device, address, params.toString());
            nls.start();
            nls.waitFor();
            results = nls.getResponse();
            if(!nls.returnValue()) {
                String errMsg = "";
                for(int i=0; i<results.size(); i++) {
                    errMsg += results.get(i).toString();
                    if(i<results.size()-1) {
                        errMsg += "\n";
                    }
                }
                throw new AdbException(errMsg);
            }
        } catch (IOException e) {
            throw new AdbException(e);
        } catch (InterruptedException e) {
            throw new AdbException(e);
        }
        return results;
    }

    @Override
    public void launchTerminal() {
        try {
            Adb.launchTerminal(device);
        } catch(Exception e) {}
    }
}