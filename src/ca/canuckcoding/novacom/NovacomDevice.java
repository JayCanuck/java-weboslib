
package ca.canuckcoding.novacom;

import ca.canuckcoding.utils.DESEncrypter;
import ca.canuckcoding.utils.TextStreamConsumer;
import ca.canuckcoding.webos.DeviceInfo;
import ca.canuckcoding.webos.WebOSConnection;
import ca.canuckcoding.webos.WebOSDevice;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.prefs.Preferences;

/**
 * @author Jason Robitaille
 */
public class NovacomDevice extends WebOSDevice {
    private int port;
    private String id;
    private String transport;
    private String name;

    public NovacomDevice(int port, String id, String transport, String name) {
        this.port = port;
        this.id = id;
        this.transport = transport;
        this.name = name;
    }

    public int getPort() { return port; }

    public String getId() { return id; }

    public byte[] getIdAsBytes() { return id.getBytes(); }

    public String getTransport() { return transport; }

    public String getName() { return name; }

    public String getModel() { return DeviceInfo.Model.determineModel(name).toString(); }

    public boolean isEmulator() { return name.equals("emulator"); }

    public WebOSConnection connect() throws NovacomException {
        NovacomConnection result = new NovacomConnection(this);
        if(!result.login()) {
            throw new NovacomException();
        }
        return result;
    }

    public boolean isConnected() throws NovacomException {
        boolean connected = false;
        NovacomDevice[] devices = Novacom.listDevices();
        for(int i=0; i<devices.length; i++) {
            if(devices[i].getId().equalsIgnoreCase(id)) {
                port = devices[i].getPort(); //update port # as it may have changed
                connected = true;
                break;
            }
        }
        return connected;
    }
    
    public String getCachedPassword() {
        DESEncrypter des = new DESEncrypter("password-" + id);
        String password = Preferences.userNodeForPackage(DeviceInfo.class).get("device-" + id, null);
        if(password!=null) {
            password = des.decrypt(password);
        }
        return password;
    }

    public void cachePassword(String password) {
        DESEncrypter des = new DESEncrypter("password-" + id);
        if(password==null) {
            Preferences.userNodeForPackage(DeviceInfo.class).remove("device-" + id);
        } else {
            Preferences.userNodeForPackage(DeviceInfo.class).put("device-" + id, des.encrypt(password));
        }
    }

    public boolean isPasswordProtected() {
        boolean needsPass = false;
        try {
            Process p = Runtime.getRuntime().exec(new String[] {Novacom.execPath(), "-d", id, "-c", "login", "-r", " "});
            OutputStream os = p.getOutputStream();
            os.flush();
            os.close();
            TextStreamConsumer stdout = new TextStreamConsumer(p.getInputStream());
            stdout.start();
            TextStreamConsumer stderr = new TextStreamConsumer(p.getErrorStream());
            stderr.start();
            int exitCode = p.waitFor();
            stdout.waitFor();
            stderr.waitFor();
            String response = stdout.toString() + stderr.toString();
            response = response.toLowerCase();
            if(response.contains("device does not require authentication")) {
                //old device on current drivers or no password set
                needsPass = false;
            } else if(response.contains("please specify password")) {
                needsPass = true;
            } else if(exitCode==0) {
                //no pass needed to login
                needsPass = false;
            } else if(response.contains("usage:")) {
                // old novacom driver; since new devices wouldn't even be listed on old drivers, must be old device
                needsPass = false;
            } else {
                needsPass = true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return needsPass;
    }
}
