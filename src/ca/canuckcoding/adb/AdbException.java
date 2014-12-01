
package ca.canuckcoding.adb;

import ca.canuckcoding.webos.WebOSException;

public class AdbException extends WebOSException{
    public AdbException() {
        super();
    }
    public AdbException(String error) {
        super(error);
    }
    public AdbException(Exception error) {
        super(error);
    }
}
