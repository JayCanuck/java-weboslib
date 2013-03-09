
package ca.canucksoftware.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Jason Robitaille
 */
public class TextStreamConsumer extends Thread {
    private InputStream is;
    private String output;

    public TextStreamConsumer(InputStream stream) {
        is = stream;
        output = null;
    }

    @Override
    public void run() {
        String result = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            while(line!=null) {
                result += line;
                line = br.readLine();
                if(line!=null) {
                    result += "\n";
                }
            }
            br.close();
            is.close();
        } catch(Exception e) {}
        output = result;
    }

    public void waitFor() {
        try {
            super.join();
        } catch (InterruptedException e) {
            System.err.println("TextStreamConsumer thread interrupted");
        }
    }

    @Override
    public String toString() {
        return output;
    }
}
