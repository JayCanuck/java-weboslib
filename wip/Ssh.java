
package ca.canuckcoding.ssh;

/*import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;*/
import java.io.File;
import java.net.InetAddress;

/**
 * @author Jason Robitaille
 */
public class Ssh {
    public static void generateKeys(File publicKey, File privateKey) throws Exception {
        /*String comment = System.getProperty("user.name") + "@" +
                InetAddress.getLocalHost().getHostName();
        JSch jsch=new JSch();
        KeyPair kpair=KeyPair.genKeyPair(jsch, KeyPair.RSA);
        kpair.setPassphrase("");
        kpair.writePrivateKey(privateKey.getCanonicalPath());
        kpair.writePublicKey(publicKey.getCanonicalPath(), comment);
        kpair.dispose();*/
    }
}
