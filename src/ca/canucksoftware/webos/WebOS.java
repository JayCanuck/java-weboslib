
package ca.canucksoftware.webos;

import ca.canucksoftware.novacom.Novacom;
import ca.canucksoftware.novacom.NovacomException;
import java.util.ArrayList;
import java.util.Arrays;

public class WebOS {
    public static WebOSDevice[] listDevices() {
        ArrayList<WebOSDevice> devices = new ArrayList<WebOSDevice>();
        if(Novacom.isInstalled()) {
            try {
                devices.addAll(Arrays.asList(Novacom.listDevices()));
            } catch (NovacomException e) {}
        }
        return devices.toArray(new WebOSDevice[devices.size()]);
    }
}
