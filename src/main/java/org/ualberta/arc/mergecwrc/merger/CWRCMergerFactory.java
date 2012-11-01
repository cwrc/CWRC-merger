package org.ualberta.arc.mergecwrc.merger;

import java.io.InputStream;
import org.ualberta.arc.mergecwrc.io.CWRCDataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.MergeReport;
import org.ualberta.arc.mergecwrc.merger.custom.AuthorSimpleDifMerger;
import org.ualberta.arc.mergecwrc.merger.custom.OrganizationMerger;
import org.ualberta.arc.mergecwrc.merger.custom.TitleMerger;
import org.ualberta.arc.mergecwrc.ui.MergerController;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The factory used to merge datasources.
 * 
 * @author mpm1
 */
public class CWRCMergerFactory {

    private static int MAX_THREADS = 1;
    private volatile CWRCMerger merger;
    private List<MergeThread> runningThreads = new ArrayList<MergeThread>(MAX_THREADS);
    private MergeReport report;
    private boolean autoMerge;

    public static enum MergeType {

        AUTHOR,
        TITLE,
        ORGANIZATION
    }
    
    public static void setMaxThreads(int threads){
        if(threads < 0){
            threads = 1;
        }
        
        MAX_THREADS = threads;
    }

    public static CWRCMergerFactory getFactory(MergerController controller, MergeType type, MergeReport report, Collection<InputStream> inputFiles, boolean autoMerge) throws CWRCException {
        
        switch (type) {
            case AUTHOR:
                return new CWRCMergerFactory(controller, new AuthorSimpleDifMerger(), report, inputFiles, autoMerge);

            case TITLE:
                return new CWRCMergerFactory(controller, new TitleMerger(report), report, inputFiles, autoMerge);

            case ORGANIZATION:
                return new CWRCMergerFactory(controller, new OrganizationMerger(report), report, inputFiles, autoMerge);
        }

        return null;
    }

    private CWRCMergerFactory(MergerController controller, CWRCMerger merger, MergeReport report, Collection<InputStream> inputFiles, boolean autoMerge) throws CWRCException {
        this.merger = merger;
        merger.setController(controller);
        this.report = report;
        this.autoMerge = autoMerge;

        this.merger.init(inputFiles);
    }

    public CWRCMerger getMerger() {
        return merger;
    }

    /**
     * Merge two datasources.
     * @param mainSrc The data source to merge into.
     * @param data The data to merge into the main source.
     * @throws CWRCException 
     */
    public void mergeFile(CWRCDataSource mainSrc, CWRCDataSource data) throws CWRCException {
        NodeList entities = data.getAllEntities(); // Obtains all entity elements in the document;

        System.out.println("Reading Nodes.");
        for (int index = 0; index < entities.getLength(); ++index) {
            MergeThread thread = openThread(mainSrc, (Element) entities.item(index));
        }

        // Wait for all threads to close
        System.out.println("Closing threads.");
        while (runningThreads.size() > 0) {
            Iterator<MergeThread> threads = runningThreads.iterator();

            while (threads.hasNext()) {
                MergeThread check = threads.next();

                if (!check.isAlive()) {
                    threads.remove();
                }
            }
        }

        System.out.println("Flushing all elements to the main document.");
        merger.flushNodes(mainSrc);
    }

    private MergeThread openThread(CWRCDataSource mainSrc, Element node) {
        // Wait for an open thread

        while (runningThreads.size() >= MAX_THREADS) {
            Iterator<MergeThread> threads = runningThreads.iterator();

            while (threads.hasNext()) {
                MergeThread check = threads.next();

                if (!check.isAlive()) {
                    threads.remove();
                }
            }
        }

        MergeThread thread = new MergeThread(mainSrc, node, autoMerge);
        runningThreads.add(thread);
        thread.start();

        return thread;
    }
    
    private class MergeThread extends Thread {

        private Element node;
        private CWRCDataSource mainSrc;
        private boolean autoMerge;

        public MergeThread(CWRCDataSource mainSrc, Element node, boolean autoMerge) {
            this.node = node;
            this.mainSrc = mainSrc;
            this.autoMerge = autoMerge;
        }

        public void setNode(Element node) {
            this.node = node;
        }

        @Override
        public void run() {
            try {
                merger.mergeNodes(mainSrc, node, report, autoMerge);
            } catch (CWRCException ex) {
                ex.printStackTrace();
            }

            return;
        }
    }
}
