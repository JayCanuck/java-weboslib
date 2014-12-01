
package ca.canuckcoding.novacom;

import ca.canuckcoding.webos.WebOSException;

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
