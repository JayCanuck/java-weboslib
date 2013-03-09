
package ca.canucksoftware.webos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ResourceBundle;

/**
 * @author Jason Robitaille
 */
public class PatchFile extends File {
    private ResourceBundle locale;
    private String name;
    private String base;
    private String id;
    private String metaName;
    private String metaVersion;
    private String metaAuthor;
    private String metaDescription;

    public PatchFile(File patch) {
       this(patch.getAbsolutePath());
    }
    public PatchFile(String path) {
        super(path);
        locale = ResourceBundle.getBundle("ca/canucksoftware/webos/Locale");
        name = getFileName(path);
        base = name.substring(0, name.lastIndexOf(".")).toLowerCase()
                .replaceAll("[^a-zA-Z0-9-]", "");
        base = base.replace(" ", "");
        id = "ca.canucksoftware.patches." + base;
        name = base + ".patch";
    }

    private String getFileName(String path) {
        String result = path;
        int index = result.lastIndexOf("/");
        if(index!=-1) {
            result = result.substring(index+1);
        }
        index = result.lastIndexOf("\\");
        if(index!=-1) {
            result = result.substring(index+1);
        }
        return result;
    }

    public void scan() {
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader br = new BufferedReader(new FileReader(this));
            String line = br.readLine();
            boolean isMeta=true;
            while(line!=null) {
                if(isMeta) {
                    if(line.startsWith("Name:")) {
                        metaName = parseLine(line);
                    } else if(line.startsWith("Version:")) {
                        metaVersion = parseLine(line);
                        String[] tokens = metaVersion.split("-");
                        if(tokens.length==2) {
                            metaVersion = tokens[1];
                        } else {
                            metaVersion = null;
                        }
                    } else if(line.startsWith("Author:")) {
                        metaAuthor = parseLine(line);
                    } else if(line.startsWith("Description:")) {
                        metaDescription = parseLine(line);
                    }
                }
                if(line.startsWith("+++")) {
                    isMeta = false;
                }
                sb.append(line+"\n");
                line = br.readLine();
            }
            br.close();
            BufferedWriter out=new BufferedWriter (new FileWriter(this));
            out.write(sb.toString());
            out.flush();
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private String parseLine(String s) {
        int i = s.indexOf(":");
        String result = null;
        if(i>-1) {
            result = s.substring(i+1).trim();
        }
        return result;
    }

    public String getIdBase() { return base; }
    public String getId() { return id; }
    public String getPatchFilename() { return name; }

    public String getPatchName() {
        if(metaName==null || metaName.length()==0) {
            metaName = base;
        }
        return metaName;
    }

    public String getPatchVersion(String webOSVersion) {
        String result = webOSVersion;
        if(metaVersion==null || metaVersion.length()==0) {
            result += "-1";
        } else {
            result += "-" + metaVersion;
        }
        return result;
    }

    public String getPatchAuthor() {
        if(metaAuthor==null || metaAuthor.length()==0) {
            metaAuthor = locale.getString("UNKNOWN");
        }
        return metaAuthor;
    }

    public String getPatchDescription() {
        if(metaDescription==null || metaDescription.length()==0) {
            metaDescription = locale.getString("UNKNOWN");
        }
        return metaDescription;
    }
}
