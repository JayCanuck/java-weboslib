
package ca.canuckcoding.webos;

import ca.canuckcoding.utils.OnlineFile;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
 * @author Jason Robitaille
 */
public class Patcher {
    private WebOSConnection webOSCon;
    private ResourceBundle locale;
    private ArrayList<InstalledEntry> installed;
    private DeviceInfo info;
    
    public Patcher(WebOSConnection wc) {
        webOSCon = wc;
        locale = ResourceBundle.getBundle("ca/canuckcoding/webos/Locale");
        installed = wc.listInstalled();
        info = wc.getDeviceInfo();
    }
    
    public boolean meetsRequirements(String ausmtUrl) {
        boolean result = false;
        boolean hasPatch = installed.contains(new InstalledEntry("org.webosinternals.patch"));
        boolean hasLsdiff = installed.contains(new InstalledEntry("org.webosinternals.lsdiff"));
        boolean hasAusmt = installed.contains(new InstalledEntry("org.webosinternals.ausmt"));
        if(!hasPatch || !hasLsdiff || !hasAusmt) {
            if(JOptionPane.showConfirmDialog(null, "<html><body width=\"300px\">" +
                    locale.getString("PATCHING_REQUIRES_BOTH_GNU_PATCH_AND_LSDIFF_INSTALLED_TO_FUNCTION." +
                    "_ONE_OR_BOTH_OF_THESE_ARE_MISSING_FROM_YOUR_DEVICE._WOULD_YOU_LIKE_TO_DOWNLOAD_AND_INSTALL_THEM_NOW?"),
                    locale.getString("REQUIRED_COMPONENT_MISSING"), JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(null, "<html>" +
                        locale.getString("THIS_MAY_TAKE_UPTO_A_MINUTE_TO_COMPLETE,_SO_PLEASE_BE_PATIENT."));
                boolean status = true;
                OnlineFile url = null;
                File local = null;
                if(!hasPatch) {
                    url = new OnlineFile("http://ipkg.preware.net/feeds/webos-internals/" + info.arch()
                            + "/org.webosinternals.patch_2.5.9-4_" + info.arch() + ".ipk");
                    local = url.download();
                    if(local!=null) {
                        status |= webOSCon.install(local);
                        if(status) {
                            JOptionPane.showMessageDialog(null, locale.getString("GNU_PATCH_DOWNLOADED_AND_INSTALLED!"));
                        }
                    }
                }
                if(!hasLsdiff) {
                    url = new OnlineFile("http://ipkg.preware.net/feeds/webos-internals/" + info.arch()
                            + "/org.webosinternals.lsdiff_0.3.1-1_" + info.arch() + ".ipk");
                    local = url.download();
                    if(local!=null) {
                        status |= webOSCon.install(local);
                        if(status) {
                            JOptionPane.showMessageDialog(null, locale.getString("LSDIFF_DOWNLOADED_AND_INSTALLED!"));
                        }
                    }
                }
                if(!hasAusmt) {
                    url = new OnlineFile(ausmtUrl);
                    local = url.download();
                    if(local!=null) {
                        status |= webOSCon.install(local);
                        if(status) {
                            JOptionPane.showMessageDialog(null, locale.getString("AUSMT_DOWNLOADED_AND_INSTALLED!"));
                        }
                    }
                }
                result = status;
            }
        } else {
            result = true;
        }
        return result;
    }

    public boolean install(File file) {
        boolean result = false;
        PatchFile patch = new PatchFile(file);
        String appid = patch.getId();
        if(installed.contains(new InstalledEntry(appid))) {
            if(uninstall(appid)) {
                result = doInstall(patch);
            }
        } else {
            result = doInstall(patch);
        }
        return result;
    }

    private boolean doInstall(PatchFile patch) {
        boolean result = false;
        patch.scan();
        if(webOSCon.mkdir("/media/internal/.developer")) {
            String patchPath = "/media/internal/.developer/" + patch.getPatchFilename();
            if(webOSCon.sendFile(patch, patchPath)) {
                try {
                    String stdout = webOSCon.runProgram("/bin/sh", new String[] {"/var/"
                            + ScriptType.Patch.filename(), patchPath, patch.getIdBase(),
                            patch.getPatchName(), patch.getPatchVersion(info.version()),
                            patch.getPatchAuthor(), patch.getPatchDescription()});
                    webOSCon.lunaRestart();
                    result = true;
                } catch(Exception e) {
                    JOptionPane.showMessageDialog(null, MessageFormat.format(
                            locale.getString("ERROR:_AN_ERROR_OCCURED_WHILE_ATTEMPTING_TO_APPLY_{0}"),
                            new Object[] {patch.getPatchFilename()}) + "\n\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public boolean uninstall(File file) {
        PatchFile patch = new PatchFile(file);
        return uninstall(patch.getId());
    }
    
    public boolean uninstall(String id) {
        return webOSCon.uninstall(id);
    }
}