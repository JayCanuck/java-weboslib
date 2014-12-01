
package ca.canuckcoding.webos;

import ca.canuckcoding.utils.JarResource;

/**
 * @author Jason Robitaille
 */
public class Script extends JarResource{
    public Script(ScriptType script) {
        super(script.toString(), Script.class);
    }
}
