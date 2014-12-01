
package ca.canuckcoding.adb;

import ca.canuckcoding.novacom.*;
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
public class AdbDevice extends WebOSDevice {
    private int port;
    private String id;
    private String host;
    private String name = "Adb Device";

    public AdbDevice(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public int getPort() { return port; }

    @Override
    public String getId() { return id; }

    public byte[] getIdAsBytes() { return id.getBytes(); }

    public String getHost() { return host; }

    @Override
    public String getName() { return name; }

    public String getModel() { return DeviceInfo.Model.determineModel(name).toString(); }

    public boolean isEmulator() { return name.equals("emulator"); }

    @Override
    public WebOSConnection connect() throws NovacomException {
        return new AdbConnection(this);
    }

    public boolean isConnected() throws AdbException {
        boolean connected = false;
        AdbDevice[] devices = Adb.listDevices();
        for(int i=0; i<devices.length; i++) {
            if(devices[i].getId().equalsIgnoreCase(id)) {
                port = devices[i].getPort(); //update port # as it may have changed
                connected = true;
                break;
            }
        }
        return connected;
    }
}
