
package ca.canuckcoding.novacom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class NovacomPacket {
    private InputStream input;
    private OutputStream output;
    private Header header;
    private int index;
    private byte[] packet;
    private ArrayList<OOB> oobList;

    public NovacomPacket(InputStream is, OutputStream os) {
        input = is;
        output = os;
        header = new Header();
        index = -1;
        packet = new byte[32768];
        oobList = new ArrayList<OOB>();
    }

    public void writeOOBClose(int file) throws IOException {
        byte[] oob = new byte[20];
        stuffAnInt(0, oob, 0);
        stuffAnInt(file, oob, 4);
        output.write(header.createHeader(oob.length, 2));
        output.write(oob);
    }

    public void write(byte[] data) throws IOException {
        output.write(header.createHeader(data.length, 0));
        output.write(data);
    }

    public void write(byte[] data, int offset, int length) throws IOException {
        output.write(header.createHeader(length, 0));
        output.write(data, offset, length);
    }

    public int read() throws IOException {
        if((index == -1) || (index >= header.getSize())) {
            int val = 2;
            while(val == 2) {
                val = readPacket();
                if(val == -1) {
                    return -1;
                }
            }
        }
        if (index == -1) {
            return -1;
        }
        int result = packet[index] & 0xFF;
        index++;
        return result;
    }
    
    public int readPacket() throws IOException {
        index = -1;
        int val = readArray(header.getHeader(), 16);
        if(val!=16) {
            return -1;
        }
        if(header.getMagic() != -557122643) {
            throw new IOException("Inalid magic number in packet: " + header.getMagic());
        }
        int type = header.getType();
        if(type==0 || type==1) {
            index = 0;
            readArray(packet, header.getSize());
        } else if(type==2) {
            OOB oob = new OOB();
            readArray(oob.getData(), header.getSize());
            oobList.add(oob);
        }
        return type;
    }

    private int readArray(byte[] data, int size) throws IOException {
        int count = 0;
        try {
            while (count < size) {
                /*if(input.available()==0) {
                    return -1;
                }*/
                int val = input.read(data, count, size - count);
                if (val == -1) {
                    return -1;
                }
                count += val;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println(e.getMessage());
        }
        return count;
    }

    public int read(byte[] data) throws IOException {
        int i;
        for(i=0; i<data.length; i++) {
            int curr = read();
            if(curr==-1) {
                if(i==0) {
                    i=-1;
                }
                break;
            }
            data[i] = (byte) curr;
        }
        return i;
    }

    public ArrayList<OOB> getOOBList() {
        return oobList;
    }

    private static void stuffAnInt(int val, byte[] array, int offset) {
        array[(offset + 0)] = (byte)(val >> 0 & 0xFF);
        array[(offset + 1)] = (byte)(val >> 8 & 0xFF);
        array[(offset + 2)] = (byte)(val >> 16 & 0xFF);
        array[(offset + 3)] = (byte)(val >> 24 & 0xFF);
    }

    private static int makeAnInt(int i1, int i2, int i3, int i4) {
        i1 &= 255;
        i2 &= 255;
        i3 &= 255;
        i4 &= 255;
        return (i1 << 0 | i2 << 8 | i3 << 16 | i4 << 24);
    }

    private class Header {
        private static final int TYPE_DATA = 0;
        private static final int TYPE_ERR = 1;
        private static final int TYPE_OOB = 2;
        private byte[] header;
        public Header() {
            header = new byte[16];
        }
        public int getMagic() {
            return makeAnInt(header[0], header[1], header[2], header[3]);
        }
        public int getVersion() {
            return makeAnInt(header[4], header[5], header[6], header[7]);
        }
        public int getSize() {
            return makeAnInt(this.header[8], this.header[9], header[10], header[11]);
        }
        public int getType() {
            return makeAnInt(header[12], header[13], header[14], header[15]);
        }
        public byte[] createHeader(int size, int type) {
            stuffAnInt(-557122643, header, 0);
            stuffAnInt(1, header, 4);
            stuffAnInt(size, header, 8);
            stuffAnInt(type, header, 12);
            return header;
        }
        public byte[] getHeader() {
            return header;
        }
    }

    public class OOB {
        private static final int TYPE_DATA = 0;
        private static final int TYPE_ERR = 1;
        private static final int TYPE_OOB = 2;
        private byte[] obbData;
        public OOB() {
            obbData = new byte[20];
        }
        public int getMessageType() {
            return makeAnInt(obbData[0], obbData[1], obbData[2], obbData[3]);
        }
        public int getMessagePayload() {
            return makeAnInt(obbData[4], obbData[5], obbData[6], obbData[7]);
        }
        public byte[] getData() {
            return obbData;
        }

    }
}
