
package ca.canucksoftware.novacom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class NovacomCommand extends Thread {
    private NovacomDevice device;
    private NovacomSocket socket;
    private StringBuffer command;
    private String response;
    private int exitCode;
    private File stdoutRedirect;
    private File stdinRedirect;
    private IOException ioException;
    private NovacomException ncException;

    public NovacomCommand(NovacomDevice nd, String action, String path, String[] params)
            throws IOException {
        device = nd;
        socket = new NovacomSocket(device.getPort());
        command = new StringBuffer();
        command.append(action);
        command.append(" ");
        command.append("file://");
        command.append(formatArg(path));
        for(int i=0; i<params.length; i++) {
            command.append(" ");
            command.append(formatArg(params[i]));
        }
        exitCode = 0;
        stdoutRedirect = null;
        stdinRedirect = null;
        ioException = null;
        ncException = null;
    }

    private String formatArg(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace(" ", "\\ ");
    }
    
    public void stdoutFile(File redirect) {
        stdoutRedirect = redirect;
    }

    public void stdinFile(File input) {
        stdinRedirect = input;
    }

    @Override
    public void run() {
        try {
            socket.write(command.toString() + "\n");
            String reply = socket.readline();
            socket.setPacketMode(true);
            if(reply==null) {
                ncException = new NovacomException("No data to read from socket");
            } else if(reply.startsWith("ok")) {
                if(stdinRedirect!=null) {
                    writeFileToOutputStream(stdinRedirect);
                    socket.closeInputOutput();
                    try {
                        exitCode = socket.getExitCode();
                    } catch(Exception e1) {}
                } else if(stdoutRedirect!=null) {
                    writeInputStreamToFile(stdoutRedirect);
                    socket.closeInputOutput();
                    try {
                        exitCode = socket.getExitCode();
                    } catch(Exception e2) {}
                } else {
                    response = collectResponse();
                    try {
                        exitCode = socket.getExitCode();
                    } catch(NovacomException ne) {
                        ncException = ne;
                    }
                }
            } else {
                ncException = new NovacomException(reply);
            }
        } catch(IOException e) {
            System.err.println(e.getMessage());
            ioException = e;
            exitCode = -1;
        }
    }

    private void writeInputStreamToFile(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        byte[] buffer = new byte[2048];
        for (;;)  {
            int nBytes = socket.read(buffer);
            if (nBytes <= 0)
                break;
            fos.write(buffer, 0, nBytes);
        }
        fos.flush();
        fos.close();
    }

    private void writeFileToOutputStream(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] buffer = new byte[2048];
        for (;;)  {
            int nBytes = fis.read(buffer);
            if(nBytes <= 0)
                break;
            socket.write(buffer, 0, nBytes);
        }
        socket.flush();
        fis.close();
    }

    private String collectResponse() throws IOException {
        String result = "";
        String line = socket.readline();
        while(line!=null) {
            result += line;
            line = socket.readline();
            if(line!=null) {
                result += "\n";
            }
        }
        return result;
    }

    public int waitFor() throws InterruptedException, IOException, NovacomException {
        join();
        socket.flush();
        if(stdinRedirect==null && stdoutRedirect==null) {
            socket.closeInputOutput();
        }
        socket.close();
        if(ioException!=null) {
            throw ioException;
        }
        if(ncException!=null) {
            throw ncException;
        }
        return exitCode;
    }

    public String getResponse() {
        return response;
    }

    public int getExitCode() {
        return exitCode;
    }
}
