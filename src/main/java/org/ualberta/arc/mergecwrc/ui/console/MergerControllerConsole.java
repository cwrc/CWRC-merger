package org.ualberta.arc.mergecwrc.ui.console;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import org.ualberta.arc.mergecwrc.ui.MergerController;
import org.ualberta.arc.mergecwrc.ui.MultipleMatchModel;

/**
 *
 * @author mpm1
 */
public class MergerControllerConsole implements MergerController, Runnable {
    private URL apiUrl = null;
    private File file = null;
    
    public static void main(final String args[]){
        if(args.length > 2){
            Authenticator.setDefault(new Authenticator(){
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(args[2], args[3].toCharArray());
                }
            });
        }
        
        Runnable runnable = new MergerControllerConsole(args[0], args[1]);
        
        runnable.run();
    }
    
    public MergerControllerConsole(String apiUrl, String file){       
        // Check if we can access the server
        try {
            this.apiUrl = new URL(apiUrl);
            URLConnection connection = this.apiUrl.openConnection();
        } catch (IOException ex) {
            error(ex.getMessage(), ex);
        }
        
        // Check if thefile exists
        this.file = new File(file);        
        if(!this.file.exists()){
            error("File '" + file + "' does not exist", null);
        }
    }
    
    public void error(String message, Exception ex){
        System.err.println("Error: " + message);
    }
    
    public void addMerge(MultipleMatchModel match) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setTotalEntities(int total) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void incrementCurrentEntities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void run() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
