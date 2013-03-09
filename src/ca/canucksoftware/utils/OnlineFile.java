
package ca.canucksoftware.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Jason Robitaille
 */
public class OnlineFile {
    private String url;
    
    public OnlineFile(URL file) {
        this(file.toString());
    }
    public OnlineFile(String file) {
        url = file;
    }

    public boolean exists() {
        boolean result = false;
        try {
            HttpURLConnection.setFollowRedirects(true);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setInstanceFollowRedirects(true);
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setRequestMethod("HEAD");
            result = (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    public File download() {
        String tmpFilePath = System.getProperty("java.io.tmpdir");
        File output = new File(tmpFilePath, url.substring(url.lastIndexOf("/")+1));
        return download(output);
    }

    public File download(File output) {
        File result = output;
        URLConnection urlCon = null;
        if(exists()) {
            try {
                urlCon = getURL().openConnection();
                urlCon.setRequestProperty("Content-Type", "application/binary");
                if(result.exists()) {
                    result.delete();
                }
                BufferedInputStream bis = new BufferedInputStream(urlCon.getInputStream());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(result));
                byte[] buf = new byte[1024];
                int len;
                while ((len = bis.read(buf)) > 0){
                    bos.write(buf, 0, len);
                }
                bos.flush();
                bis.close();
                bos.close();
            } catch(Exception e) {
                e.printStackTrace();
                if(result.exists()) {
                    result.delete();
                }
                result = null;
            }
        } else {
            if(result.exists()) {
                result.delete();
            }
            result = null;
        }
        return result;
    }
    
    @Override
    public String toString() { return url; }
    public URL getURL() throws MalformedURLException { return new URL(url); }
}
