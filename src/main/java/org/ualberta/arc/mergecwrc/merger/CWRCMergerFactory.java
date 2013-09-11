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

        // Wait for all threads to close
        nodeReader.close();

        System.out.println("Flushing all elements to the main document.");
        merger.flushNodes(mainSrc);
    }

    private class ReaderThread extends Thread {

        private NodeList entities;
        private List<MergeThread> runningThreads;
        private CWRCDataSource mainSrc;
        private int maxThreads;
        private int ittIndex;
        private final Integer indexLock = 0x00000001;

        public ReaderThread(NodeList entities, CWRCDataSource mainSrc, int maxThreads) {
            this.entities = entities;
            this.maxThreads = maxThreads;
            this.runningThreads = new ArrayList<MergeThread>(maxThreads);
            this.mainSrc = mainSrc;
        }

        @Override
        public void run() {
            int firstBatch = Math.min(maxThreads, entities.getLength());

            synchronized (indexLock) {
                ittIndex = 0;
                for (int index = 0; index < firstBatch; ++index) {
                    MergeThread thread = openThread(mainSrc, (Element) entities.item(ittIndex++));
                }
            }

            for (MergeThread thread : runningThreads) {
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                    System.err.println("Thread Closed.");
                }
            }
        }

        public void mergeNext(MergeThread thread) {
            if (ittIndex < entities.getLength()) {
                thread.setNode((Element) entities.item(ittIndex++));
            } else {
                thread.setNode(null);
            }
        }

        private MergeThread openThread(CWRCDataSource mainSrc, Element node) {
            // Wait for an open thread
            MergeThread thread = null;
            while (runningThreads.size() >= MAX_THREADS) {
                Iterator<MergeThread> threads = runningThreads.iterator();

                while (threads.hasNext()) {
                    MergeThread check = threads.next();

                    if (!check.isAlive()) {
                        thread = check;
                        thread.setNode(node);
                        break;
                    }
                }
            }

            if (thread == null) {
                thread = new MergeThread(mainSrc, node, autoMerge, this);
                runningThreads.add(thread);
            }

            thread.start();

            return thread;
        }

        public void close() {
            while (runningThreads.size() > 0) {
                Iterator<MergeThread> threads = runningThreads.iterator();

                while (threads.hasNext()) {
                    MergeThread check = threads.next();

                    if (!check.isAlive()) {
                        threads.remove();
                    }
                }

                if ((runningThreads.size() > 0)) {
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ex) {
                        System.err.println("Waiting for threads to close.");
                        //Logger.getLogger(CWRCMergerFactory.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

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

        public void setNode(Element node) {
            this.node = node;
        }

        @Override
        public void run() {
            try {
                while (this.node != null) {
                    merger.mergeNodes(mainSrc, node, report, autoMerge);
                    reader.mergeNext(this);
                }
            } catch (CWRCException ex) {
                ex.printStackTrace();
            }

            return;
        }
    }
}
