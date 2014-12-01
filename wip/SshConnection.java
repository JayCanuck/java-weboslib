
package ca.canuckcoding.ssh;

import ca.canuckcoding.webos.WebOSConnection;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
/*import org.rev6.scf.ScpDownload;
import org.rev6.scf.ScpFile;
import org.rev6.scf.ScpUpload;
import org.rev6.scf.SshCommand;
import org.rev6.scf.SshConnectionWrapper;*/

/**
 *
 * @author Jason
 */
public class SshConnection extends WebOSConnection {
    //private SshConnectionWrapper scw;
    private String host;
    private String username;
    private int port;
    private File key;

    public SshConnection() {
        //scw = null;
        host = Preferences.userRoot().get("ssh-host", "127.0.0.1");
        username = Preferences.userRoot().get("ssh-username", "root");
        port = Preferences.userRoot().getInt("ssh-port", 5522);
        key = new File(Preferences.userRoot().get("ssh-key", "ssh-key"));
    }

    public boolean connect() {
        boolean result = false;
        /*try {
            if(!key.isFile()) {
                throw new Exception("SSH not setup correctly; private key missing.");
            }
            scw = new SshConnectionWrapper(host, username, key);
            scw.setPort(port);
            scw.connect();
            result = true;
        } catch(Exception e) {
            JOptionPane.showMessageDialog(null, "Error 1: " + e.getMessage());
        }*/
        return result;
    }

    public boolean sendFile(File src, String dest) {
        boolean result = false;
        /*try {
            ScpFile scpFile = new ScpFile(src,dest);
            scw.executeTask(new ScpUpload(scpFile));
            result = true;
        } catch(Exception e) {}*/
        return result;
    }

    public boolean receiveFile(String src, File dest) {
        boolean result = false;
        /*try {
            ScpFile scpFile = new ScpFile(dest,src);
            scw.executeTask(new ScpDownload(scpFile));
            result = true;
        } catch(Exception e) {}*/
        return result;
    }

    public String runProgram(String app, String[] params) throws Exception {
        String out = null;
        /*String err = null;
        String cmd = app;
        for(int i=0; i<params.length; i++) {
            cmd += " " + format(params[i]);
        }
        SshCommand sc = new SshCommand(cmd, new StringOutputStream(),
                new StringOutputStream());
        scw.executeTask(sc);
        err = sc.getErrorStream().toString();
        out = sc.getOutputStream().toString();
        if(sc.getExitCode()!=0) {
            throw new Exception(err);
        }*/
        return out;
    }

    private String format(String s) {
        return s.replace("\"", "\\\"").replace(" ", "\\ ");
    }
}
