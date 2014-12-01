
package ca.canuckcoding.adb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AdbCommand extends Thread {
    private final int FILE_MODE = 0664;    
    private AdbDevice device;
    private AdbSocket socket;
    private String actionCode;
    private StringBuffer command;
    private String response;
    private int exitCode;
    private File stdoutRedirect;
    private File stdinRedirect;
    private IOException ioException;
    private AdbException adbException;

    public AdbCommand(AdbDevice nd, String action, String path, String[] params)
            throws IOException {
        device = nd;
        socket = new AdbSocket(device.getHost(), device.getPort());
        actionCode = action;
        command = new StringBuffer();
        boolean sync = isSync();
        if(!sync) {
            command.append(action);
            command.append(":");
        }
        command.append(formatArg(path));
        for(int i=0; i<params.length; i++) {
            command.append(" ");
            command.append(formatArg(params[i]));
        }
        if(actionCode.equalsIgnoreCase("SEND")) {
            command.append(",");
            command.append(Integer.toString(FILE_MODE));
        } else if(!sync) {
            command.append(" ; echo $?");
        }
        exitCode = 0;
        stdoutRedirect = null;
        stdinRedirect = null;
        ioException = null;
        adbException = null;
    }
    
    private boolean isSync() {
        return actionCode.equalsIgnoreCase("RECV") || actionCode.equalsIgnoreCase("SEND");
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
            if(socket.sendCommand("host:transport:" + device.getId())) {
                boolean okay = false;
                if(!isSync()) {
                    okay = socket.sendCommand(command.toString());
                } else {
                    okay = socket.sendSyncAction(actionCode, command.toString());
                }
                if(okay) {
                    if(stdinRedirect!=null) {
                        writeFileToOutputStream(stdinRedirect);
                    } else if(stdoutRedirect!=null) {
                        writeInputStreamToFile(stdoutRedirect);
                    } else {
                        response = collectResponse();
                    }
                } else {
                    adbException = new AdbException("Adb command failure");
                }
            } else {
                adbException = new AdbException("Adb failure: unable to connect to device");
            }
        } catch(IOException e) {
            System.err.println(e.getMessage());
            ioException = e;
            exitCode = -1;
        }
    }

    private void writeInputStreamToFile(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        byte data[] = new byte[1024 * 64];
        int n = socket.readChunk(data);
        while(n>0) {
            fos.write(data, 0, n);
            n = socket.readChunk(data);
        }
        fos.flush();
        fos.close();
    }

    private void writeFileToOutputStream(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] data = new byte[1024*64];
        int n = fis.read(data);
        while(n>0) {
            socket.writeChunk(data, 0, n);
            n = fis.read(data);
        }
        socket.sendStatus("DONE", (int)f.lastModified());
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
        if(!isSync()) {
            int last = result.lastIndexOf("\n");
            if(last>0) {
                try {
                    exitCode = Integer.parseInt(result.substring(last).trim());
                    result = result.substring(0, last);
                } catch(Exception e) {
                    exitCode = -1;
                }
            } else {
                try {
                    exitCode = Integer.parseInt(result.trim());
                    result = "";
                } catch(Exception e) {
                    exitCode = -1;
                }
            }
        }
        return result;
    }

    public int waitFor() throws InterruptedException, IOException, AdbException {
        join();
        socket.flush();
        socket.close();
        if(ioException!=null) {
            throw ioException;
        }
        if(adbException!=null) {
            throw adbException;
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
