package org.ualberta.arc.mergecwrc.merger.custom;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.MergeReport;
import org.ualberta.arc.mergecwrc.io.CWRCDataSource;
import org.ualberta.arc.mergecwrc.merger.CWRCMerger;
import org.ualberta.arc.mergecwrc.merger.QueryResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mpm1
 */
public class TitleMerger extends CWRCMerger {

    private int checkValue = 0;
    private MergeReport report = null;
    private DocumentBuilder docBuilder = null;

    public TitleMerger(MergeReport report) throws CWRCException {
        this.report = report;
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new CWRCException(ex);
        }
    }
    
    public void setReport(MergeReport report) {
        this.report = report;
    }

    @Override
    public void init(Collection<InputStream> inputFiles) throws CWRCException {
        checkValue = 0;
    }

    private void recordAllMatches(Element inputNode, List<QueryResult> matchesResults) {
        Document doc = docBuilder.newDocument();

        Element possibleMatches = doc.createElement("possibleMatches");

        Element input = doc.createElement("input");
        input.appendChild(doc.importNode(inputNode, true));
        possibleMatches.appendChild(input);

        Element matches = doc.createElement("matches");

        for (QueryResult result : matchesResults) {
            Element match = doc.createElement("match");
            match.setAttribute("score", Float.toString(result.getScore()));
            match.setAttribute("isAcceptableMatch", Boolean.toString(result.isMatch()));

            match.appendChild(doc.importNode(result.getNode(), true));

            matches.appendChild(match);
        }

        possibleMatches.appendChild(matches);
        doc.appendChild(possibleMatches);

        report.printCustomElement(possibleMatches);
    }

    @Override
    public List<QueryResult> search(CWRCDataSource mainData, Element inputNode) throws CWRCException {
        try {
            Map<Node, QueryResult> results = new HashMap<Node, QueryResult>();

            NodeList children = inputNode.getElementsByTagName("title");

            if (children.getLength() == 0) {
                return Collections.EMPTY_LIST;
            }

            Element node = (Element) children.item(0);

            NodeList preferredName = getNodeList("identity/preferredForm/namePart", node, null);

            if (preferredName == null || preferredName.getLength() == 0) {
                throw new CWRCException(CWRCException.Error.INVALID_NODE);
            }

            NodeList variantNames = getNodeList("identity/variantForms/variant", node, null);
            NodeList entities = mainData.getAllEntities();

            for (int i = 0; i < entities.getLength(); ++i) {
                Element entity = (Element) entities.item(i);
                children = entity.getElementsByTagName("title");

                if (children.getLength() > 0) {
                    Element title = (Element) children.item(0);

                    // First check if there is a preferred name match.
                    if (preferredName != null) {
                        searchName(title, preferredName, node, results, 1, mainData);
                    }

                    for (int index = 0; index < variantNames.getLength(); ++index) {
                        try {
                            searchName(title, ((Element) variantNames.item(index)).getElementsByTagName("namePart"), node, results, 0, mainData);
                        } catch (CWRCException ex) {
                            if (CWRCException.Error.INVALID_NODE != ex.getError()) {
                                throw ex;
                            }
                        }
                    }
                }
            }

            List output = new ArrayList<QueryResult>(results.size());
            output.addAll(results.values());
            
            if(output.size() > 0){
                recordAllMatches(inputNode, output);
            }
            
            return output;
        } catch (NullPointerException ex) {
            System.err.println("Found null pointer exception. Attempting to re-search.");
            return search(mainData, inputNode);
        }
    }

    private void searchName(Element entity, NodeList namePart, Element inputNode, Map<Node, QueryResult> results, int multiplier, CWRCDataSource mainData) throws CWRCException {
        if (namePart.getLength() == 0) {
            return;
        }

        String name = ((Element) (namePart.item(0))).getTextContent();

        // This avoids the odd blank match.
        if (name == null || StringUtils.isEmpty(name)) {
            throw new CWRCException(CWRCException.Error.INVALID_NODE);
        }
        
        String level = getLevel(inputNode);

        Element identity = getSingleChild("identity", entity);
        boolean checkPreferred = queryPreferredNames(name, identity, level);
        boolean checkVariant = queryVariantNames(name, identity, level);

        int minScore = 1 << multiplier;

        if (checkPreferred) {
            addResult(inputNode, entity, results, minScore << 1, mainData);
        }

        if (checkVariant) {
            addResult(inputNode, entity, results, minScore, mainData);
        }
    }

    private void addResult(Element inputNode, Element matchedNode, Map<Node, QueryResult> results, int score, CWRCDataSource mainData) throws CWRCException, NullPointerException {
        QueryResult result = results.get(matchedNode);

        if (result == null) {
            result = new QueryResult(mainData.getId(matchedNode.getParentNode()));
            result.setNode(matchedNode);


            NodeList preferredName = getNodeList("identity/preferredForm/namePart", matchedNode, null);
            StringBuilder name = new StringBuilder(((Element) preferredName.item(0)).getTextContent());
            for (int index = 1; index < preferredName.getLength(); ++index) {
                name.append(" @ ");
                name.append(((Element) preferredName.item(index)).getTextContent());
            }

            result.setName(name.toString());
        }

        result.incrementScore(score);
        result.setMatch(checkMatch(inputNode, result));
        results.put(matchedNode, result);
    }

    private String getLevelFromIdentity(Element identity) {
        NodeList children = identity.getElementsByTagName("level");

        if (children.getLength() == 0) {
            return null;
        }

        return ((Element) children.item(0)).getTextContent();
    }

    private String getLevel(Element titleNode) {
        NodeList children = titleNode.getElementsByTagName("identity");

        if (children.getLength() == 0) {
            return null;
        }

        Element identity = (Element) children.item(0);

        return getLevelFromIdentity(identity);
    }

    private boolean checkMatch(Element inputNode, QueryResult result) throws CWRCException {
        // Check if the preferred name matches.
        NodeList preferredName = getNodeList("identity/preferredForm/namePart", inputNode, null);
        StringBuilder name = new StringBuilder(((Element) preferredName.item(0)).getTextContent());
        for (int index = 1; index < preferredName.getLength(); ++index) {
            name.append(" @ ");
            name.append(((Element) preferredName.item(index)).getTextContent());
        }

        if (!StringUtils.equalsIgnoreCase(name, result.getName())) {
            return false;
        }

        //Check if they are of the same level.        
        String level1 = getLevel(result.getNode());
        String level2 = getLevel(inputNode);

        if (level1 != null && level2 != null) {
            return StringUtils.equalsIgnoreCase(level1, level2);
        }

        return true;
    }

    private boolean queryVariantNames(String name, Element identity, String level) throws CWRCException {
        Element variantForms = getSingleChild("variantForms", identity);

        if (variantForms == null) {
            return false;
        }

        NodeList variants = variantForms.getElementsByTagName("variant");

        for (int varIndex = 0; varIndex < variants.getLength(); ++varIndex) {
            Element variant = (Element) variants.item(varIndex);
            NodeList nameParts = variant.getElementsByTagName("namePart");

            if (nameParts.getLength() == 1) {
                if (StringUtils.equalsIgnoreCase(name, ((Element) nameParts.item(0)).getTextContent())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean queryPreferredNames(String name, Element identity, String level) throws CWRCException {
        Element preferredForm = getSingleChild("preferredForm", identity);

        if (preferredForm == null) {
            return false;
        }

        NodeList nameParts = preferredForm.getElementsByTagName("namePart");

        if (nameParts.getLength() == 1) {
            if (StringUtils.equalsIgnoreCase(name, ((Element) nameParts.item(0)).getTextContent())) {
                return true;
            } else if (StringUtils.containsIgnoreCase(name, ((Element) nameParts.item(0)).getTextContent())) {
                // Check if there is a partial match with the same level.
                String childLevel = getLevelFromIdentity(identity);
                if (childLevel == null || StringUtils.equalsIgnoreCase(level, childLevel)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Node performMerge(QueryResult result, Element inputNode) throws CWRCException {
        Element mainElement = (Element) result.getNode();
        Element newElement = (Element) inputNode.getElementsByTagName("title").item(0);

        mergeRecordInfo((Element) mainElement.getElementsByTagName("recordInfo").item(0), (Element) newElement.getElementsByTagName("recordInfo").item(0));
        mergeIdentity((Element) mainElement.getElementsByTagName("identity").item(0), (Element) newElement.getElementsByTagName("identity").item(0));

        return mainElement;
    }

    private void mergeRecordInfo(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        String projectId = ((Element) getNode("originInfo/projectId", newElement)).getTextContent();
        Element originInfo = (Element) mainElement.getElementsByTagName("originInfo").item(0);

        NodeList list = getNodeList("originInfo[projectId = '" + projectId + "']", mainElement, null);

        if (list.getLength() == 0) {
            Element newId = doc.createElement("projectId");
            newId.setTextContent(projectId);

            originInfo.appendChild(newId);
        }

        // Merge Record identifiers
        NodeList recordIdentifier = getNodeList("originInfo/recordIdentifier", newElement, null);

        for (int index = 0; index < recordIdentifier.getLength(); ++index) {
            String identifier = ((Element) recordIdentifier.item(index)).getTextContent();
            list = getNodeList("originInfo[recordIdentifier = '" + identifier + "']", mainElement, null);

            if (list.getLength() == 0) {
                Element newIdentifier = doc.createElement("recordIdentifier");
                newIdentifier.setTextContent(identifier);

                originInfo.appendChild(newIdentifier);
            }
        }
    }

    private void mergeIdentity(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        // Merge the varaints
        NodeList variantList = getNodeList("variantForms/variant", newElement, null);

        if (variantList.getLength() > 0) {
            Element variants = checkAndAddElement(mainElement, "variantForms");
            for (int index = 0; index < variantList.getLength(); ++index) {
                if (addVariant(variants, (Element) variantList.item(index))) {
                    Node variant = doc.importNode(variantList.item(index), true);
                    variants.appendChild(variant);
                }
            }
        }
    }

    private boolean addVariant(Element variantForms, Element variant) throws CWRCException {
        NodeList children = variant.getElementsByTagName("namePart");

        String name = ((Element) children.item(0)).getTextContent();

        NodeList variants = variantForms.getElementsByTagName("variant");

        for (int index = 0; index < variants.getLength(); ++index) {
            Element checkVariant = (Element) variants.item(index);
            NodeList nameParts = checkVariant.getElementsByTagName("namePart");

            if (nameParts.getLength() == 1) {
                Element namePart = (Element) nameParts.item(0);
                if (StringUtils.equals(name, namePart.getTextContent())) {
                    return false;
                }
            }
        }

        return true;
    }
}
