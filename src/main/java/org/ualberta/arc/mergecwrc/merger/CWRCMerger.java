package org.ualberta.arc.mergecwrc.merger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.MergeReport;
import org.ualberta.arc.mergecwrc.io.CWRCDataSource;
import org.ualberta.arc.mergecwrc.io.CWRCDataSource.VariableResolver;
import org.ualberta.arc.mergecwrc.ui.MergerController;
import org.ualberta.arc.mergecwrc.ui.MultipleMatchModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mpm1
 */
public abstract class CWRCMerger {

    private static final int WAIT_TIME = 2500;
    private volatile Document doc;
    private final Integer docLock = 1;
    private volatile MergerController controller;
    //private int completedEntities = 0;
    private String mainNode = "cwrc";
    private String entityNode = "entity";
    protected boolean layerIn = true;

    public String getMainNode() {
        return mainNode;
    }

    public String getEntityNode() {
        return entityNode;
    }
    public static NodeList emptyList = new NodeList() {

        public Node item(int i) {
            return null;
        }

        public int getLength() {
            return 0;
        }
    };

    public CWRCMerger() throws CWRCException {
        this("cwrc", "entity");
    }

    public CWRCMerger(String mainNode, String entityNode) throws CWRCException {
        this.mainNode = mainNode;
        this.entityNode = entityNode;

        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element cwrc = doc.createElement(mainNode);
            doc.appendChild(cwrc);
        } catch (ParserConfigurationException ex) {
            throw new CWRCException(ex);
        }
    }

    public void setController(MergerController controller) {
        this.controller = controller;
    }

    /**
     * Initializes the Merger.
     * @param params Any parameters passed into the merger.
     * @throws CWRCException 
     */
    public abstract void init(Collection<InputStream> inputFiles) throws CWRCException;

    /**
     * Searches for a existing element in the datasource based on a specified node.
     * @param mainData The datasource to search.
     * @param inputNode The node used to search on.
     * @return The list of results matching the search.
     * @throws CWRCException 
     */
    public abstract List<QueryResult> search(CWRCDataSource mainData, Element inputNode) throws CWRCException;

    /**
     * Tries to merge the node with the result.
     * @param result The result to merge.
     * @param inputNode The node to merge with.
     * @return The merged result.
     * @throws CWRCException 
     */
    public abstract Node performMerge(QueryResult result, Element inputNode) throws CWRCException;

    private void cleanUpResults(List<QueryResult> result, float prevBest) {
        if (result.isEmpty()) {
            return;
        }

        Iterator<QueryResult> iterator = result.iterator();
        boolean hasMultiple = false;
        boolean recheckList = false;
        float bestScore = prevBest;

        // Remove any unacceptable matches.
        while (iterator.hasNext()) {
            QueryResult check = iterator.next();
            if (!check.isMatch()) {
                iterator.remove();
                continue;
            }

            if (check.getScore() > bestScore) {
                recheckList = bestScore != prevBest;
                hasMultiple = false;
                bestScore = check.getScore();
            } else if (check.getScore() == bestScore) {
                hasMultiple = true;
            } else {
                iterator.remove();
            }
        }

        if (recheckList) {
            cleanUpResults(result, bestScore);
        }
    }

    /**
     * Runs the merging process on an input node. This is done by performing a search on a node. The results are then checked to see if there are multiple ones.
     * If there is more than one result, then the results are sent to the controller for the user to choose the best match.
     * If there is only one result, then this result is merged with the input node.
     * If there is no match, then the inputNode is appended to the datasource.
     * @param mainData The data source.
     * @param inputNode The node to check.
     * @param report The file that contains any reporting information.
     * @throws CWRCException 
     */
    public void mergeNodes(CWRCDataSource mainData, Element inputNode, MergeReport report, boolean autoMerge) throws CWRCException {
        try {
            List<QueryResult> result;
            //synchronized (mainData) {
            result = search(mainData, inputNode);
            cleanUpResults(result, Integer.MIN_VALUE);

            if (result.size() > (autoMerge ? 1 : 0)) {
                // Merge the best possible match
                //QueryResult bestMatch = null;
                MultipleMatchModel match = new MultipleMatchModel(result.size() + " matches", result, inputNode, layerIn);
                controller.addMerge(match);
                try {
                    while (!match.isSelected()) {
                        Thread.sleep(WAIT_TIME);
                    }

                    QueryResult selectedMatch = match.getSelection();

                    if (selectedMatch == null) {
                        appendNode(inputNode, report);
                    } else {
                        mergeNode(mainData, inputNode, selectedMatch, report);
                    }
                } catch (InterruptedException ex) {
                    throw new CWRCException(ex);
                }
            } else if (autoMerge && result.size() == 1) { // Changed to force selection
                // Check if this is a true match
                if (result.get(0).isMatch()) {
                    mergeNode(mainData, inputNode, result.get(0), report);
                } else {
                    appendNode(inputNode, report);
                }
            } else {
                appendNode(inputNode, report);
            }
            //}
        } catch (CWRCException ex) {
            if (CWRCException.Error.INVALID_NODE == ex.getError()) {
                report.printError(ex, inputNode);
            } else {
                throw ex;
            }
        }
    }

    private void mergeNode(CWRCDataSource mainData, Element node, QueryResult selectedMatch, MergeReport report) throws CWRCException {
        System.out.println("Merging: " + selectedMatch.getName() + "   Score: " + selectedMatch.getScore());
        Node mergeData = performMerge(selectedMatch, node);

        if (mergeData != null) {
            mainData.triggerMerge(mergeData, selectedMatch.getId());
        }

        synchronized (docLock) {
            Element entity = (Element) doc.importNode(node, true); // This is done to avoid any null pointer exceptions when printing DOM objects on multiple threads.
            report.printMerge(entity, selectedMatch);

            controller.incrementCurrentEntities();
        }
    }

    private void appendNode(Element node, MergeReport report) {
        synchronized (docLock) {
            Element entity = (Element) doc.importNode(node, true);
            report.printAppend(entity);
            ((Document) doc).getDocumentElement().appendChild(entity);

            controller.incrementCurrentEntities();
        }
    }

    /**
     * Flushes all current append nodes to the data source.
     * @param outData
     * @throws CWRCException 
     */
    public void flushNodes(CWRCDataSource outData) throws CWRCException {
        NodeList children = doc.getDocumentElement().getElementsByTagName(entityNode);

        for (int index = 0; index < children.getLength(); ++index) {
            outData.appendNode(children.item(index));
        }

        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element cwrc = doc.createElement("cwrc");
            doc.appendChild(cwrc);
        } catch (ParserConfigurationException ex) {
            throw new CWRCException(ex);
        }
    }

    protected final Node getNode(String path, Element inputNode) throws CWRCException {
        try {

            XPathExpression expr = CWRCDataSource.getExpression(path);

            //Node parent = inputNode.getParentNode();
            //parent.removeChild(inputNode);

            Object result = null;
            try {
                synchronized (inputNode.getOwnerDocument()) {
                    result = expr.evaluate(inputNode, XPathConstants.NODE);
                }
            } catch (NullPointerException ex) {
                System.out.println("Rerunning Query: " + path);
                getNode(path, inputNode);
            }

            //parent.appendChild(inputNode);

            return (Node) result;
        } catch (XPathExpressionException ex) {
            throw new CWRCException(CWRCException.Error.QUERY_ERROR, ex);
        }
    }

    protected final NodeList getNodeList(String path, Element inputNode, VariableResolver resolver) throws CWRCException {
        if (inputNode == null) {
            return emptyList;
        }
        
        Object result = null;

        try {
            XPathExpression expr = null;

            boolean searchComplete = false;

            while (!searchComplete) {
                if (resolver != null) {
                    CWRCDataSource.getFactory().setXPathVariableResolver(resolver);
                    XPath xpath = CWRCDataSource.getFactory().newXPath();
                    expr = xpath.compile(path);
                } else {
                    expr = CWRCDataSource.getExpression(path);
                }

                //Node parent = inputNode.getParentNode();
                //parent.removeChild(inputNode);
                
                searchComplete = true;
                try {
                    synchronized (inputNode.getOwnerDocument()) {
                        result = expr.evaluate(inputNode, XPathConstants.NODESET);
                    }
                } catch (NullPointerException ex) {
                    System.out.println("Rerunning Query: " + path);
                    searchComplete = false;
                }
            }

            //parent.appendChild(inputNode);

            return (NodeList) result;
        } catch (XPathExpressionException ex) {
            throw new CWRCException(CWRCException.Error.QUERY_ERROR, ex);
        }
    }

    protected final Element checkAndAddElement(Element parentElement, String tagName) {
        Element out = null;
        NodeList nodeList = parentElement.getElementsByTagName(tagName);

        if (nodeList.getLength() > 0) {
            out = (Element) nodeList.item(0);
        } else {
            out = parentElement.getOwnerDocument().createElement(tagName);
            parentElement.appendChild(out);
        }

        return out;


    }

    protected static class CWRCNodeList implements NodeList {

        private List<Node> nodes = new ArrayList<Node>();

        public Node item(int index) {
            return nodes.get(index);
        }

        public int getLength() {
            return nodes.size();
        }

        public void addNode(Node node) {
            nodes.add(node);
        }
    }

    protected Element getSingleChild(String name, Element element) {
        NodeList nodes = element.getElementsByTagName(name);

        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        }

        return null;
    }

    protected void setTotalEntities(int total) {
        controller.setTotalEntities(total);
    }
}
