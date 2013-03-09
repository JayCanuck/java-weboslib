
package ca.canucksoftware.webos;

/**
 * @author Jason Robitaille
 */
public enum ScriptType {
    ScanID("resources/appid.sh"),
    Patch("resources/patch.sh");

    private String type;
    ScriptType(String val) {
        type = val;
    }
    @Override
    public String toString() {
        return type;
    }
    public String filename() {
        return type.substring(type.indexOf("/")+1);
    }
}
