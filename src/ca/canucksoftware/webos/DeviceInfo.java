
package ca.canucksoftware.webos;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import org.json.JSONObject;

/**
 * @author Jason Robitaille
 */
public class DeviceInfo {
    private WebOSConnection webOSCon;
    private WebOSDevice device;
    private String name;
    private String model;
    private String os;
    private String version;
    private String buildName;
    private String buildTime;
    private String arch;

    public DeviceInfo(WebOSConnection wc, WebOSDevice wd) {
        webOSCon = wc;
        name = null;
        device = wd;
        model = Model.Unknown.toString();
        os = "Palm webOS";
        version = "?.?.?";
        buildName = "Nova";
        buildTime = "????";
        arch = null;
        loadBuildInfo();
        if(model.equals(Model.Unknown.toString())) {
            Model test = Model.determineModel(device.getName());
            if(test!=Model.Unknown) {
                model = test.toString();
                if(arch==null) {
                    arch = test.getArch();
                }
            }
        }
        if(arch==null) {
            loadArch();
        }
        if(arch==null) {
            arch = "????";
        }
    }

    private void loadBuildInfo() {
        try {
            String stdout = webOSCon.runProgram("/bin/cat",
                    new String[] {"/etc/palm-build-info"});
            String[] info = stdout.split("\n");
            for(int i=0; i<info.length; i++) {
                if(info[i].startsWith("PRODUCT_VERSION_STRING")) {
                    os = info[i].substring(info[i].indexOf("=")+1);
                    String[] tokens = os.split(" ");
                    if(tokens.length>=3) {
                        version = tokens[2];
                    }
                } else if(info[i].startsWith("BUILDNAME")) {
                    buildName = info[i].substring(info[i].indexOf("=")+1);
                    Model test = Model.determineModel(buildName);
                    if(test!=Model.Unknown) {
                        model = test.toString();
                        if(arch==null) {
                            arch = test.getArch();
                        }
                    }
                } else if(info[i].startsWith("BUILDTIME")) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
                    Date datetime = format.parse(info[i],
                            new ParsePosition(info[i].indexOf("=")+1));
                    buildTime = DateFormat.getDateTimeInstance().format(datetime);

                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadArch() {
        try {
            String stdout = webOSCon.runProgram("/bin/uname", new String[] {"-a"});
            if(stdout.contains("i686")) {
                arch = "i686";
            } else if(stdout.contains("armv6")) {
                arch = "armv6";
            } else if(stdout.contains("armv7")) {
                arch = "armv7";
            } else {
                String[] token = stdout.split("\\s+");
                arch = token[token.length-2];
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public String name() {
        String result = model;
        if(Preferences.systemRoot().getBoolean("useDeviceName", false)) {
            if(name!=null) {
                result = name;
            } else {
                try {
                    JSONObject param = new JSONObject("{\"keys\":[\"deviceName\"]}");
                    List<JSONObject> payload = webOSCon.lunaSend("palm://com.palm.systemservice/getPreferences",
                            param);
                    if(payload.size()>0) {
                        JSONObject response = payload.get(0);
                        if(response.has("deviceName")) {
                            name = response.getString("deviceName");
                            result = name;
                        }
                    }
                } catch(Exception e) {}
            }
        }
        return result;
    }
    
    public String model() { return model; }

    public String os() { return os; }

    public String version() { return version; }

    public String buildName() { return buildName; }

    public String buildTime() { return buildTime; }

    public String arch() { return arch; }

    public static void useDeviceName(boolean use) {
        Preferences.systemRoot().putBoolean("useDeviceName", use);
    }

    public static boolean isUsingDeviceName() {
        return Preferences.systemRoot().getBoolean("useDeviceName", false);
    }

    public enum Model {
        Palm_Pre("Palm Pre", "armv7"),
        Palm_Pixi("Palm Pixi", "armv6"),
        Palm_Pre_Plus("Palm Pre Plus", "armv7"),
        Palm_Pixi_Plus("Palm Pixi Plus", "armv6"),
        Palm_Pre_2("Palm Pre 2", "armv7"),
        HP_Veer("HP Veer", "armv7"),
        HP_TouchPad("HP TouchPad", "armv7"),
        HP_Pre_3("HP Pre 3", "armv7"),
        Emulator(ResourceBundle.getBundle("ca/canucksoftware/webos/Locale")
                .getString("EMULATOR"), "i686"),
        Unknown(ResourceBundle.getBundle("ca/canucksoftware/webos/Locale")
                .getString("UNKNOWN_DEVICE"), null);

        private String model;
        private String arch;
        Model(String model, String arch) {
            this.model = model;
            this.arch = arch;
        }

        public String getArch() { return arch; }

        @Override
        public String toString() { return model; }

        public static Model determineModel(String test) {
            Model result = Unknown;
            test = test.toLowerCase();
            if(test.contains("verizon") && test.contains("pixie")) {
                result = Model.Palm_Pixi_Plus;
            } else if(test.contains("castleplus")) {
                result = Model.Palm_Pre_Plus;
            } else if(test.contains("castle")) {
                result = Model.Palm_Pre;
            } else if(test.contains("pixie")) {
                result = Model.Palm_Pixi;
            } else if(test.contains("roadrunner")) {
                result = Model.Palm_Pre_2;
            } else if(test.contains("broadway")) {
                result = Model.HP_Veer;
            } else if(test.contains("manta")) {
                result = Model.HP_Pre_3;
            } else if(test.contains("topaz")) {
                result = Model.HP_TouchPad;
            } else if(test.contains("sdk")) {
                result = Model.Emulator;
            } else if(test.contains("emulator")) {
                result = Model.Emulator;
            }
            return result;
        }
    }
}
