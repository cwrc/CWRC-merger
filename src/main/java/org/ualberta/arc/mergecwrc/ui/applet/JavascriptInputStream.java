package org.ualberta.arc.mergecwrc.ui.applet;

import java.applet.Applet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.utils.JavaScriptUtil;

/**
 * A helper class used to conveniently transfer arrays of bytes from Javascript to our applet.
 * @author mpm1
 */
public class JavascriptInputStream extends InputStream {

    private int cursor = 0;
    private ReadingDataThread readThread = null;
    private byte[] byteData = null;
    private long appletSleepTime = 15000;

    public JavascriptInputStream(Applet caller, String function, Object[] params) {
        //TODO: Check if this code works in multiple browsers
        JSObject js = JSObject.getWindow(caller);
        JSObject data = (JSObject) JavaScriptUtil.callFunction(js, function, params);

        readThread = new ReadingDataThread(data);
        readThread.start();
    }

    @Override
    public synchronized void mark(int i) {
        if (i < 0) {
            cursor = 0;
        } else {
            cursor = i;
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        mark(0);
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read(byte[] bytes, int offset, int maxSize) throws IOException {
        int checkSize = cursor + maxSize;
        while (!checkThread(checkSize)) {
            try {
                // This allows us to wait unitl the thread has read enough data to warrent the reading.
                Thread.sleep(appletSleepTime);
            } catch (InterruptedException ex) {
                // Just forces the loop to rerun.
            }
        }

        int readCount = byteData.length - cursor;
        if (readCount <= 0) {
            return -1;
        }
        if (maxSize < readCount) {
            readCount = maxSize;
        }

        System.arraycopy(byteData, cursor, bytes, offset, readCount);

        cursor += readCount;

        return readCount;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    @Override
    public int read() throws IOException {
        byte[] value = new byte[1];

        int result = read(value, 0, 1);

        if (result < 0) {
            return result;
        }

        return (value[0] & 0xFF);
    }

    private boolean checkThread(int checkSize) {
        try {
            if (readThread != null) {
                if (readThread.isAlive()) {
                    if (readThread.getSize() >= checkSize) {
                        byteData = readThread.getArray();
                    } else {
                        return false;
                    }
                } else {
                    byteData = readThread.getArray();
                    readThread.close();
                    readThread = null;
                }
            }
        } catch (CWRCException ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    private static class ReadingDataThread extends Thread {

        private JSObject data;
        private ByteArrayOutputStream byteData = new ByteArrayOutputStream(1024); // set the initial size to 1mb

        public ReadingDataThread(JSObject data) {
            this.data = data;
        }

        public int getSize() {
            return byteData.size();
        }

        public byte[] getArray() {
            return byteData.toByteArray();
        }

        public void close() throws CWRCException {
            try {
                byteData.close();
            } catch (IOException ex) {
                throw new CWRCException(ex);
            }
        }

        @Override
        public void run() {
            Number dataVal = null;

            try {
                for (int i = 0; (dataVal = (Number) data.getSlot(i)) != null; ++i) {
                    byteData.write(dataVal.intValue());
                }
            } catch (JSException ex) {
                // This means the conversion is complete in some browsers.
            }

            System.out.println("Finished reading input file.");
        }
    }
}
