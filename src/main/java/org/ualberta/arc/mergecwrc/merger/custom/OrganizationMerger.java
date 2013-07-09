package org.ualberta.arc.mergecwrc.merger.custom;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class OrganizationMerger extends CWRCMerger {

    private MergeReport report = null;

    public OrganizationMerger(MergeReport report) throws CWRCException {
        this.report = report;
    }

    @Override
    public void init(Collection<InputStream> inputFiles) throws CWRCException {
    }

    private float queryVariantNames(String name, Element identity) throws CWRCException {
        Element variantForms = getSingleChild("variantForms", identity);

        if (variantForms == null) {
            return 0;
        }

        NodeList variants = variantForms.getElementsByTagName("variant");

        if (variants.getLength() == 0) {
            return 0;
        }

        float total = 0;
        for (int varIndex = 0; varIndex < variants.getLength(); ++varIndex) {
            Element variant = (Element) variants.item(varIndex);
            NodeList nameParts = variant.getElementsByTagName("namePart");

            if (nameParts.getLength() == 1) {
                total += ScoringUtil.computeLevenshteinPercent(name.toLowerCase(), ((Element) nameParts.item(0)).getTextContent().toLowerCase());
            }
        }

        if (total == 0) {
            return 0;
        }

        return total / (float) variants.getLength();
    }

    private float queryPreferredNames(String name, Element identity) throws CWRCException {
        Element preferredForm = getSingleChild("preferredForm", identity);

        if (preferredForm == null) {
            return 0;
        }

        NodeList nameParts = preferredForm.getElementsByTagName("namePart");

        float nameFound = 0;
        if (nameParts.getLength() == 1) {
            nameFound = ScoringUtil.computeLevenshteinPercent(name.toLowerCase(), ((Element) nameParts.item(0)).getTextContent().toLowerCase());
        }

        return nameFound;
    }

    private boolean checkMatch(Element inputNode, QueryResult result) throws CWRCException {
        if (result.getScore() < 0.8) {
            return false;
        }

        //TODO: Add a check for type

        return true;
    }

    private void addResult(Element inputNode, Element matchedNode, Map<Node, QueryResult> results, float percent, int weight, CWRCDataSource mainData) throws CWRCException, NullPointerException {
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
            result.setScore((1.0f + ((float) weight * percent)) / ((float) weight + 1.0f));
        } else {
            result.setScore((result.getScore() + ((float) weight * percent)) / ((float) weight + 1.0f));
        }

        result.setMatch(checkMatch(inputNode, result));
        results.put(matchedNode, result);
    }

    private void searchName(Element entity, NodeList namePart, Element inputNode, Map<Node, QueryResult> results, int multiplier, CWRCDataSource mainData) throws CWRCException {
        if (namePart.getLength() < 1) {
            return;
        }

        String name = ((Element) namePart.item(0)).getTextContent();

        Element identity = getSingleChild("identity", entity);

        float preferredMatch = queryPreferredNames(name, identity);
        float variantMatch = queryVariantNames(name, identity);

        addResult(inputNode, entity, results, (preferredMatch + preferredMatch + variantMatch) / 3.0f, multiplier, mainData);
    }

    @Override
    public List<QueryResult> search(CWRCDataSource mainData, Element inputNode) throws CWRCException {
        try {
            Map<Node, QueryResult> results = new HashMap<Node, QueryResult>();

            NodeList children = inputNode.getElementsByTagName("organization");

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
                children = entity.getElementsByTagName("organization");

                if (children.getLength() > 0) {
                    Element organization = (Element) children.item(0);

                    // First check if there is a preferred name match.
                    if (preferredName != null) {
                        searchName(organization, preferredName, node, results, 1, mainData);
                    }

                    // Secondly check if there is a variant name match.
                    for (int index = 0; index < variantNames.getLength(); ++index) {
                        try {
                            searchName(organization, ((Element) variantNames.item(index)).getElementsByTagName("namePart"), node, results, 0, mainData);
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

            return output;
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            System.err.println("Found null pointer exception. Attempting to re-search.");
            return search(mainData, inputNode);
        }
    }

    private void mergeRecordInfo(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        String projectId = ((Element) getNode("originInfo/projectId", newElement)).getTextContent();
        Element originInfo = (Element) mainElement.getElementsByTagName("originInfo").item(0);

        NodeList list = this.getNodeList("originInfo[projectId = '" + projectId + "']", mainElement, null);

        if (list.getLength() == 0) {
            Element newId = doc.createElement("projectId");
            newId.setTextContent(projectId);

            originInfo.appendChild(newId);
        }
    }

    private boolean addVariant(Element variantForms, Element variant) throws CWRCException {
        NodeList children = variant.getElementsByTagName("namePart");
        String name = null;
        for (int index = 0; index < children.getLength(); ++index) {
            Element child = (Element) children.item(index);
            name = child.getTextContent();
        }

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
    
    private boolean addOrgType(Element orgTypes, Element orgType) throws CWRCException{
        String check = orgType.getTextContent();
        
        NodeList orgs = orgTypes.getElementsByTagName("orgType");
        
        for(int index = 0; index < orgs.getLength(); ++index){
            Element checkOrg = (Element)orgs.item(index);
            
            if(StringUtils.endsWithIgnoreCase(check, checkOrg.getTextContent())){
                return false;
            }
        }
        
        return true;
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
    
    private void mergeDescription(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        //Merge org types
        NodeList domList = getNodeList("orgTypes/orgType", newElement, null);
        
        if(domList.getLength() > 0){
            Element orgTypes = checkAndAddElement(mainElement, "orgTypes");
            
            for(int index = 0; index < domList.getLength(); ++index){
                if(addOrgType(orgTypes, (Element)domList.item(index))){
                    Node orgType = doc.importNode(domList.item(index), true);
                    orgTypes.appendChild(orgType);
                }
            }
        }
    }
    
    private void mergeRelations(Element mainElement, NodeList newRelations) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();
        
        if(newRelations.getLength() > 0){
            Element mainRelations = checkAndAddElement(mainElement, "relaitons");
            
            for(int index = 0; index < newRelations.getLength(); ++index){
                Node relation = doc.importNode(newRelations.item(index), true);
                mainRelations.appendChild(relation);
            }
        }
    }

    @Override
    public synchronized Node performMerge(QueryResult result, Element inputNode) throws CWRCException {
        Element mainElement = (Element) result.getNode();
        Element newElement = (Element) inputNode.getElementsByTagName("organization").item(0);

        mergeRecordInfo((Element) mainElement.getElementsByTagName("recordInfo").item(0), (Element) newElement.getElementsByTagName("recordInfo").item(0));
        mergeIdentity((Element) mainElement.getElementsByTagName("identity").item(0), (Element) newElement.getElementsByTagName("identity").item(0));
        mergeDescription((Element) mainElement.getElementsByTagName("description").item(0), (Element) newElement.getElementsByTagName("description").item(0));
        mergeRelations(mainElement, getNodeList("relations/relation", newElement, null));

        return mainElement;
    }
}
