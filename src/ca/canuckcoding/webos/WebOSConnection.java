
package ca.canuckcoding.webos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import org.json.JSONObject;

/**
 * @author Jason Robitaille
 */
public abstract class WebOSConnection {
    public final String offlineRoot = "/media/cryptofs/apps";
    public boolean javaRestartFlag;
    public boolean lunaRestartFlag;
    public boolean deviceRestartFlag;
    protected ResourceBundle locale;
    private DeviceInfo info;
    private WebOSDevice device;
    private String pkgMgr = "ipkg";

    public WebOSConnection(WebOSDevice wd) {
        javaRestartFlag = false;
        lunaRestartFlag = false;
        deviceRestartFlag = false;
        locale = ResourceBundle.getBundle("ca/canuckcoding/webos/Locale");
        info = null;
        device = wd;
    }

    public abstract boolean isConnected();

    public abstract boolean sendFile(File src, String dest);

    public abstract boolean receiveFile(String src, File dest);

    public File receiveFile(String src) {
        File result = null;
        String tmpFilePath = System.getProperty("java.io.tmpdir");
        File tmp = new File(tmpFilePath, getFileName(src));
        if(tmp.exists()) {
            tmp.delete();
        }
        if(receiveFile(src, tmp)) {
            result = tmp;
        }
        return result;
    }

    public abstract String runProgram(String app, String[] params) throws WebOSException;

    public abstract List<JSONObject> lunaSend(String address, JSONObject params) throws WebOSException;

    public boolean remountPartitionReadWrite() {
        boolean result = false;
        try {
            if(!getDeviceInfo().buildName().contains("SDK")) {
                runProgram("/bin/mount", new String[] {"-o", "remount,rw", "/"});
            }
            result = true;
        } catch(Exception e) {}
        return result;
    }

    public boolean fileExists(String file) {
        boolean result = false;
        try {
            runProgram("/bin/ls", new String[] {"-d", file});
            result = true;
        } catch(Exception e) {}
        return result;
    }

    public boolean mkdir(String path) {
        boolean result = false;
        try {
            runProgram("/bin/mkdir", new String[] {"-p", path});
            result = true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean delete(String path) {
        boolean result = false;
        try {
            runProgram("/bin/rm", new String[] {"-fr", path});
            result = true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public DeviceInfo getDeviceInfo() {
        if(info==null) {
            info = new DeviceInfo(this, device);
        }
        return info;
    }
    
    public String getPkgMgr() {
        return pkgMgr;
    }

    /* Old ipkg-based version
    public ArrayList<InstalledEntry> listInstalled() {
        ArrayList<InstalledEntry> apps = new ArrayList();
        try {
            String stdout = runProgram("/usr/bin/ipkg", new String[] {"-o", offlineRoot,
                    "list_installed"});
            String[] lines = stdout.split("\n");
            for(int i=0; i<lines.length; i++) {
                lines[i] = lines[i].trim();
                if(lines[i].length()>0) {
                    InstalledEntry curr = new InstalledEntry(lines[i]);
                    if(curr!=null && curr.getName()!=null) {
                        apps.add(curr);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return apps;
    }*/

     public ArrayList<InstalledEntry> listInstalled() {
        ArrayList<InstalledEntry> apps = new ArrayList();
        try {
            String status = offlineRoot + "/usr/lib/ipkg/status";
            if(pkgMgr.equals("opkg")) {
                status = offlineRoot + "/var/lib/opkg/status";
            }
            if(fileExists(status)) {
                String stdout = runProgram("/bin/grep", new String[] {"-e", "Package:",
                        "-e", "Version:", "-e", "Description:", status}); //"-e", "^$"
                String[] lines = stdout.split("\n");
                String id = "<unknown id>";
                String version = "?.?.?";
                String name = "Unknown";
                boolean pending = false;
                for(int i=0; i<lines.length; i++) {
                    String val = lines[i].substring(lines[i].indexOf(":")+1).trim();
                    if(lines[i].startsWith("Package:")) {
                        if(i>0 && i!=lines.length-1) {
                            apps.add(new InstalledEntry(name, id, version));
                            version = "?.?.?";
                            name = "Unknown";
                        }
                        id = val;
                        pending = true;
                    } else if(lines[i].startsWith("Version:")) {
                        version = val;
                    } else if(lines[i].startsWith("Description:")) {
                        name = val;
                    }
                }
                if(pending) {
                    apps.add(new InstalledEntry(name, id, version));
                }
            }
        } catch(Exception e) {}
        return apps;
    }

    public ArrayList<String> whatDepends(String appid) {
        ArrayList<String> apps = new ArrayList();
        try {
            String stdout = runProgram("/usr/bin/" + pkgMgr, new String[] {"-o", offlineRoot,
                    "whatdepends", appid});
            String[] lines = stdout.split("\n");
            for(int i=3; i<lines.length; i++) {
                String[] tokens = lines[i].trim().split("\\s+");
                if(tokens[tokens.length-1].equals(appid)) {
                    apps.add(tokens[0]);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return apps;
    }

    public ArrayList<String> whatDependsRecursive(String appid) {
        ArrayList<String> apps = new ArrayList();
        try {
            String stdout = runProgram("/usr/bin/" + pkgMgr, new String[] {"-o", offlineRoot,
                    "whatdependsrec", appid});
            String[] lines = stdout.split("\n");
            for(int i=3; i<lines.length; i++) {
                lines[i] = lines[i].trim();
                apps.add(lines[i].substring(0, lines[i].indexOf(" ")));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return apps;
    }

    public boolean sendScript(ScriptType type) {
        Script scriptRes = new Script(type);
        File script = scriptRes.extract();
        boolean result = false;
        if(script!=null) {
            if(sendFile(script, "/var/" + type.filename())) {
                result = true;
            } else {
                System.err.println("ERROR: Unable to copy " + type.filename()
                        + " to device");
            }
        } else {
            System.err.println("ERROR: " + type.filename() + " could not be extracted");
        }
        return result;
    }

    public boolean removeScript(ScriptType type) {
        boolean result = false;
        try {
            runProgram("/bin/rm", new String[] {"-f", "/var/"
                    + type.filename()});
            result = true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String getFileName(String path) {
        String result = path;
        int index = result.lastIndexOf("/");
        if(index!=-1) {
            result = result.substring(index+1);
        }
        index = result.lastIndexOf("\\");
        if(index!=-1) {
            result = result.substring(index+1);
        }
        return result;
    }

    public boolean isSystemBusy() {
        boolean result = false;
        if(!fileExists("/usr/bin/ipkg")) {
            pkgMgr = "opkg";
        }
        if(pkgMgr.equals("ipkg")) {
            try {
                String out = runProgram("/usr/bin/pgrep", new String[] {"-f",
                        "ApplicationInstallerUtility"});
                if(!out.trim().isEmpty()) {
                    result = true;
                }
            } catch(Exception e) {}
        }
        return result;
    }

    public void configPkgMgr() {
        if(!fileExists("/usr/bin/ipkg")) {
            pkgMgr = "opkg";
        }
        if(!fileExists("/media/cryptofs/apps/etc/" + pkgMgr + "/arch.conf")) {
            try {
                mkdir("/media/cryptofs/apps/etc/" + pkgMgr);
                runProgram("/bin/cp", new String[]{"-f", "/etc/" + pkgMgr + "/arch.conf",
                        "/media/cryptofs/apps/etc/" + pkgMgr + "/arch.conf"});
            } catch(Exception e) {
                System.err.println("Unable to copy " + pkgMgr + " configuration file:" +
                        e.getMessage());
            }
        }
    }

    public boolean install(File file) {
        boolean result = false;
        String name = getFileName(file.getName());
        if(mkdir("/media/internal/.developer")) {
            if(sendFile(file, "/media/internal/.developer/" + name)) {
                result = install("/media/internal/.developer/" + name);
            }
        }
        return result;
    }

    public boolean install(String filename) {
        boolean result = false;
        try {
            String out = runProgram("/bin/sh", new String[] {"/var/"
                    + ScriptType.ScanID.filename(), filename});
            String[] tokens = out.split("\n");
            String appid = null, restartFlag = null, arch = null;
            String[] depends = new String[0];
            for(int i=0; i<tokens.length; i++) {
                if(tokens[i].startsWith("Package:")) {
                    appid = tokens[i].substring(tokens[i].indexOf(":")+1).trim();
                } else if(tokens[i].startsWith("Architecture:")) {
                    arch = tokens[i].substring(tokens[i].indexOf(":")+1).trim();
                } else if(tokens[i].startsWith("Depends:")) {
                    depends = tokens[i].substring(tokens[i].indexOf(":")+1).split(",");
                } else if(tokens[i].startsWith("Source:")) {
                    try {
                        JSONObject source = new JSONObject(tokens[1]
                                .substring(tokens[1].indexOf("{"), tokens[1].lastIndexOf("}")+1));
                        restartFlag = source.getString("PostInstallFlags").toLowerCase();
                    } catch(Exception e) {}
                }
            }
            try {
                if(arch!=null && !arch.equals("any") && !arch.equals("all") &&
                        !getDeviceInfo().arch().startsWith(arch)) {
                    throw new WebOSException(locale.getString("IPKG_INSTALL_FAILED_ARCH"));
                }
                ArrayList<InstalledEntry> installed = listInstalled();
                for(int i=0; i<depends.length; i++) {
                    depends[i] = depends[i].trim();
                    if(depends[i].length()!=0) {
                        if(!installed.contains(new InstalledEntry(depends[i]))) {
                            throw new WebOSException(locale.getString("IPKG_INSTALL_FAILED_DEPENDS"));
                        }
                    }
                }
                JSONObject params = new JSONObject();
                params.put("target", filename);
                params.put("subscribe", true);
                params.put("uncompressedSize", 0);
                lunaSend("palm://com.palm.appinstaller/installNoVerify", params);
                String infoDir = offlineRoot + "/usr/lib/ipkg/info/";
                if(pkgMgr.equals("opkg")) {
                    infoDir = offlineRoot + "/var/lib/opkg/info/";
                }
                if(fileExists(infoDir + appid + ".control")) {
                    if(fileExists(infoDir + appid + ".postinst")) {
                        out = runIpkgScript(infoDir + appid + ".postinst");
                    }
                    result = true;
                    handleRestartFlag(restartFlag);
                } else {
                    throw new WebOSException(locale.getString("IPKG_INSTALL_FAILED"));
                }
            } catch(Exception e) {
                JOptionPane.showMessageDialog(null, MessageFormat.format(
                        locale.getString("ERROR:_AN_ERROR_OCCURED_WHILE_ATTEMPTING_TO_INSTALL_{0}"),
                        new Object[] {getFileName(filename)}) + "\n\n" + e.getMessage());
                System.err.println("Unable to install " + getFileName(filename));
            }
        } catch(Exception e) {
            JOptionPane.showMessageDialog(null,
                    locale.getString("ERROR:_INVALID_OR_CORRUPT_PACKAGE") + "\n"
                    + getFileName(filename));
        }
        return result;
    }

    public boolean uninstall(String appid) {
        boolean result = false;
        try {
            String out;
            String restartFlag = "";
            String infoDir = offlineRoot + "/usr/lib/ipkg/info/";
            if(pkgMgr.equals("opkg")) {
                infoDir = offlineRoot + "/var/lib/opkg/info/";
            }
            try {
                out = runProgram("/bin/grep", new String[] {"Source:", infoDir + appid +
                        ".control"});
                JSONObject source = new JSONObject(out.substring(out.indexOf("{"),
                        out.lastIndexOf("}")+1));
                restartFlag = source.getString("PostRemoveFlags").toLowerCase();
            } catch(Exception e) {}
            if(fileExists(infoDir + appid + ".prerm")) {
                out = runIpkgScript(infoDir + appid + ".prerm");
            }
            if(fileExists(offlineRoot + "/usr/palm/applications/" + appid +
                    "/appinfo.json")) {
                JSONObject params = new JSONObject();
                params.put("packageName", appid);
                params.put("subscribe", true);
                lunaSend("palm://com.palm.appinstaller/remove", params);
            } else {
                try {
                    out = runProgram("/bin/sh", new String[] {offlineRoot + "/.scripts/" +
                            appid + "/pmPreRemove.script"});
                } catch(Exception e) {}
                try {
                    out = runProgram("/usr/bin/" + pkgMgr, new String[] {"-o", offlineRoot,
                            "-force-depends", "remove", appid});
                } catch(Exception e) {
                    throw new WebOSException(locale.getString("IPKG_UNINSTALL_FAILED"));
                }
            }
            if(!fileExists(infoDir + appid + ".control")) {
                result = true;
                if(appid.startsWith("ca.canucksoftware.patches.") || appid.startsWith("ca.canuckcoding.patches.")) {
                    lunaRestartFlag = true;
                } else {
                    handleRestartFlag(restartFlag);
                }
            } else {
                throw new WebOSException(locale.getString("IPKG_UNINSTALL_FAILED"));
            }
         } catch(Exception e) {
            JOptionPane.showMessageDialog(null, MessageFormat.format(
                    locale.getString("ERROR:_AN_ERROR_OCCURED_WHILE_ATTEMPTING_TO_UNINSTALL_{0}"),
                    new Object[] {appid}) + "\n\n" + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    private String runIpkgScript(String file) throws WebOSException {
        File script = new File(System.getProperty("java.io.tmpdir"),
                "ipkgScript");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(script));
            bw.write("/bin/sh -c 'export IPKG_OFFLINE_ROOT=" + offlineRoot +
                    " ; /bin/sh " + file + "'\n");
            bw.flush();
            bw.close();
            sendFile(script, "/tmp/ipkgScript.sh");
            script.delete();
        } catch(IOException e) {
            throw new WebOSException(e);
        }
        return runProgram("/bin/sh", new String[] {"/tmp/ipkgScript.sh"});
    }

    private void handleRestartFlag(String flag) {
        if(flag!=null) {
            if(flag.length()>0) {
                javaRestartFlag |= flag.equals("restartjava");
                lunaRestartFlag |= flag.equals("restartluna");
                deviceRestartFlag |= flag.equals("restartdevice");
            }
        }
    }

    public void executeRestartFlags() {
        if(deviceRestartFlag) {
            deviceRestart();
        } else {
            if(javaRestartFlag) {
                javaRestart();
            }
            if(lunaRestartFlag) {
                lunaRestart();
            }
        }
        javaRestartFlag = false;
        lunaRestartFlag = false;
        deviceRestartFlag = false;
    }

    public void javaRestart() {
        if(getDeviceInfo().version().charAt(0)=='1') {
            try {
                runProgram("/usr/bin/killall", new String[] {"-9", "java"});
            } catch(Exception e) {
                System.err.println("Error while attemptong to Java restart: " +
                        e.getMessage());
            }
        }
    }

    public void lunaRestart() {
        try {
            runProgram("/usr/bin/killall", new String[] {"-HUP",
                    "LunaSysMgr"});
        } catch(Exception e) {
            System.err.println("Error while attemptong to Java restart: " +
                    e.getMessage());
        }
    }

    public void deviceRestart() {
        try {
            runProgram("/sbin/reboot", new String[] {});
        } catch(Exception e) {
            System.err.println("Error while attemptong to Java restart: " +
                    e.getMessage());
        }
    }

    public abstract void launchTerminal() throws UnsupportedOperationException;
}
