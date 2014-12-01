
package ca.canuckcoding.adb;

import ca.canuckcoding.utils.TextStreamConsumer;
import ca.canuckcoding.webos.WebOSConnection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;

/**
 * @author Jason Robitaille
 */
public class Adb {
    public static boolean isInstalled() {
        boolean isInstalled = true;
        attemptStartAdbServer();
        try {
            AdbSocket adb = new AdbSocket();
            isInstalled = adb.sendCommand("host:version");
            adb.close();
        } catch(Exception e) {
            isInstalled = false;
        }
        return isInstalled;
    }

    public static AdbDevice[] listDevices() throws AdbException {
        ResourceBundle locale = ResourceBundle.getBundle("ca/canuckcoding/webos/Locale");
        ArrayList<AdbDevice> devices = new ArrayList<AdbDevice>();
        try {
            AdbSocket adb = new AdbSocket();
            if(adb.sendCommand("host:devices")) {
                String line = adb.readline();
                while(line!=null) {
                    if(line.length()>4) {
                        String[] parts = line.substring(4).split("\\s+");
                        if(parts.length>1) {
                            AdbDevice curr = new AdbDevice(adb.getHost(), adb.getPort(), parts[0]);
                            if(isWebOS(curr)) {
                                devices.add(curr);
                            }
                        }
                    }
                    line = adb.readline();
                }
            }
            adb.close();
        } catch(Exception e) {}
        return devices.toArray(new AdbDevice[devices.size()]);
    }
    
    private static boolean isWebOS(AdbDevice device) {
        boolean result = false;
        try {
            WebOSConnection con = device.connect();
            try {
                String out1 = con.runProgram("/bin/cat", new String[] {"/etc/os-release"});
                result |= out1.toString().contains("webos");
            }catch(Exception e1) {}
            try {
                String out2 = con.runProgram("/bin/cat", new String[] {"/etc/webos-release"});
                result |= out2.toString().contains("luneos");
            }catch(Exception e2) {}
        } catch(Exception e) {}
        return result;
    }
    
    public static void attemptStartAdbServer() {
        try {
            Process adb = Runtime.getRuntime().exec("adb devices");
            doProcess(adb);
        } catch (Exception e) {}
    }
    
    public static void launchTerminal(AdbDevice device) {
        Process launcher = null;
        try {
            String platform = System.getProperty("os.name").toLowerCase();
            if(platform.contains("windows") || platform.contains("linux")) {
                String tmpFilePath = System.getProperty("java.io.tmpdir");
                File script = new File(tmpFilePath, "adb.cmd");
                if(script.exists()) {
                    script.delete();
                }
                BufferedWriter bw = new BufferedWriter(new FileWriter(script));
                bw.write("adb -s " + device.getId() + " shell");
                bw.flush();
                bw.close();
                if(platform.contains("windows")) {
                    launcher = Runtime.getRuntime().exec("cmd.exe /c start " +
                            script.getAbsolutePath());
                } else if(platform.contains("linux")) {
                    launcher = Runtime.getRuntime().exec(new String[] {"xterm", "+hold",
                            "-e", "sh \"" + script.getAbsolutePath() + "\""});
                }
            } else if(platform.contains("mac")) {
                launcher = Runtime.getRuntime().exec(new String[] {"/usr/bin/osascript",
                        "-e", "tell application \"Terminal\" to do script \"" +
                        "adb -s " + device.getId() + " shell\""});
            }
            if(launcher!=null) {
                OutputStream os = launcher.getOutputStream();
                os.flush();
                os.close();
                TextStreamConsumer stdout = new TextStreamConsumer(launcher.getInputStream());
                stdout.start();
                TextStreamConsumer stderr = new TextStreamConsumer(launcher.getErrorStream());
                stderr.start();
            }
        } catch(Exception e) {
            JOptionPane.showMessageDialog(null, "Unable to launch terminal access.\nMake sure adb "
                    + "is in your PATH environment variable");
        }
    }
    
    private static boolean doProcess(Process p) throws IOException, InterruptedException {
        OutputStream os;
        TextStreamConsumer stdout, stderr;
        os = p.getOutputStream();
        os.flush();
        os.close();
        stdout = new TextStreamConsumer(p.getInputStream());
        stdout.start();
        stderr = new TextStreamConsumer(p.getErrorStream());
        stderr.start();
        int exitCode = p.waitFor();
        stdout.waitFor();
        stderr.waitFor();
        return (exitCode==0);
    }
}
