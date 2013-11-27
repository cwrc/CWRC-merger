package org.ualberta.arc.mergecwrc.merger;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ualberta.arc.mergecwrc.io.CWRCDataSource;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.MergeReport;
import org.ualberta.arc.mergecwrc.merger.custom.AuthorSimpleDifMerger;
import org.ualberta.arc.mergecwrc.merger.custom.OrganizationMerger;
import org.ualberta.arc.mergecwrc.merger.custom.TitleModsMerger;
import org.ualberta.arc.mergecwrc.ui.MergerController;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The factory used to merge datasources.
 * 
 * @author mpm1
 */
public class CWRCMergerFactory {

    private static int MAX_THREADS = 10;
    private volatile CWRCMerger merger;
    private MergeReport report;
    private boolean autoMerge;

    public static enum MergeType {

        AUTHOR,
        TITLE,
        ORGANIZATION
    }

    public static void setMaxThreads(int threads) {
        if (threads < 0) {
            threads = 1;
        }

        MAX_THREADS = threads;
    }

    public static CWRCMergerFactory getFactory(MergerController controller, MergeType type, MergeReport report, Collection<InputStream> inputFiles, boolean autoMerge) throws CWRCException {

        switch (type) {
            case AUTHOR:
                return new CWRCMergerFactory(controller, new AuthorSimpleDifMerger(), report, inputFiles, autoMerge);

            case TITLE:
                return new CWRCMergerFactory(controller, new TitleModsMerger(report), report, inputFiles, autoMerge);

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
        ReaderThread nodeReader = new ReaderThread(entities, mainSrc, MAX_THREADS);
        nodeReader.start();

        try {
            nodeReader.join();
        } catch (InterruptedException ex) {
            //throw new CWRCException(ex);
        }

        System.out.println("Flushing all elements to the main document.");
        merger.flushNodes(mainSrc);
    }

    private class ReaderThread extends Thread {

        private NodeList entities;
        private CWRCDataSource mainSrc;
        private ThreadPoolExecutor pool;
        private int maxBlockingSize;
        private BlockingQueue<Runnable> blockingQueue;

        public ReaderThread(NodeList entities, CWRCDataSource mainSrc, int maxThreads) {
            this.maxBlockingSize = maxThreads << 3;
           this. blockingQueue = new ArrayBlockingQueue<Runnable>(this.maxBlockingSize);
            this.pool = new ThreadPoolExecutor(maxThreads, maxThreads, 7l, TimeUnit.DAYS, blockingQueue);
        
            this.entities = entities;
            this.mainSrc = mainSrc;
        }

        @Override
        public void run() {
            try {
                for(int index = 0; index < entities.getLength(); ++index){
                    MergeThread thread = new MergeThread(mainSrc, (Element)entities.item(index), autoMerge);
                    
                    while(blockingQueue.size() >= this.maxBlockingSize){
                        //System.err.println("Blocked, waiting " + index);
                    }
                    
                    pool.execute(thread);
                }
                
                pool.shutdown();
                pool.awaitTermination(7, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
                Logger.getLogger(CWRCMergerFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class MergeThread implements Runnable {

        private Element node;
        private CWRCDataSource mainSrc;
        private boolean autoMerge;

        public MergeThread(CWRCDataSource mainSrc, Element node, boolean autoMerge) {
            this.node = node;
            this.mainSrc = mainSrc;
            this.autoMerge = autoMerge;
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
