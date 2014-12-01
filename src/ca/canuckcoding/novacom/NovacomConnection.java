
package ca.canuckcoding.novacom;

import ca.canuckcoding.utils.TextStreamConsumer;
import ca.canuckcoding.webos.WebOSConnection;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.swing.JOptionPane;
import org.json.JSONObject;

public class NovacomConnection extends WebOSConnection {
    private NovacomDevice device;
    private boolean loggedIn;
    public NovacomConnection(NovacomDevice nd) {
        super(nd);
        device = nd;
        loggedIn = false;
    }

    public boolean isConnected() {
        boolean result = false;
        try {
            result = device.isConnected();
        } catch(Exception e) {}
        return result;
    }

    public boolean login() throws NovacomException {
        boolean result = false;
        boolean savePass = false;
        Process launcher = null;
        String password = device.getCachedPassword();
        if(password==null) {
            if(device.isPasswordProtected()) {
                LoginDialog dialog = new LoginDialog(device);
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                password = dialog.getPassword();
                savePass = dialog.savePassword();
            } else {
                result = true;
            }
        }
        if(password!=null) {
            try {
                launcher = Runtime.getRuntime().exec(new String[] {Novacom.execPath(), "-d", device.getId(),
                        "-c", "login", "-r", password});
            } catch(IOException e) {
                launcher = null;
                throw new NovacomException(locale.getString("NOVACOM_DRIVER_IS_NOT_RUNNING_OR_NOT_INSTALLED."));
            }
            if(launcher!=null) {
                try {
                    OutputStream os = launcher.getOutputStream();
                    os.flush();
                    os.close();
                    TextStreamConsumer stdout = new TextStreamConsumer(launcher.getInputStream());
                    stdout.start();
                    TextStreamConsumer stderr = new TextStreamConsumer(launcher.getErrorStream());
                    stderr.start();
                    int exitCode = launcher.waitFor();
                    stdout.waitFor();
                    stderr.waitFor();
                    String response = stdout.toString();
                    if(!response.endsWith("\n")) {
                        response += "\n";
                    }
                    response += stderr.toString();
                    if(exitCode==0) {
                        result = true;
                        if(savePass) {
                            device.cachePassword(password);
                        }
                    } else {
                        int choice = JOptionPane.showConfirmDialog(null, locale.getString("NOVACOM_LOGIN_FAILED"),
                                locale.getString("ERROR"), JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.ERROR_MESSAGE);
                        device.cachePassword(null);
                        if(choice==JOptionPane.OK_OPTION) {
                            return login();
                        } else {
                            throw new Exception(response);
                        }
                    }
                } catch(Exception e) {
                    System.err.println("LOGIN ERROR: " + Novacom.execPath() + " -d " + device.getId() +
                            " -c login -r \"" + password + "\"");
                    e.printStackTrace();
                }
            }
        }
        loggedIn = result;
        return result;
    }

    public boolean sendFile(File src, String dest) {
        boolean result = true;
        try {
            if(!loggedIn) {
                login();
            }
            NovacomCommand nCmd = new NovacomCommand(device, "put", dest, new String[]{});
            nCmd.stdinFile(src);
            nCmd.start();
            nCmd.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public boolean receiveFile(String src, File dest) {
        boolean result = true;
        try {
            if(!loggedIn) {
                login();
            }
            NovacomCommand nCmd = new NovacomCommand(device, "get", src, new String[]{});
            nCmd.stdoutFile(dest);
            nCmd.start();
            nCmd.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public String runProgram(String app, String[] params) throws NovacomException {
        String output = null;
        try {
            if(!loggedIn) {
                login();
            }
            NovacomCommand nCmd = new NovacomCommand(device, "run", app, params);
            nCmd.start();
            nCmd.waitFor();
            output = nCmd.getResponse();
            if(nCmd.getExitCode()!=0) {
                throw new NovacomException(output);
            }
        } catch (IOException e) {
            throw new NovacomException(e);
        } catch (InterruptedException e) {
            throw new NovacomException(e);
        }
        return output;
    }

    public List<JSONObject> lunaSend(String address, JSONObject params)
            throws NovacomException {
        List results = null;
        try {
            if(!loggedIn) {
                login();
            }
            NovacomLunaSend nls = new NovacomLunaSend(device, address, params.toString());
            nls.start();
            nls.waitFor();
            results = nls.getResponse();
            if(!nls.returnValue()) {
                String errMsg = "";
                for(int i=0; i<results.size(); i++) {
                    errMsg += results.get(i).toString();
                    if(i<results.size()-1) {
                        errMsg += "\n";
                    }
                }
                throw new NovacomException(errMsg);
            }
        } catch (IOException e) {
            throw new NovacomException(e);
        } catch (InterruptedException e) {
            throw new NovacomException(e);
        }
        return results;
    }

    public void launchTerminal() {
        try {
            if(!loggedIn) {
                login();
            }
            Novacom.launchTerminal(device);
        } catch(Exception e) {}
    }
}