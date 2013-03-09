
package ca.canucksoftware.webos;

import ca.canucksoftware.utils.JarResource;

/**
 * @author Jason Robitaille
 */
public class Script extends JarResource{
    public Script(ScriptType script) {
        super(script.toString(), Script.class);
    }
}
