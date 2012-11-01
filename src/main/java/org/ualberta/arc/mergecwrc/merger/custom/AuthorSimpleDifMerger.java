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
import org.ualberta.arc.mergecwrc.io.CWRCDataSource;
import org.ualberta.arc.mergecwrc.merger.CWRCMerger;
import org.ualberta.arc.mergecwrc.merger.QueryResult;
import org.ualberta.arc.mergecwrc.utils.ScoringUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mpm1
 */
public class AuthorSimpleDifMerger extends CWRCMerger {

    public AuthorSimpleDifMerger() throws CWRCException {
        super();
    }

    @Override
    public void init(Collection<InputStream> inputFiles) throws CWRCException {
        System.out.println("Currently we are not performing any initialization functions.");
    }

    private boolean compareDates(List<Integer> date1, List<Integer> date2) {
        if (date1 == null || date2 == null) {
            return true;
        }

        List<Integer> main = null;
        List<Integer> compare = null;
        if (date1.size() > date2.size()) {
            compare = date1;
            main = date2;
        } else {
            compare = date2;
            main = date1;
        }

        for (int index = 0; index < main.size(); ++index) {
            if (main.get(index).intValue() != compare.get(index).intValue()) {
                return false;
            }
        }

        return true;
    }

    private List<Integer> extractDate(String dateString) {
        String dates[] = dateString.split("-");
        List<Integer> outList = new ArrayList<Integer>(3);

        for (String date : dates) {
            date = date.replace('?', ' ').trim();
            if (StringUtils.isNotBlank(date)) {
                outList.add(Integer.parseInt(date));
            }
        }

        return outList;
    }

    private Map<String, List<Integer>> getCompareDates(Element entity) {
        Map<String, List<Integer>> outMap = new HashMap<String, List<Integer>>();

        // First get the needed date range element.        
        Element description = getSingleChild("description", entity);
        if (description == null) {
            return outMap;
        }

        Element existDates = getSingleChild("existDates", description);
        if (existDates == null) {
            return outMap;
        }

        Element dateRange = getSingleChild("dateRange", existDates);
        if (dateRange == null) {
            return outMap;
        }

        // Obtain the birth date
        NodeList nodes = dateRange.getElementsByTagName("fromDate");
        for (int index = 0; index < nodes.getLength(); ++index) {
            Element dateElement = (Element) nodes.item(index);

            Element dateTypeElement = getSingleChild("dateType", dateElement);
            if (dateTypeElement == null) {
                continue;
            }

            if (StringUtils.equals("birth", dateTypeElement.getTextContent())) {
                Element standardDate = getSingleChild("standardDate", dateElement);
                if (standardDate == null) {
                    continue;
                }

                outMap.put("birth", extractDate(standardDate.getTextContent()));
                break;
            }
        }

        // Obtain the death date
        nodes = dateRange.getElementsByTagName("toDate");
        for (int index = 0; index < nodes.getLength(); ++index) {
            Element dateElement = (Element) nodes.item(index);

            Element dateTypeElement = getSingleChild("dateType", dateElement);
            if (dateTypeElement == null) {
                continue;
            }

            if (StringUtils.equals("death", dateTypeElement.getTextContent())) {
                Element standardDate = getSingleChild("standardDate", dateElement);
                if (standardDate == null) {
                    continue;
                }

                outMap.put("death", extractDate(standardDate.getTextContent()));
                break;
            }
        }

        return outMap;
    }

    private boolean checkMatch(Element inputNode, QueryResult result) throws CWRCException {
        if (result.getScore() < 0.8) {
            return false;
        }

        Map<String, List<Integer>> inputRange = getCompareDates(inputNode);
        Map<String, List<Integer>> resultRange = getCompareDates((Element) result.getNode());

        if (compareDates(inputRange.get("birth"), resultRange.get("birth"))) {
            if (!compareDates(inputRange.get("death"), resultRange.get("death"))) {
                return false;
            }
        } else {
            return false;
        }

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
        String surname = null;
        String forename = null;

        for (int index = 0; index < namePart.getLength(); ++index) {
            Element node = (Element) namePart.item(index);
            String attr = node.getAttribute("partType");

            if (StringUtils.equalsIgnoreCase(attr, "surname")) {
                surname = node.getTextContent();
            } else {
                forename = node.getTextContent();
            }
        }

        // This avoids the odd blank match.
        if (forename == null || StringUtils.isEmpty(forename)) {
            throw new CWRCException(CWRCException.Error.INVALID_NODE);
        }

        Element identity = getSingleChild("identity", entity);

        float preferredMatch = 0.0f;
        float variantMatch = 0.0f;
        float joinMatch = 0.0f;
        if (surname != null) {
            preferredMatch = queryPreferredNames(surname, forename, identity);
            variantMatch = queryVariantNames(surname, forename, identity);
            joinMatch = queryPreferredNames(forename + " " + surname, identity);
        } else {
            preferredMatch = queryPreferredNames(forename, identity);
            variantMatch = queryVariantNames(forename, identity);
        }

        if(joinMatch > 0.0) {
            addResult(inputNode, entity, results, joinMatch, 2 << multiplier, mainData);
        }else if (preferredMatch > 0.0) {
            addResult(inputNode, entity, results, preferredMatch, 2 << multiplier, mainData);
        }
        
        addResult(inputNode, entity, results, variantMatch, 1 << multiplier, mainData);
    }

    private float queryVariantNames(String name, Element identity) throws CWRCException {
        Element variantForms = getSingleChild("variantForms", identity);

        if (variantForms == null) {
            return 0;
        }

        NodeList variants = variantForms.getElementsByTagName("variant");
        
        if(variants.getLength() == 0){
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

        if(total == 0){
            return 0;
        }
        
        return total / (float)variants.getLength();
    }

    private float queryVariantNames(String surname, String forename, Element identity) throws CWRCException {
        Element variantForms = getSingleChild("variantForms", identity);

        if (variantForms == null) {
            return 0;
        }

        NodeList variants = variantForms.getElementsByTagName("namePart");
        
        if(variants.getLength() == 0){
            return 0;
        }

        float total = 0;
        for (int varIndex = 0; varIndex < variants.getLength(); ++varIndex) {
            float surFound = 0;
            float foreFound = 0;

            Element variant = (Element) variants.item(varIndex);
            NodeList nameParts = variant.getElementsByTagName("namePart");

            if (nameParts.getLength() > 1) {
                for (int nameIndex = 0; nameIndex < nameParts.getLength(); ++nameIndex) {
                    Element namePart = (Element) nameParts.item(nameIndex);
                    String partType = namePart.getAttribute("partType");

                    if (StringUtils.equals("surname", partType)) {
                        surFound = ScoringUtil.computeLevenshteinPercent(surname.toLowerCase(), namePart.getTextContent().toLowerCase());
                    } else {
                        foreFound = ScoringUtil.computeLevenshteinPercent(forename.toLowerCase(), namePart.getTextContent().toLowerCase());
                    }
                }
            }

            total += (surFound + foreFound)/2.0f;
        }

        return total/(float)variants.getLength();
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

    private float queryPreferredNames(String surname, String forename, Element identity) throws CWRCException {
        Element preferredForm = getSingleChild("preferredForm", identity);

        if (preferredForm == null) {
            return 0;
        }

        NodeList nameParts = preferredForm.getElementsByTagName("namePart");

        float surFound = 0;
        float foreFound = 0;
        if (nameParts.getLength() > 1) {
            for (int nameIndex = 0; nameIndex < nameParts.getLength(); ++nameIndex) {
                Element namePart = (Element) nameParts.item(nameIndex);
                String partType = namePart.getAttribute("partType");

                if (StringUtils.equals("surname", partType)) {
                    surFound = ScoringUtil.computeLevenshteinPercent(surname.toLowerCase(), namePart.getTextContent().toLowerCase());
                } else {
                    foreFound = ScoringUtil.computeLevenshteinPercent(forename.toLowerCase(), namePart.getTextContent().toLowerCase());
                }
            }
        }

        return (surFound + foreFound) / 2.0f;
    }

    @Override
    public List<QueryResult> search(CWRCDataSource mainData, Element inputNode) throws CWRCException {
        try {
            Map<Node, QueryResult> results = new HashMap<Node, QueryResult>();

            NodeList children = inputNode.getElementsByTagName("person");

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
                children = entity.getElementsByTagName("person");

                if (children.getLength() > 0) {
                    Element person = (Element) children.item(0);

                    // First check if there is a preferred name match.
                    if (preferredName != null) {
                        searchName(person, preferredName, node, results, 1, mainData);
                    }

                    // Secondly check if there is a variant name match.
                    for (int index = 0; index < variantNames.getLength(); ++index) {
                        try {
                            searchName(person, ((Element) variantNames.item(index)).getElementsByTagName("namePart"), node, results, 0, mainData);
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
            System.err.println("Found null pointer exception. Attempting to re-search.");
            return search(mainData, inputNode);
        }
    }

    @Override
    public Node performMerge(QueryResult result, Element inputNode) throws CWRCException {
        System.err.println("Merge not yet implemented.");
        
        return null;
    }
}
