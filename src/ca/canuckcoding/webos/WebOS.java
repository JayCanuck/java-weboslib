
package ca.canuckcoding.webos;

import ca.canuckcoding.adb.Adb;
import ca.canuckcoding.novacom.Novacom;
import java.util.ArrayList;
import java.util.Arrays;

public class WebOS {
    public final String LIB_VERSION = "1.1.0";
    public static WebOSDevice[] listDevices() {
        ArrayList<WebOSDevice> devices = new ArrayList<WebOSDevice>();
        if(Novacom.isInstalled()) {
            try {
                devices.addAll(Arrays.asList(Novacom.listDevices()));
            } catch (WebOSException e) {}
        }
        if(Adb.isInstalled()) {
            try {
                devices.addAll(Arrays.asList(Adb.listDevices()));
            } catch (WebOSException e) {}
        }
        return devices.toArray(new WebOSDevice[devices.size()]);
    }
}
