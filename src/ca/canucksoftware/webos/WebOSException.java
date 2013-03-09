
package ca.canucksoftware.webos;

public class WebOSException extends Exception {
    public WebOSException() {
        super();
    }
    public WebOSException(String error) {
        super(error);
    }
    public WebOSException(Exception error) {
        super(error);
        setStackTrace(error.getStackTrace());
    }
}
