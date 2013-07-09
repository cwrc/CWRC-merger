package org.ualberta.arc.mergecwrc.merger.custom;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.io.CWRCDataSource;
import org.ualberta.arc.mergecwrc.merger.CWRCMerger;
import org.ualberta.arc.mergecwrc.merger.QueryResult;
import org.ualberta.arc.mergecwrc.xslt.XSLTUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A merger used to merge Author entries.
 * 
 * @author mpm1
 */
public class AuthorMerger extends CWRCMerger {

    private Map<String, Integer> variantMap = new HashMap<String, Integer>();
    private int totalEntities = 0;
    private int checkValue = 0; // The value to compare the score against

    public AuthorMerger() throws CWRCException {
    }

    @Override
    public void init(Collection<InputStream> inputFiles) throws CWRCException {
        for (InputStream file : inputFiles) {
            InputStream xslt = this.getClass().getClassLoader().getResourceAsStream("org/ualberta/arc/mergecwrc/xslt/ExtractAuthorVariants.xslt");
            String variantFile = XSLTUtil.transformStream(xslt, file);
            readVaraintFile(variantFile);

            try {
                xslt.close();
            } catch (IOException ex) {
                throw new CWRCException(ex);
            }
        }

        // Update the scores list
        Set<String> keys = variantMap.keySet();
        for (String key : keys) {
            int score = totalEntities / (variantMap.get(key) * variantMap.get(key));
            variantMap.put(key, score);
        }

        checkValue = (totalEntities << 2);
        setTotalEntities(totalEntities);
        System.out.println("Done reading variant names. " + totalEntities + " entities found.");
        System.out.println("Score must be greater than " + checkValue);
    }

    private void readVaraintFile(String varaintFile) throws CWRCException {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(varaintFile));

            String variant = variant = reader.readLine();

            if (variant != null) {
                totalEntities += Integer.parseInt(variant);
            }

            while ((variant = reader.readLine()) != null) {
                addVariant(variant.trim());
            }
        } catch (FileNotFoundException ex) {
            throw new CWRCException(CWRCException.Error.VARAINT_READ, ex);
        } catch (IOException ex) {
            throw new CWRCException(CWRCException.Error.VARAINT_READ, ex);
        }
    }

    private void addVariant(String variant) {
        Integer score = variantMap.get(variant);

        if (score == null) {
            score = new Integer(1);
        } else {
            score = score.intValue() + 1;
        }

        variantMap.put(variant, score);
    }

    private int getVaraintScore(String surname, String forename) {
        StringBuilder builder = new StringBuilder(surname);
        builder.append(" @ ");
        builder.append(forename);
        return getVaraintScore(builder.toString());
    }

    private int getVaraintScore(String variant) {
        Integer score = variantMap.get(variant.trim());

        if (score != null) {
            return score.intValue();
        }

        return totalEntities;
    }

    @Override
    public synchronized Node performMerge(QueryResult result, Element inputNode) throws CWRCException {
        Element mainElement = (Element) result.getNode();
        Element newElement = (Element) inputNode.getElementsByTagName("person").item(0);

        mergeRecordInfo((Element) mainElement.getElementsByTagName("recordInfo").item(0), (Element) newElement.getElementsByTagName("recordInfo").item(0));
        mergeIdentity((Element) mainElement.getElementsByTagName("identity").item(0), (Element) newElement.getElementsByTagName("identity").item(0));
        mergeDescription((Element) mainElement.getElementsByTagName("description").item(0), (Element) newElement.getElementsByTagName("description").item(0));

        return mainElement;
    }

    // Check if this is a canwwr project id that needs to be added. Also it removes any uneeded canwwr ids if no longer needed.
    private boolean addCanwwr(String projectId, Element mainElement) throws CWRCException {
        if (StringUtils.equals(projectId, "canwwr")) {
            NodeList list = this.getNodeList("originInfo[projectId = 'ceww']", mainElement, null);

            return list.getLength() == 0; // This means that we do not add the project id since a ceww one already exists.
        } else if (StringUtils.equals(projectId, "ceww")) {
            NodeList list = this.getNodeList("originInfo[projectId = 'canwwr']", mainElement, null);

            for (int index = 0; index < list.getLength(); ++index) {
                mainElement.removeChild(list.item(index)); // Remove any existing canwwr elements
            }
        }

        return true;
    }

    private void mergeRecordInfo(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        String projectId = ((Element) getNode("originInfo/projectId", newElement)).getTextContent();
        Element originInfo = (Element) mainElement.getElementsByTagName("originInfo").item(0);

        NodeList list = this.getNodeList("originInfo[projectId = '" + projectId + "']", mainElement, null);

        if (list.getLength() == 0) {
            if (addCanwwr(projectId, mainElement)) {
                Element newId = doc.createElement("projectId");
                newId.setTextContent(projectId);

                originInfo.appendChild(newId);
            }
        }

        //Merge personType
        NodeList personTypeList = getNodeList("personTypes/personType", newElement, null);

        if (personTypeList.getLength() > 0) {
            Element personTypes = checkAndAddElement(mainElement, "personTypes");

            for (int index = 0; index < personTypeList.getLength(); ++index) {
                if (!checkIfTextNodeMatch(personTypes.getElementsByTagName("personType"),
                        personTypeList.item(index).getTextContent())) {
                    Node node = doc.importNode(personTypeList.item(index), true);
                    personTypes.appendChild(node);
                }
            }
        }
    }

    private static boolean checkIfTextNodeMatch(NodeList list, String text) {
        for (int index = 0; index < list.getLength(); ++index) {
            if (StringUtils.equals(list.item(index).getTextContent(), text)) {
                return true;
            }
        }

        return false;
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

        String surname = null;
        String forename = null;
        for (int index = 0; index < children.getLength(); ++index) {
            Element child = (Element) children.item(index);

            if (StringUtils.equals(child.getAttribute("partType"), "surname")) {
                surname = child.getTextContent();
            } else {
                forename = child.getTextContent();
            }
        }

        NodeList variants = variantForms.getElementsByTagName("variant");

        if (surname != null) {
            for (int index = 0; index < variants.getLength(); ++index) {
                boolean checkSur = false;
                boolean checkFore = false;
                Element checkVariant = (Element) variants.item(index);
                NodeList nameParts = checkVariant.getElementsByTagName("namePart");

                if (nameParts.getLength() == 2) {
                    for (int nameIndex = 0; nameIndex < 2; ++nameIndex) {
                        Element namePart = (Element) nameParts.item(nameIndex);
                        String partType = namePart.getAttribute("partType");

                        if (StringUtils.equals("surname", partType)) {
                            checkSur = StringUtils.equals(surname, namePart.getTextContent());
                        } else {
                            checkFore = StringUtils.equals(forename, namePart.getTextContent());
                        }
                    }
                }

                if (checkSur && checkFore) {
                    return false;
                }
            }
        } else {
            for (int index = 0; index < variants.getLength(); ++index) {
                Element checkVariant = (Element) variants.item(index);
                NodeList nameParts = checkVariant.getElementsByTagName("namePart");

                if (nameParts.getLength() == 1) {
                    Element namePart = (Element) nameParts.item(0);
                    if (StringUtils.equals(forename, namePart.getTextContent())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private void mergeDescription(Element mainElement, Element newElement) throws CWRCException {
        Document doc = mainElement.getOwnerDocument();

        //Merge notes first
        NodeList domList = getNodeList("descriptiveNotes/note", newElement, null);

        if (domList.getLength() > 0) {
            Element descriptiveNotes = checkAndAddElement(mainElement, "descriptiveNotes");

            for (int index = 0; index < domList.getLength(); ++index) {
                Node note = doc.importNode(domList.item(index), true);
                descriptiveNotes.appendChild(note);
            }
        }

        //Merge Exist Dates
        domList = getNodeList("existDates/*", newElement, null);

        if (domList.getLength() > 0) {
            Element existDates = checkAndAddElement(mainElement, "existDates");

            for (int index = 0; index < domList.getLength(); ++index) {
                Node node = doc.importNode(domList.item(index), true);
                existDates.appendChild(node);
            }
        }

        // Merge occupations
        domList = getNodeList("occupations/*", newElement, null);

        if (domList.getLength() > 0) {
            Element occupations = checkAndAddElement(mainElement, "occupations");

            for (int index = 0; index < domList.getLength(); ++index) {
                Node node = doc.importNode(domList.item(index), true);
                occupations.appendChild(node);
            }
        }
    }
    
    @Override
    public List<QueryResult> search(CWRCDataSource mainData, Element inputNode) throws CWRCException {
        return search(mainData, inputNode, 10);
    }

    public List<QueryResult> search(CWRCDataSource mainData, Element inputNode, int searchLeft) throws CWRCException {
        try {
            Map<Node, QueryResult> results = new HashMap<Node, QueryResult>();

            NodeList children = inputNode.getElementsByTagName("person");

            if (children.getLength() == 0) {
                return Collections.EMPTY_LIST;
            }

            Element node = (Element) children.item(0);

            NodeList preferredName = getNodeList("identity/preferredForm/namePart", node, null);
            
            if(preferredName == null || preferredName.getLength() == 0){
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
                        try{
                            searchName(person, ((Element) variantNames.item(index)).getElementsByTagName("namePart"), node, results, 0, mainData);
                        }catch(CWRCException ex){
                            if(CWRCException.Error.INVALID_NODE != ex.getError()){
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
            if(searchLeft > 0){
                return search(mainData, inputNode, searchLeft - 1);
            }
            
            return Collections.EMPTY_LIST;
        }
    }

    private void searchName(Element entity, NodeList namePart, Element inputNode, Map<Node, QueryResult> results, int multiplier, CWRCDataSource mainData) throws CWRCException {
        String surname = null;
        String forename = null;
        int minScore = 0;

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

        boolean checkPreferred = false;
        boolean checkVariant = false;
        boolean checkJoined = false;
        if (surname != null) {
            minScore = getVaraintScore(surname, forename);
            checkPreferred = queryPreferredNames(surname, forename, identity);
            checkVariant = queryVariantNames(surname, forename, identity);
            checkJoined = queryPreferredNames(forename + " " + surname, identity);
        } else {
            minScore = getVaraintScore(forename);
            checkPreferred = queryPreferredNames(forename, identity);
            checkVariant = queryVariantNames(forename, identity);
        }
        minScore = minScore << multiplier;

        if (checkPreferred) {
            addResult(inputNode, entity, results, minScore << 2, mainData);
        }

        if (checkVariant) {
            addResult(inputNode, entity, results, minScore, mainData);
        }

        if (checkJoined) {
            addResult(inputNode, entity, results, minScore << 1, mainData);
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

    private boolean checkMatch(Element inputNode, QueryResult result) throws CWRCException {
        Map<String, List<Integer>> inputRange = getCompareDates(inputNode);
        Map<String, List<Integer>> resultRange = getCompareDates((Element) result.getNode());

        if (compareDates(inputRange.get("birth"), resultRange.get("birth"))) {
            if (!compareDates(inputRange.get("death"), resultRange.get("death"))) {
                return false;
            }
        } else {
            return false;
        }

        /*NodeList dateRange = getNodeList("description/existsDates/dateRange/*", inputNode, null);
        
        if (dateRange.getLength() == 0) {
        NodeList dateRange2 = getNodeList("description/existsDates/dateRange/*", (Element) result.getNode(), null);
        
        if (dateRange2.getLength() != 0) {
        result.incrementScore(-1);
        }
        }*/

        return result.getScore() >= this.checkValue; // This is so any minor variant matches do not count as a consice match.
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

    private boolean queryVariantNames(String name, Element identity) throws CWRCException {
        //VariableResolver resolver = new VariableResolver();
        //resolver.setVariable("authorName", name);
        //return mainFile.executeQuery("//entity[identity/variantForms/variant/namePart = $authorName]", resolver);

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

    private boolean queryVariantNames(String surname, String forename, Element identity) throws CWRCException {
        //VariableResolver resolver = new VariableResolver();
        //resolver.setVariable("surname", surname);
        //resolver.setVariable("forename", forename);
        //return mainFile.executeQuery("//entity[identity/variantForms/variant/namePart[@partType = 'surname'] = $surname and identity/variantForms/variant/namePart[@partType = 'forename'] = $forename]", resolver);

        Element variantForms = getSingleChild("variantForms", identity);

        if (variantForms == null) {
            return false;
        }

        NodeList variants = variantForms.getElementsByTagName("variant");

        for (int varIndex = 0; varIndex < variants.getLength(); ++varIndex) {
            boolean surFound = false;
            boolean foreFound = false;

            Element variant = (Element) variants.item(varIndex);
            NodeList nameParts = variant.getElementsByTagName("namePart");

            if (nameParts.getLength() > 1) {
                for (int nameIndex = 0; nameIndex < nameParts.getLength(); ++nameIndex) {
                    Element namePart = (Element) nameParts.item(nameIndex);
                    String partType = namePart.getAttribute("partType");

                    if (StringUtils.equals("surname", partType)) {
                        surFound = StringUtils.equalsIgnoreCase(surname, namePart.getTextContent());
                    } else {
                        foreFound = StringUtils.equalsIgnoreCase(forename, namePart.getTextContent());
                    }
                }
            }

            if (surFound && foreFound) {
                return true;
            }
        }

        return false;
    }

    private boolean queryPreferredNames(String name, Element identity) throws CWRCException {
        //VariableResolver resolver = new VariableResolver();
        //resolver.setVariable("authorName", name);
        //return mainFile.executeQuery("//entity[identity/preferredForm/namePart = $authorName]", resolver);

        Element preferredForm = getSingleChild("preferredForm", identity);

        if (preferredForm == null) {
            return false;
        }

        NodeList nameParts = preferredForm.getElementsByTagName("namePart");

        if (nameParts.getLength() == 1) {
            if (StringUtils.equalsIgnoreCase(name, ((Element) nameParts.item(0)).getTextContent())) {
                return true;
            }
        }

        return false;
    }

    private boolean queryPreferredNames(String surname, String forename, Element identity) throws CWRCException {
        //VariableResolver resolver = new VariableResolver();
        //resolver.setVariable("surname", surname);
        //resolver.setVariable("forename", forename);
        //return mainFile.executeQuery("//entity[identity/preferredForm/namePart[@partType = 'surname'] = $surname and identity/preferredForm/namePart[@partType = 'forename'] = $forename]", resolver);

        Element preferredForm = getSingleChild("preferredForm", identity);

        if (preferredForm == null) {
            return false;
        }

        NodeList nameParts = preferredForm.getElementsByTagName("namePart");

        boolean surFound = false;
        boolean foreFound = false;
        if (nameParts.getLength() > 1) {
            for (int nameIndex = 0; nameIndex < nameParts.getLength(); ++nameIndex) {
                Element namePart = (Element) nameParts.item(nameIndex);
                String partType = namePart.getAttribute("partType");

                if (StringUtils.equals("surname", partType)) {
                    surFound = StringUtils.equalsIgnoreCase(surname, namePart.getTextContent());
                } else {
                    foreFound = StringUtils.equalsIgnoreCase(forename, namePart.getTextContent());
                }
            }
        }

        if (surFound && foreFound) {
            return true;
        }

        return false;
    }
}
