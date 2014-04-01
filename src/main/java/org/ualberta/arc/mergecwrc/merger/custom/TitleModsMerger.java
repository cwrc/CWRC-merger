package org.ualberta.arc.mergecwrc.merger.custom;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.MergeReport;
import org.ualberta.arc.mergecwrc.io.CWRCDataSource;
import org.ualberta.arc.mergecwrc.merger.CWRCMerger;
import org.ualberta.arc.mergecwrc.merger.QueryResult;
import org.ualberta.arc.mergecwrc.utils.ScoringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mpm1
 */
public class TitleModsMerger extends CWRCMerger {

    public static final String MAIN_NODE = "modsCollectionDefinition";
    public static final String ENTITY_NODE = "mods";
    private static final float MIN_PERCENT = 0.9f;
    private DocumentBuilder docBuilder = null;
    private Document recordDoc = null;
    //private int checkValue = 0;
    private MergeReport report = null;
    private Comparator<String> comparator = new Comparator<String>(){
        public int compare(String t, String t1) {
            return t.compareTo(t1);
        }
    };

    public TitleModsMerger(MergeReport report) throws CWRCException {
        super(MAIN_NODE, ENTITY_NODE);
        this.report = report;

        if (recordDoc == null) {
            try {
                docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                recordDoc = docBuilder.newDocument();
            } catch (ParserConfigurationException ex) {
                throw new CWRCException(ex);
            }
        }

        this.layerIn = false;
    }

    public void setReport(MergeReport report) {
        this.report = report;
    }

    @Override
    public void init(Collection<InputStream> inputFiles) throws CWRCException {
        //checkValue = 0;
    }

    public void searchName(Element checkChild, Element title, Element inputNode, Map<String, QueryResult> results, CWRCDataSource mainData) {
        String checkChildString = checkChild.getAttribute("type");
        String titleString = title.getAttribute("type");
        int checkChildStrength = 1;
        int titleStrength = 2;

        // Set the strength for the result
        if (checkChildString != null && checkChildString.compareTo("alternative") == 0) {
            checkChildStrength = 0;
        }

        if (titleString != null && titleString.compareTo("alternative") == 0) {
            titleStrength = 1;
        }

        // Get the titles to match
        checkChildString = ((Element) checkChild.getElementsByTagName("title").item(0)).getTextContent();
        titleString = ((Element) title.getElementsByTagName("title").item(0)).getTextContent();

        // Compare the matches
        float difference = ScoringUtil.computeLevenshteinPercent(checkChildString.toLowerCase(), titleString.toLowerCase());

        if (difference > MIN_PERCENT) {
            Element matchedNode = (Element) title.getParentNode();
            if (isMatched((Element) checkChild.getParentNode(), matchedNode)) {
                addResult((Element) checkChild.getParentNode(), checkChildString, results, difference, checkChildStrength + titleStrength);
            }
        }
    }

    private boolean isMatched(Element checkChild, Element matchedNode) {
        NodeList checkList = checkChild.getElementsByTagName("genre");
        NodeList matchedList = matchedNode.getElementsByTagName("genre");
        String text = null;

        // Obtain needed genres to compare
        String checkLevel = null;
        for (int i = 0; i < checkList.getLength(); ++i) {
            Element element = (Element) checkList.item(i);
            String attribute = element.getAttribute("authority");

            if (StringUtils.equals(attribute, "tei:level")) {
                checkLevel = element.getTextContent();
                break;
            }
        }

        // Compare Genres
        if (checkLevel != null) {
            for (int i = 0; i < matchedList.getLength(); ++i) {
                Element element = (Element) matchedList.item(i);
                String attribute = element.getAttribute("authority");
                text = element.getTextContent();

                if (StringUtils.equals(attribute, "tei:level")) {
                    return StringUtils.equals(checkLevel, text);
                }
            }
        }

        return true;
    }

    private void addResult(Element matchedNode, String title, Map<String, QueryResult> results, float score, int multiplier) {
        String id = matchedNode.hashCode() + "";
        QueryResult result = results.get(id);

        if (result == null) {
            result = new QueryResult(id);
            result.setNode(matchedNode);
            result.setName(title);
        }

        result.incrementScore(score * (float) multiplier);
        result.setMatch(true);
        results.put(id, result);
    }

    @Override
    public List<QueryResult> search(CWRCDataSource mainData, Element inputNode) throws CWRCException {
        try {
            Map<String, QueryResult> results = new TreeMap<String, QueryResult>(comparator);

            // Use search on each title. Alternatives are matched based on a property
            List<Element> titles = getChildrenOfName(inputNode, "titleInfo");

            if (titles.isEmpty()) {
                return Collections.EMPTY_LIST;
            }

            //titles = ((Element)titles.item(0)).getElementsByTagName("title");

            NodeList entities = mainData.getAllEntities();

            for (int i = 0; i < entities.getLength(); ++i) {
                Element entity = (Element) entities.item(i);
                List<Element> children = getChildrenOfName(entity, "titleInfo");
                //children = ((Element)children.item(0)).getElementsByTagName("title");

                while (children.size() > 0) {
                    Element checkChild = (Element) children.remove(0);

                    for (int k = 0; k < titles.size(); ++k) {
                        searchName(checkChild, (Element) titles.get(k), inputNode, results, mainData);
                    }
                }
            }

            List output = new ArrayList<QueryResult>(results.size());
            output.addAll(results.values());
            
            results.clear();

            if (!output.isEmpty()) {
                recordAllMatches(inputNode, output);
            }

            return output;
        } catch (NullPointerException ex) {
            System.err.println("Found null pointer exception. Attempting to re-search.");
            ex.printStackTrace();
            return search(mainData, inputNode);
        }
    }

    private void recordAllMatches(Element inputNode, List<QueryResult> matchesResults) {
        Document doc = recordDoc;

        Element possibleMatches = doc.createElement("possibleMatches");

        Element input = doc.createElement("input");
        input.appendChild(doc.adoptNode(inputNode.cloneNode(true)));
        possibleMatches.appendChild(input);

        Element matches = doc.createElement("matches");

        for (QueryResult result : matchesResults) {
            Element match = doc.createElement("match");
            match.setAttribute("score", Float.toString(result.getScore()));
            match.setAttribute("isAcceptableMatch", Boolean.toString(result.isMatch()));

            match.appendChild(doc.adoptNode(result.getNode().cloneNode(true)));

            matches.appendChild(match);
        }

        possibleMatches.appendChild(matches);
        //doc.appendChild(possibleMatches);

        report.printCustomElement(possibleMatches);
    }

    @Override
    public Node performMerge(QueryResult result, Element inputNode) throws CWRCException {
        Element mainElement = (Element) result.getNode();

        mergeGenres(mainElement, inputNode);
        mergeTitleInfo(mainElement, inputNode);
        mergeNames(mainElement, inputNode);
        mergeOriginInfo(mainElement, inputNode);
        mergeRelatedItems(mainElement, inputNode);
        mergeRecordInfo(mainElement, inputNode);

        return mainElement;
    }

    private void mergeGenres(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        NodeList newMatching = newElement.getElementsByTagName("genre");

        for (int index = 0; index < newMatching.getLength(); ++index) {
            Element newGenre = (Element) newMatching.item(index);
            String newAuthority = newGenre.getAttribute("authority");
            String newValue = newGenre.getTextContent();
            boolean found = false;

            NodeList mainMatching = mainElement.getElementsByTagName("genre");

            for (int i = 0; i < mainMatching.getLength(); ++i) {
                Element mainGenre = (Element) mainMatching.item(i);
                String mainAuthority = mainGenre.getAttribute("authority");
                String mainValue = mainGenre.getTextContent();

                if (StringUtils.endsWithIgnoreCase(mainAuthority, newAuthority)
                        && StringUtils.endsWithIgnoreCase(mainValue, newValue)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Element addElement = doc.createElement("genre");
                addElement.setAttribute("authority", newAuthority);
                addElement.setTextContent(newValue);

                mainElement.appendChild(addElement);
            }
        }
    }

    private void mergeTitleInfo(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        NodeList newMatching = getNodeList("titleInfo/title", newElement, null);
        NodeList mainMatching = getNodeList("titleInfo/title", mainElement, null);
        NodeList mainTitleInfo = getNodeList("titleInfo", mainElement, null);
        int count = mainMatching.getLength();
        int lastCount = mainMatching.getLength();

        for (int index = 0; index < newMatching.getLength(); ++index) {
            String newTitle = ((Element) newMatching.item(index)).getTextContent();
            boolean found = false;

            for (int i = 0; i < mainMatching.getLength(); ++i) {
                String mainTitle = ((Element) mainMatching.item(i)).getTextContent();

                if (StringUtils.equals(newTitle, mainTitle)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Element newTitleInfo = doc.createElement("titleInfo");
                newTitleInfo.setAttribute("type", "alternative");

                Element title = doc.createElement("title");
                title.setTextContent(newTitle);
                newTitleInfo.appendChild(title);

                mainElement.appendChild(newTitleInfo);
                mainElement.insertBefore(((Element) mainTitleInfo.item(0)).getNextSibling(), newTitleInfo);
                ++count;
            }
        }

        if (count > 1 && lastCount == 1) {     
            ((Element) mainTitleInfo.item(0)).setAttribute("usage", "primary");
        }
    }

    private void mergeNames(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        NodeList newMatching = getNodeList("namePart/name", newElement, null);

        for (int index = 0; index < newMatching.getLength(); ++index) {
            String newName = ((Element) newMatching.item(index)).getTextContent();
            NodeList mainMatching = getNodeList("namePart/name", mainElement, null);
            boolean found = false;

            for (int i = 0; i < mainMatching.getLength(); ++i) {
                String mainName = ((Element) mainMatching.item(i)).getTextContent();

                if (StringUtils.equals(newName, mainName)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Element newTitleInfo = doc.createElement("name");

                Element name = doc.createElement("namePart");
                name.setTextContent(newName);
                newTitleInfo.appendChild(name);

                mainElement.appendChild(newTitleInfo);
            }
        }
    }

    private void mergeOriginInfo(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        Element mainOriginInfo = null;
        NodeList newMatching = getNodeList("originInfo", newElement, null);

        for (int index = 0; index < newMatching.getLength(); ++index) {
            if (mainOriginInfo == null) {
                mainOriginInfo = checkAndAddElement(mainElement, "originInfo");
            }

            Element newOriginInfo = (Element) newMatching.item(index);
            NodeList children = newOriginInfo.getChildNodes();

            for (int i = 0; i < children.getLength(); ++i) {
                if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element) children.item(i);

                    Node addElement = doc.adoptNode(child.cloneNode(true));
                    mainOriginInfo.appendChild(addElement);
                }
            }
        }
    }

    private void mergeRelatedItems(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        NodeList newMatching = newElement.getElementsByTagName("relatedItem");

        for (int index = 0; index < newMatching.getLength(); ++index) {
            Node relatedItem = doc.adoptNode(newMatching.item(index).cloneNode(true));

            mainElement.appendChild(relatedItem);
        }
    }

    private void mergeRecordInfo(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        Element recordInfo = this.checkAndAddElement(mainElement, "recordInfo");
        NodeList newMatching = newElement.getElementsByTagName("recordInfo");

        for (int index = 0; index < newMatching.getLength(); ++index) {
            Element matching = (Element) newMatching.item(index);
            NodeList children = matching.getChildNodes();

            for (int i = 0; i < children.getLength(); ++i) {
                Node child = doc.adoptNode(children.item(i).cloneNode(true));

                recordInfo.appendChild(child);
            }
        }
    }
}
