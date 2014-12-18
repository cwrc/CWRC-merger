package org.ualberta.arc.mergecwrc.ui.applet;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JApplet;
import netscape.javascript.JSObject;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.MergeReport;
import org.ualberta.arc.mergecwrc.io.CWRCAPIOutput;
import org.ualberta.arc.mergecwrc.io.CWRCStringBuilder;
import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory;
import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory.MergeType;
import org.ualberta.arc.mergecwrc.ui.MergerController;
import org.ualberta.arc.mergecwrc.ui.MultipleMatchModel;
import org.ualberta.arc.mergecwrc.utils.JSONUtil;
import org.ualberta.arc.mergecwrc.utils.JavaScriptUtil;

/**
 *
 * @author mpm1
 */
public class MergerControllerApplet extends JApplet implements MergerController {

    /** function(text) **/
    private static final String PARAM_CONSOLE_FUNC = "consoleFunction";
    /** function(text) **/
    private static final String PARAM_REPORT_FUNC = "reportFunction";
    /** function(jsonText) **/
    private static final String PARAM_MERGE_CHANGE_FUNC = "mergeChangeFunction";
    /** function() **/
    private static final String PARAM_APPLET_INITIALIZED = "initializedFunction";
    /** function() **/
    private static final String PARAM_MERGE_COMPLETE = "completedFunction";
    /** The url to CWRC **/
    private static final String PARAM_CWRC_URL = "cwrcUrl";
    /* Variables */
    private AppletConsole console = null;
    private List<MultipleMatchModel> currentMerge = new ArrayList<MultipleMatchModel>();
    private Map<String, CWRCStringBuilder> filesToMerge = new HashMap<String, CWRCStringBuilder>();
    private boolean merging = false;
    private int totalComplete = 0;
    private int totalEntities = 0;
    private String mergeChangeFunc = null;

    /**
     * Runs the merge process on a selected list of files.
     * 
     * @param files The files to be merged.
     * @param mergeType The type of merge to be performed as defined in org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory.MergeType
     * @param totalThreads Defines the total number of threads to be used during the merging process.
     * @param autoMerge Defines whether or not to auto merge.
     */
    public synchronized void runMerge(int dataCount, final String mergeType, boolean autoMerge, int totalThreads) {
        Thread runThread = new RunningThread(this, dataCount, MergeType.valueOf(mergeType), autoMerge, totalThreads);
        runThread.start();
    }

    /**
     * Returns the total progress of the merge as a json object.
     * 
     * The object is structured as:
     * { numComplete: (total completed), total: (total entities to do) }
     * 
     * @return The system progress.
     */
    public synchronized String getProgress() {
        // Temporary code for testing.
        StringBuilder out = new StringBuilder("{\"numComplete\": ");
        out.append(totalComplete);
        out.append(",\"total\": ");
        out.append(totalEntities);
        out.append("}");

        return out.toString();
    }

    @Override
    public void stop() {
        //CWRCNetworkThread.getInstance().triggerStop();

        super.stop();
    }

    @Override
    public void init() {
        super.init();

        mergeChangeFunc = this.getParameter(PARAM_MERGE_CHANGE_FUNC);

        String function = this.getParameter(PARAM_CONSOLE_FUNC);

        if (function != null) {
            console = new AppletConsole(function, this);
            // Currently we cannot set the system.out functionality. We will work on this later.
            System.setOut(new PrintStream(console));
        }

        //CWRCNetworkThread.getInstance().start();
    }

    @Override
    public void start() {
        super.start();

        //Tell the page that the applet has been initialized.
        String function = this.getParameter(PARAM_APPLET_INITIALIZED);

        if (function != null) {
            JSObject jso = JSObject.getWindow(this);
            JavaScriptUtil.callFunction(jso, function, new Object[]{});
        }
    }

    @Override
    public synchronized void addMerge(MultipleMatchModel match) {
        currentMerge.add(match);

        try {
            sendMergeSelectInfo();
        } catch (CWRCException ex) {
            ex.printStackTrace();
        }
    }

    private synchronized void removeMerge(MultipleMatchModel match) {
        currentMerge.remove(match);

        try {
            sendMergeSelectInfo();
        } catch (CWRCException ex) {
            ex.printStackTrace();
        }
    }

    public void newEntity(int multipleMatchId) {
        MultipleMatchModel matches = currentMerge.get(multipleMatchId);

        matches.setSelection(null);

        removeMerge(matches);
    }

    public void mergeEntity(int multipleMatchId, int matchId) {
        MultipleMatchModel matches = currentMerge.get(multipleMatchId);

        matches.setSelection(matches.getPossibleMatches().get(matchId));

        removeMerge(matches);
    }

    private void sendMergeSelectInfo() throws CWRCException {
        if (mergeChangeFunc != null) {
            try {
                JSObject jso = JSObject.getWindow(this);

                String newTable = JSONUtil.convertMultipleMatchListToJson(currentMerge);
                JavaScriptUtil.callFunction(jso, mergeChangeFunc, new Object[]{newTable});
            } catch (Exception ex) {
                throw new CWRCException(ex);
            }
        }
    }

    public void setTotalEntities(int total) {
        totalComplete = 0;
        totalEntities = total;
    }

    public void incrementCurrentEntities() {
        ++totalComplete;
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

        @Override
        public void run() {
            try {
                if (!merging) {
                    merging = true;
                    List<InputStream> inputFiles = new ArrayList<InputStream>(filesToMerge.size());

                    for (CWRCStringBuilder builder : filesToMerge.values()) {
                        inputFiles.add(builder.getAsStream());
                    }

                    System.out.println("Obtaining merger factory.");
                    MergeReport report = new MergeReport("Applet Merge", new AppletConsole(controller.getParameter(PARAM_REPORT_FUNC), controller));
                    CWRCMergerFactory.setMaxThreads(totalThreads);
                    CWRCMergerFactory merger = CWRCMergerFactory.getFactory(controller, mergeType, report, inputFiles, autoMerge);

                    inputFiles.clear();

                    CWRCAPIOutput mainData = new CWRCAPIOutput(controller.getParameter(PARAM_CWRC_URL), mergeType, controller);

                    for (CWRCStringBuilder builder : filesToMerge.values()) {
                        merger.mergeFile(mainData, builder);
                    }

                    filesToMerge.clear();
                    String function = controller.getParameter(PARAM_MERGE_COMPLETE);

                    totalComplete = 0;
                    totalEntities = 0;

                    if (function != null) {
                        JSObject jso = JSObject.getWindow(controller);
                        JavaScriptUtil.callFunction(jso, function, new Object[]{});
                    }

                    merging = false;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /*@Override
        public void run() {
        try {
        if (!merging) {
        CWRCXMLRequest.reset();
        
        merging = true;
        
        List<InputStream> inputFiles = new ArrayList<InputStream>(filesToMerge.size());
        for (CWRCStringBuilder builder : filesToMerge.values()) {
        inputFiles.add(builder.getAsStream());
        }
        
        System.out.println("Obtaining merger factory.");
        MergeReport report = new MergeReport("Applet Merge", new AppletConsole(controller.getParameter(PARAM_REPORT_FUNC), controller));
        CWRCMergerFactory.setMaxThreads(totalThreads);
        CWRCMergerFactory merger = CWRCMergerFactory.getFactory(controller, mergeType, report, inputFiles, autoMerge);
        
        inputFiles.clear();
        
        CWRCDataSource mainData = new CWRCJSOutput(controller.getParameter(PARAM_CWRC_URL), mergeType);
        
        for (CWRCStringBuilder builder : filesToMerge.values()) {
        merger.mergeFile(mainData, builder);
        }
        
        filesToMerge.clear();
        String function = controller.getParameter(PARAM_MERGE_COMPLETE);
        
        totalComplete = 0;
        totalEntities = 0;
        
        if (function != null) {
        JSObject jso = JSObject.getWindow(controller);
        JavaScriptUtil.callFunction(jso, function, new Object[]{});
        }
        
        merging = false;
        }
        } catch (Exception ex) {
        ex.printStackTrace();
        }
        }*/
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
}
