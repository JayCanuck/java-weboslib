package ca.canuckcoding.ssh;

import java.io.*;

public class StringOutputStream extends OutputStream {
	StringBuffer buffer;

	public StringOutputStream() {
    	buffer=new StringBuffer();
	}

	public StringOutputStream(int size) {
    buffer=new StringBuffer(size);
	}

	public StringOutputStream(StringBuffer buff) {
		if(buff!=null) {
			buffer=buff;
    	} else {
			buffer=new StringBuffer();
  		}
	}

  	public void write(int c) {
    	buffer.append((char)c);
  	}

  	public String toString() {
            try {
                flush();
                close();
            } catch (Exception e) {}
            return buffer.toString();
  	}

	public int length() {
    	return buffer.length();
  	}

  	public void setLength(int len) {
    	buffer.setLength(len);
  	}
}
