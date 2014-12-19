package org.ualberta.arc.mergecwrc.ui.applet;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JApplet;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.io.CWRCStringBuilder;
import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory.MergeType;
import org.ualberta.arc.mergecwrc.ui.MergerController;
import org.ualberta.arc.mergecwrc.ui.MultipleMatchModel;

/**
 *
 * @author mpm1
 */
public class AdvancedMergerControllerApplet extends JApplet implements MergerController {

    private final String MERGE_LOCK = "Merge Lock - " + this.hashCode();
    private Map<String, CWRCStringBuilder> filesToMerge = new HashMap<String, CWRCStringBuilder>();

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
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

    private class RunningThread extends Thread {

        private int dataCount;
        private MergeType mergeType;
        private MergerControllerApplet controller;
        private boolean autoMerge;
        private int totalThreads;

        public RunningThread(MergerControllerApplet controller, int dataCount, MergeType mergeType, boolean autoMerge, int totalThreads) {
            this.dataCount = dataCount;
            this.mergeType = mergeType;
            this.controller = controller;
            this.autoMerge = autoMerge;
            this.totalThreads = totalThreads;
        }

        /**
         * Starts a new file to be added to the applet list.
         * 
         * @param name 
         */
        public void startFile(String name) {
            CWRCStringBuilder file = new CWRCStringBuilder(name);

            filesToMerge.put(name, file);
        }

        /** 
         * Adds information to the file.
         * 
         * @param data 
         */
        public void addToFile(String name, String data) {
            CWRCStringBuilder file = filesToMerge.get(name);

            if (file != null) {
                file.addToFile(data);
            }
        }

        /**
         * Closes the current file.
         */
        public void endFile(String name) {
            CWRCStringBuilder file = filesToMerge.get(name);

            try {
                if (file != null) {
                    file.closeFile();
                }
            } catch (CWRCException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            synchronized (MERGE_LOCK) {
                for (CWRCStringBuilder builder : filesToMerge.values()) {
                    InputStream stream = null;
                    
                    try {
                        stream = builder.getAsStream();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        continue;
                    }
                }
            }
        }
    }
}
