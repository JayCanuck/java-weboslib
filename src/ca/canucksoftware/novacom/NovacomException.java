
package ca.canucksoftware.novacom;

import ca.canucksoftware.webos.WebOSException;

public class NovacomException extends WebOSException{
    public NovacomException() {
        super();
    }
    public NovacomException(String error) {
        super(error);
    }
    public NovacomException(Exception error) {
        super(error);
    }
}
