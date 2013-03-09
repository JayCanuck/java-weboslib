
package ca.canucksoftware.novacom;

import ca.canucksoftware.utils.TextStreamConsumer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * @author Jason Robitaille
 */
public class Novacom {
    public static String execPath() {
        String novacom = "novacom";
        if(new File("/usr/local/bin/novacom").exists()) { //mac or linux
            novacom = "/usr/local/bin/novacom";
        } else if(new File("/opt/nova/bin/novacom").exists()) { //mac or linux
            novacom = "/opt/nova/bin/novacom";
        } else { //windows
            String programPath32 = System.getenv("ProgramFiles");
            String programPath64 = null;
            if(programPath32!=null) {
                programPath32 = programPath32.replace("\\", "/").trim();
                if(programPath32.endsWith("(x86)")) {
                    programPath64 = programPath32.substring(0,
                            programPath32.lastIndexOf("(x86)")).trim();
                    programPath64 = programPath64.replace("\\", "/");
                }

                if(programPath64!=null && new File(programPath64 + "/HP webOS/SDK/bin/novacom.exe").exists()) {
                    novacom = programPath64 + "/HP webOS/SDK/bin/novacom.exe";
                } else if(new File(programPath32 + "/HP webOS/SDK/bin/novacom.exe").exists()) {
                    novacom = programPath32 + "/HP webOS/SDK/bin/novacom.exe";
                } else if(programPath64!=null && new File(programPath64 + "/Palm, Inc/novacom.exe").exists()) {
                    novacom = programPath64 + "/Palm, Inc/novacom.exe";
                } else if(new File(programPath32 + "/Palm, Inc/novacom.exe").exists()) {
                    novacom = programPath32 + "/Palm, Inc/novacom.exe";
                } else if(programPath64!=null && new File(programPath64 + "/Palm, Inc/novacom/novacom.exe")
                        .exists()) {
                    novacom = programPath64 + "/Palm, Inc/novacom/novacom.exe";
                } else if(new File(programPath32 + "/Palm, Inc/novacom/novacom.exe").exists()) {
                    novacom = programPath32 + "/Palm, Inc/novacom/novacom.exe";
                } else if(programPath64!=null && new File(programPath64 + "/Palm/SDK/bin/novacom.exe").exists()) {
                    novacom = programPath64 + "/Palm/SDK/bin/novacom.exe";
                } else if(new File(programPath32 + "/Palm/SDK/bin/novacom.exe").exists()) {
                    novacom = programPath32 + "/Palm/SDK/bin/novacom.exe";
                } else if(programPath64!=null && new File(programPath64 + "/PDK/bin/novacom.exe").exists()) {
                    novacom = programPath64 + "/PDK/bin/novacom.exe";
                } else if(new File(programPath32 + "/PDK/bin/novacom.exe").exists()) {
                    novacom = programPath32 + "/PDK/bin/novacom.exe";
                }
            } else {
                if(new File("C:/Program Files/HP webOS/SDK/bin/novacom.exe").exists()) {
                    novacom = "C:/Program Files/HP webOS/SDK/bin/novacom.exe";
                } else if(new File("C:/Program Files/Palm, Inc/novacom.exe").exists()) {
                    novacom = "C:/Program Files/Palm, Inc/novacom.exe";
                } else if(new File("C:/Program Files/Palm, Inc/novacom/novacom.exe").exists()) {
                    novacom = "C:/Program Files/Palm, Inc/novacom/novacom.exe";
                } else if(new File("C:/Program Files/Palm/SDK/bin/novacom.exe").exists()) {
                    novacom = "C:/Program Files/Palm/SDK/bin/novacom.exe";
                } else if(new File("C:/Program Files/PDK/bin/novacom.exe").exists()) {
                    novacom = "C:/Program Files/PDK/bin/novacom.exe";
                }
            }
        }
        return novacom;
    }

    public static boolean isInstalled() {
        boolean isInstalled = false;
        String execPath = execPath();
        if(execPath.equals("novacom")) {
            try {
                Process p = Runtime.getRuntime().exec("novacom -l");
                OutputStream os = p.getOutputStream();
                os.flush();
                os.close();
                TextStreamConsumer stdout = new TextStreamConsumer(p.getInputStream());
                stdout.start();
                TextStreamConsumer stderr = new TextStreamConsumer(p.getErrorStream());
                stderr.start();
                stdout.waitFor();
                isInstalled = (p.waitFor()==0);
            } catch(Exception e) {}
        } else {
            isInstalled = new File(execPath).exists();
        }
        return isInstalled;
    }

    public static boolean serviceInstalled() {
        boolean installed = false;
        if(new File("/opt/Palm/novacom/novacomd").exists()) { //linux
            installed = true;
        } else if(new File("/opt/nova/bin/novacomd").exists() ||
                new File("/Library/LaunchDaemons/com.palm.novacomd").exists()) { //mac
            installed = true;
        }else { //windows
            String programPath32 = System.getenv("ProgramFiles");
            String programPath64 = null;
            if(programPath32!=null) {
                programPath32 = programPath32.replace("\\", "/").trim();
                if(programPath32.endsWith("(x86)")) {
                    programPath64 = programPath32.substring(0,
                            programPath32.lastIndexOf("(x86)")).trim();
                    programPath64 = programPath64.replace("\\", "/");
                }
                if(new File(programPath32 + "/HP webOS/SDK/bin/novacomd/amd64/novacomd.exe").exists() ||
                        new File(programPath32 + "/HP webOS/SDK/bin/novacomd/x86/novacomd.exe").exists() ||
                        new File(programPath32 + "/Palm, Inc/novacom/amd64/novacomd.exe").exists() ||
                        new File(programPath32 + "/Palm, Inc/novacom/x86/novacomd.exe").exists() ||
                        new File(programPath32 + "/Palm/SDK/novacom/amd64/novacomd.exe").exists() ||
                        new File(programPath32 + "/Palm/SDK/novacom/x86/novacomd.exe").exists() ||
                        new File(programPath32 + "/Palm/SDK/bin/novacom/amd64/novacomd.exe").exists() ||
                        new File(programPath32 + "/Palm/SDK/bin/novacom/x86/novacomd.exe").exists()) {
                    installed = true;
                }
                if(programPath64 != null) {
                    if(new File(programPath64 + "/HP webOS/SDK/bin/novacomd/amd64/novacomd.exe").exists() ||
                            new File(programPath64 + "/HP webOS/SDK/bin/novacomd/x86/novacomd.exe").exists() ||
                            new File(programPath64 + "/Palm, Inc/novacom/amd64/novacomd.exe").exists() ||
                            new File(programPath64 + "/Palm, Inc/novacom/x86/novacomd.exe").exists() ||
                            new File(programPath64 + "/Palm/SDK/novacom/amd64/novacomd.exe").exists() ||
                            new File(programPath64 + "/Palm/SDK/novacom/x86/novacomd.exe").exists() ||
                            new File(programPath64 + "/Palm/SDK/bin/novacom/amd64/novacomd.exe").exists() ||
                            new File(programPath64 + "/Palm/SDK/bin/novacom/x86/novacomd.exe").exists()) {
                        installed = true;
                    }
                }
            } else {
                if(new File("C:/Program Files/HP webOS/SDK/bin/novacomd/amd64/novacomd.exe").exists() ||
                        new File("C:/Program Files/HP webOS/SDK/bin/novacomd/x86/novacomd.exe").exists() ||
                        new File("C:/Program Files/Palm, Inc/novacom/amd64/novacomd.exe").exists() ||
                        new File("C:/Program Files/Palm, Inc/novacom/x86/novacomd.exe").exists() ||
                        new File("C:/Program Files/Palm/SDK/novacom/amd64/novacomd.exe").exists() ||
                        new File("C:/Program Files/Palm/SDK/novacom/x86/novacomd.exe").exists() ||
                        new File("C:/Program Files/Palm/SDK/bin/novacom/amd64/novacomd.exe").exists() ||
                        new File("C:/Program Files/Palm/SDK/bin/novacom/x86/novacomd.exe").exists()) {
                    installed = true;
                }
            }
        }
        if(!installed) {
            try {
                Process p = Runtime.getRuntime().exec("novacom -l");
                OutputStream os = p.getOutputStream();
                os.flush();
                os.close();
                TextStreamConsumer stdout = new TextStreamConsumer(p.getInputStream());
                stdout.start();
                TextStreamConsumer stderr = new TextStreamConsumer(p.getErrorStream());
                stderr.start();
                stdout.waitFor();
                installed = (p.waitFor()==0);
            } catch(Exception e) {}
            }
        return installed;
    }

    public static NovacomDevice[] listDevices() throws NovacomException {
        ResourceBundle locale = ResourceBundle.getBundle("ca/canucksoftware/webos/Locale");
        ArrayList<NovacomDevice> devices = new ArrayList<NovacomDevice>();
        try {
            NovacomSocket ns = new NovacomSocket();
            String line = ns.readline();
            while(line!=null) {
                String[] tokens = line.trim().split("\\s+");
                if(!tokens[3].toLowerCase().contains("bootie")) {
                    devices.add(new NovacomDevice(Integer.parseInt(tokens[0]), tokens[1],
                            tokens[2], tokens[3]));
                }
                line = ns.readline();
            }
            ns.close();
        } catch(Exception e) {
            throw new NovacomException(locale.getString("NOVACOM_DRIVER_IS_NOT_RUNNING_OR_NOT_INSTALLED."));
        }
        return devices.toArray(new NovacomDevice[devices.size()]);
    }

    public static void launchTerminal(NovacomDevice device) {
        Process launcher = null;
        try {
            String platform = System.getProperty("os.name").toLowerCase();
            if(platform.contains("windows") || platform.contains("linux")) {
                String tmpFilePath = System.getProperty("java.io.tmpdir");
                File script = new File(tmpFilePath, "novacom.cmd");
                if(script.exists()) {
                    script.delete();
                }
                BufferedWriter bw = new BufferedWriter(new FileWriter(script));
                bw.write("\"" + execPath() + "\" -d " + device.getId() +
                        " -t open tty://");
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
                        execPath() + " -d" + device.getId() +
                        " -t open tty://\""});
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
            e.printStackTrace();
        }
    }

    public static void restartService() {
        Process restarter = null;
        try {
            String platform = System.getProperty("os.name").toLowerCase();
            if(platform.contains("windows")) {
                restarter = Runtime.getRuntime().exec("net stop novacomd");
                doProcess(restarter);
                restarter = Runtime.getRuntime().exec("net start novacomd");
                doProcess(restarter);
            } else if(platform.contains("mac")) {
                restarter = Runtime.getRuntime().exec("/usr/local/bin/stop-novacomd");
                doProcess(restarter);
                restarter = Runtime.getRuntime().exec("/usr/local/bin/start-novacomd");
                doProcess(restarter);
            } else if(platform.contains("linux")) {
                restarter = Runtime.getRuntime().exec("stop palm-novacomd");
                doProcess(restarter);
                restarter = Runtime.getRuntime().exec("killall -v -q novacomd");
                doProcess(restarter);
                restarter = Runtime.getRuntime().exec("start palm-novacomd");
                doProcess(restarter);
            }
        } catch(Exception e) {
            e.printStackTrace();
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
