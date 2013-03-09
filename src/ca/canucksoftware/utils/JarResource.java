
package ca.canucksoftware.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Jason Robitaille
 */
public class JarResource {
    protected Class specific;
    private String path;
    public JarResource(String resource) {
        this(resource, null);
    }

    public JarResource(String resource, Class specificClass) {
        path = resource;
        specific = specificClass;
    }

    public File extract() {
        String tmpFilePath = System.getProperty("java.io.tmpdir");
        File dest = new File(tmpFilePath, path.substring(path.lastIndexOf("/")+1));
        return extract(dest);
    }

    public File extract(String dest) {
        return extract(new File(dest));
    }

    public File extract(File dest) {
        try {
            if(dest.exists())
                dest.delete();
            InputStream in = null;
            if(specific==null) {
                in = new BufferedInputStream(this.getClass().getResourceAsStream(path));
            } else {
                in = new BufferedInputStream(specific.getResourceAsStream(path));
            }
            OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buffer = new byte[2048];
            for (;;)  {
                int nBytes = in.read(buffer);
                if (nBytes <= 0)
                    break;
                out.write(buffer, 0, nBytes);
            }
            out.flush();
            out.close();
            in.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
