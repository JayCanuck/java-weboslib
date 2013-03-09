
package ca.canucksoftware.webos;

import org.json.JSONObject;

/**
 * @author Jason Robitaille
 */
public class InstalledEntry {
    private String id;
    private String version;
    private String name;
    private String developer;
    private String description;
    private JSONObject source;
    public InstalledEntry(String ipkgLine) {
        int index = ipkgLine.indexOf(" - ");
        if(index>-1) {
            id = ipkgLine.substring(0, index);
            ipkgLine = ipkgLine.substring(index+3);
            index = ipkgLine.indexOf(" - ");
            if(index>-1) {
                version = ipkgLine.substring(0, index);
                name = ipkgLine.substring(index+3);
            } else {
                name = ipkgLine;
                version = "1.0.0";
            }
        } else {
            id = ipkgLine;
            version = null;
            name = null;
        }
        developer = null;
        description = null;
        source = null;
    }

    public InstalledEntry(String name, String id, String version) {
        this.name = name;
        this.id = id;
        this.version = version;
        developer = null;
        description = null;
        source = null;
    }

    public String getId() { return id; }
    public String getVersion() { return version; }
    public String getName() { return name; }
    public String getDeveloper() { return developer; }
    public String getDescription() { return description; }
    public JSONObject getSource() { return source; }
    public void setName(String newName) { name=newName; }
    public void setDeveloper(String newDeveloper) { developer=newDeveloper; }

    public String getName(WebOSConnection webOS) {
        if(name.equals("This is a webOS application.")) {
            try {
                String out = webOS.runProgram("/bin/grep", new String[] {"\\\"title\\\"",
                        "/media/cryptofs/apps/usr/palm/applications/" + id +
                        "/appinfo.json"});
                out = out.substring(out.indexOf(":")+1).trim();
                out = out.substring(out.indexOf("\"")+1, out.lastIndexOf("\""));
                name = out;
            } catch(Exception e) {
                System.out.println("Unable to get app name from appinfo.json " +
                        "for " + id + ":\n" + e.getMessage());
                e.printStackTrace();
            }
        }
        return name;
    }

    public void parseControl(WebOSConnection webOS) {
        try {
            String out = webOS.runProgram("/bin/grep", new String[] {"-e", "Maintainer:",
                    "-e", "Source:", webOS.offlineRoot + "/usr/lib/ipkg/info/" + id +
                    ".control"});
            String[] lines = out.split("\n");
            for(int i=0; i<lines.length; i++) {
                String value = lines[i].substring(lines[i].indexOf(":")+1).trim();
                if(lines[i].startsWith("Maintainer:")) {
                    if(developer==null) {
                        developer = value;
                    }
                } else if(lines[i].startsWith("Source:")) {
                    if(!value.isEmpty()) {
                        JSONObject controlSrc = new JSONObject(lines[i].substring(lines[i]
                                .indexOf("{"), lines[i].lastIndexOf("}")+1));
                        source = controlSrc;
                        if(controlSrc.has("FullDescription")) {
                            try {
                                description = controlSrc.getString("FullDescription");
                            } catch(Exception e) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Unable to parse control file for " + id);
            System.err.println("\t" + e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if(o instanceof String) {
            String currId = (String) o;
            result = currId.equals(getId());
        } else if(o instanceof InstalledEntry) {
            InstalledEntry appCurr = (InstalledEntry) o;
            result = appCurr.getId().equals(getId());
        }
        return result;
    }
}
