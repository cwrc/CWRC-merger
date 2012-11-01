package org.ualberta.arc.mergecwrc.ui.applet;

import java.applet.Applet;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.SwingUtilities;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.ualberta.arc.mergecwrc.utils.JavaScriptUtil;

/**
 *
 * @author mpm1
 */
public class AppletConsole extends OutputStream implements Runnable{
    private StringBuilder output = new StringBuilder();
    private final Long outputLock = System.currentTimeMillis();
    private String functionName;
    private Applet applet;

    public AppletConsole(String functionName, Applet applet) {
        this.functionName = functionName;
        this.applet = applet;
    }

    @Override
    public void write(final int b) {
        updateText(String.valueOf((char) b));
    }

    @Override
    public void write(byte[] b, int off, int len) {
        updateText(new String(b, off, len));
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    private void updateText(final String text) {
        synchronized(outputLock){
            output.append(text);
        }
        
        SwingUtilities.invokeLater(this);
        
    }

    public void run() {
        try{
        JSObject jso = JSObject.getWindow(applet);
        
        synchronized(outputLock){
            JavaScriptUtil.callFunction(jso,
                    functionName, new Object[]{output.toString()});
            output = new StringBuilder();
        }
        }catch(JSException ex){
            ex.printStackTrace();
        }
    }
}
