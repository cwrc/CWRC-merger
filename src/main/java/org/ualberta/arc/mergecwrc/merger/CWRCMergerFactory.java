package org.ualberta.arc.mergecwrc.merger;

import java.io.InputStream;
import java.util.ArrayList;
import org.ualberta.arc.mergecwrc.io.CWRCDataSource;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        AUTHOR("person"),
        TITLE("title"),
        ORGANIZATION("organization");
        
        private String shortType;
        
        MergeType(String val){
            shortType = val;
        }
        
        public String getShortType(){
            return shortType;
        }
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
        
        System.gc();
    }

    private class ReaderThread extends Thread {
        private NodeList entities;
        private CWRCDataSource mainSrc;
        private int maxThreads;
        private List<Thread> threads;
        
        public ReaderThread(NodeList entities, CWRCDataSource mainSrc, int maxThreads) {
            this.entities = entities;
            this.mainSrc = mainSrc;
            this.maxThreads = maxThreads;
            
            threads = new ArrayList<Thread>(maxThreads);
        }
        
        public void threadComplete(Thread thread){
            this.threads.remove(thread);
        }

        @Override
        public void run() {
            try {
                for (int index = 0; index < entities.getLength(); ++index) {
                    MergeThread thread = new MergeThread(mainSrc, (Element) entities.item(index), autoMerge, this);

                    while(threads.size() >= maxThreads){
                        Thread.sleep(100l);
                    }
                    
                    threads.add(thread);
                    thread.start();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(CWRCMergerFactory.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                threads.clear();
                this.threads = null;
                this.entities = null;
                this.mainSrc = null;
            }
        }
    }
    /*private class ReaderThread extends Thread {

        private NodeList entities;
        private CWRCDataSource mainSrc;
        private ThreadPoolExecutor pool;
        private int maxBlockingSize;
        private BlockingQueue<Runnable> blockingQueue;
        private int index = 0;
        
        public ReaderThread(NodeList entities, CWRCDataSource mainSrc, int maxThreads) {
            this.maxBlockingSize = maxThreads;
            this.blockingQueue = new ArrayBlockingQueue<Runnable>(this.maxBlockingSize);
            this.pool = new ThreadPoolExecutor(1, maxThreads, 5l, TimeUnit.MINUTES, blockingQueue);
            this.pool.prestartCoreThread();
            

            this.entities = entities;
            this.mainSrc = mainSrc;
        }

        @Override
        public void run() {
            try {
                for (index = 0; index < entities.getLength(); ++index) {
                    MergeThread thread = new MergeThread(mainSrc, (Element) entities.item(index), autoMerge);
                    
                    while(pool.getQueue().remainingCapacity() < 1){
                        Thread.sleep(100l);
                    }
                    
                    pool.execute(thread);
                }

                pool.shutdown();
                pool.awaitTermination(7, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
                Logger.getLogger(CWRCMergerFactory.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                this.blockingQueue.clear();
                
                this.blockingQueue = null;
                this.pool = null;
                this.entities = null;
                this.mainSrc = null;
            }
        }
    }*/

    private class MergeThread extends Thread {

        private Element node;
        private CWRCDataSource mainSrc;
        private boolean autoMerge;
        private ReaderThread reader;

        public MergeThread(CWRCDataSource mainSrc, Element node, boolean autoMerge, ReaderThread reader) {
            this.node = node;
            this.mainSrc = mainSrc;
            this.autoMerge = autoMerge;
            this.reader = reader;
        }

        @Override
        public void run() {
            try {
                merger.mergeNodes(mainSrc, node, report, autoMerge);
            } catch (CWRCException ex) {
                ex.printStackTrace();
            } finally {
                reader.threadComplete(this);
                this.node = null;
                this.mainSrc = null;
            }

            return;
        }
    }
}
