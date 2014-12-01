
package ca.canuckcoding.webos;

/**
 * @author Jason Robitaille
 */

public abstract class WebOSDevice {
    public abstract String getName();
    public abstract String getId();
    public abstract WebOSConnection connect() throws WebOSException;
}
