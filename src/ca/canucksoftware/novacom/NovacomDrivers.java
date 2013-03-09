
package ca.canucksoftware.novacom;

import ca.canucksoftware.utils.TextStreamConsumer;
import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.zip.GZIPInputStream;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.util.ResourceBundle;

/**
 * @author Jason Robitaille
 */
public class NovacomDrivers {
    private Driver driver;
    private JProgressBar pBar;
    private JLabel label;

    public NovacomDrivers() {
        pBar = null;
        label = null;
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows")) {
            if(System.getenv("ProgramFiles(x86)")==null) {
                driver = Driver.Windows_x86;
            } else {
                driver = Driver.Windows_x64;
            }
        } else if(os.contains("mac")) {
            driver = Driver.Mac;
        } else if(os.contains("linux")) {
            if(!is64bitLinux()) {
                driver = Driver.Linux_x86;
            } else {
                driver = Driver.Linux_x64;
            }
        }
    }

    public void setGUI(JLabel lbl, JProgressBar bar) {
        label = lbl;
        pBar = bar;
    }

    private boolean is64bitLinux() {
        boolean result = false;
        try {
            Process p = Runtime.getRuntime().exec("dpkg --print-architecture");
            OutputStream os = p.getOutputStream();
            os.flush();
            os.close();
            TextStreamConsumer stdout = new TextStreamConsumer(p.getInputStream());
            stdout.start();
            TextStreamConsumer stderr = new TextStreamConsumer(p.getErrorStream());
            stderr.start();
            stdout.waitFor();
            result = !stdout.toString().toLowerCase().contains("i386");
        } catch (IOException e) {
            System.err.println("Unable to check Linux system architecture");
        }
        return result;
    }

    public boolean install() {
        boolean result = false;
        if(driver==Driver.Windows_x86 || driver==Driver.Windows_x64) {
            result = installForWindows();
        } else if(driver==Driver.Mac) {
            result = installForMac();
        } else if(driver==Driver.Linux_x86 || driver==Driver.Linux_x64) {
            result = installForLinux();
        }
        return result;
    }

    public boolean installForWindows() {
        boolean result = false;
        File installer = extractInstaller();
        if(installer!=null) {
            if(label!=null) {
                label.setText("<html>Installing driver...");
            }
            String command = "msiexec /i " + installer.getAbsolutePath() + " /passive";
            if(isStandaloneInstalled()) {
                command = "msiexec /i " + installer.getAbsolutePath()+ " REINSTALL=ALL REINSTALLMODE=vomus /norestart /passive";
            }
            try {
                Process p = Runtime.getRuntime().exec(command);
                OutputStream os = p.getOutputStream();
                os.flush();
                os.close();
                TextStreamConsumer stdout = new TextStreamConsumer(p.getInputStream());
                stdout.start();
                TextStreamConsumer stderr = new TextStreamConsumer(p.getErrorStream());
                stderr.start();
                result = (p.waitFor()==0);
                stdout.waitFor();
                stderr.waitFor();
                installer.delete();
                result = true;
            } catch (Exception e) {
                System.err.println("Unable to install " + driver.file());
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean installForMac() {
        boolean result = false;
        File installer = extractInstaller();
        if(installer!=null) {
            if(label!=null) {
                label.setText("<html>Installing driver...");
            }
            try {
                Process p = Runtime.getRuntime().exec("open -W " + installer.getAbsolutePath());
                OutputStream os = p.getOutputStream();
                os.flush();
                os.close();
                TextStreamConsumer stdout = new TextStreamConsumer(p.getInputStream());
                stdout.start();
                TextStreamConsumer stderr = new TextStreamConsumer(p.getErrorStream());
                stderr.start();
                result = (p.waitFor()==0);
                stdout.waitFor();
                stderr.waitFor();
                deleteItem(installer);
                result = true;
            } catch (Exception e) {
                System.err.println("Unable to install " + driver.file());
            }
        }
        return result;
    }

    private boolean deleteItem(File path) {
        if(path.isDirectory()) {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteItem(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return(path.delete());
    }

    public boolean installForLinux() {
        boolean result = false;
        File installer = extractInstaller();
        if(installer!=null) {
            if(label!=null) {
                label.setText("<html>Installing driver...");
            }
            if(installer!=null) {
                try {
                    Process p = Runtime.getRuntime().exec("xterm +hold -e sudo dpkg -i "
                            + installer.getAbsolutePath());
                    OutputStream os = p.getOutputStream();
                    os.flush();
                    os.close();
                    TextStreamConsumer stdout = new TextStreamConsumer(p.getInputStream());
                    stdout.start();
                    TextStreamConsumer stderr = new TextStreamConsumer(p.getErrorStream());
                    stderr.start();
                    result = (p.waitFor()==0);
                    stdout.waitFor();
                    stderr.waitFor();
                    installer.delete();
                    result = true;
                } catch (Exception e) {
                    System.err.println("Unable to install " + driver.file());
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private File extractInstaller() {
        String tmpFilePath = System.getProperty("java.io.tmpdir");
        File result = new File(tmpFilePath, driver.file());
        if(result.exists()) {
            deleteItem(result);
        }
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(2);
        TarInputStream tis = null;
        try {
            HttpURLConnection urlCon = (HttpURLConnection) new URL(driver.url()).openConnection();
            urlCon.setInstanceFollowRedirects(true);
            urlCon.setRequestProperty("REFERER", driver.url());
            urlCon.connect();
            tis = new TarInputStream(new GZIPInputStream((urlCon.getInputStream())));
            TarEntry entry = tis.getNextEntry();
            byte data[] = new byte[2048];
            int count = 0;
            int kbCount = 0;
            while(entry!=null) {
                if(!entry.getName().endsWith("hp_license_agreement.pdf")) {
                    File curr = new File(tmpFilePath, entry.getName());
                    if(entry.isDirectory()) {
                        curr.mkdirs();
                    } else {
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(curr));
                        while((count = tis.read(data)) != -1) {
                            if(label!=null) {
                                if(kbCount<1000) {
                                    pBar.setString(kbCount + " KB");
                                } else {
                                    pBar.setString(nf.format(((double) kbCount)/1024.0) + " MB");
                                }
                            }
                            kbCount+=2;
                            out.write(data, 0, count);
                        }
                        out.flush();
                        out.close();

                        if(driver==Driver.Mac) {
                            try {
                                Process p = Runtime.getRuntime().exec("chmod ugoa+x " + curr.getAbsolutePath());
                                OutputStream os = p.getOutputStream();
                                os.flush();
                                os.close();
                                TextStreamConsumer stdout = new TextStreamConsumer(p.getInputStream());
                                stdout.start();
                                TextStreamConsumer stderr = new TextStreamConsumer(p.getErrorStream());
                                stderr.start();
                                p.waitFor();
                                stdout.waitFor();
                                stderr.waitFor();
                            } catch (Exception e) {
                                System.err.println("Unable to chmod " + curr.getAbsolutePath());
                            }
                        }
                    }
                }
                entry = tis.getNextEntry();
            }
            tis.close();
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("Unable to extract " + driver.file());
            result = null;
        }
        return result;
    }


    private boolean isStandaloneInstalled() {
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
                if(new File(programPath32 + "/Palm, Inc/novacom/amd64/novacomd.exe").exists() ||
                        new File(programPath32 + "/Palm, Inc/novacom/x86/novacomd.exe").exists()) {
                    installed = true;
                }
                if(programPath64 != null) {
                    if(new File(programPath64 + "/Palm, Inc/novacom/amd64/novacomd.exe").exists() ||
                            new File(programPath64 + "/Palm, Inc/novacom/x86/novacomd.exe").exists()) {
                        installed = true;
                    }
                }
            } else {
                if(new File("C:/Program Files/Palm, Inc/novacom/amd64/novacomd.exe").exists() ||
                        new File("C:/Program Files/Palm, Inc/novacom/x86/novacomd.exe").exists()) {
                    installed = true;
                }
            }
        }
        return installed;
    }

    private enum Driver {
        Windows_x86("NovacomInstaller_x86.msi", "http://downloads.help.palm.com/opensource/novacom/novacom-win-32.tgz"),
        Windows_x64("NovacomInstaller_x64.msi", "http://downloads.help.palm.com/opensource/novacom/novacom-win-64.tgz"),
        Mac("NovacomInstaller.pkg", "http://downloads.help.palm.com/opensource/novacom/novacom-mac.tgz"),
        Linux_x86("palm-novacom_1.0.76_i386.deb", "http://downloads.help.palm.com/opensource/novacom/novacom-linux-32.tgz"),
        Linux_x64("palm-novacom_1.0.76_amd64.deb", "http://downloads.help.palm.com/opensource/novacom/novacom-linux-64.tgz");

        private String file, url;
        Driver(String file, String url) {
            this.file = file;
            this.url = url;
        }
        public String file() { return file; }
        public String url() { return url; }
    }
}
