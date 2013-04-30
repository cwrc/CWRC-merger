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
public class TitleModsMerger extends CWRCMerger{
    public static final String MAIN_NODE = "modsCollectionDefinition";
    public static final String ENTITY_NODE = "mods";
    private static final float MIN_PERCENT = 0.85f;
    
    private int checkValue = 0;
    private MergeReport report = null;
    private DocumentBuilder docBuilder = null;
    
    public TitleModsMerger(MergeReport report) throws CWRCException{
        super(MAIN_NODE, ENTITY_NODE);
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
    
    public void searchName(Element checkChild, Element title, Element inputNode, Map<String, QueryResult> results, CWRCDataSource mainData){
        String checkChildString = checkChild.getAttribute("type");
        String titleString = title.getAttribute("type");
        int checkChildStrength = 1;
        int titleStrength = 2;
        
        // Set the strength for the result
        if(checkChildString != null && checkChildString.compareTo("alternative") == 0){
            checkChildStrength = 0;
        }
        
        if(titleString != null && titleString.compareTo("alternative") == 0){
            titleStrength = 1;
        }
        
        // Get the titles to match
        NodeList titles = checkChild.getElementsByTagName("title");
        if(titles.getLength() == 0){
            return;
        }
        checkChildString = ((Element)titles.item(0)).getNodeValue();
        
        titles = title.getElementsByTagName("title");
        if(titles.getLength() == 0){
            return;
        }
        titleString = ((Element)titles.item(0)).getNodeValue();
        
        
        // Compare the matches
        float difference = ScoringUtil.computeLevenshteinDistance(checkChildString, titleString);
        
        if(difference > MIN_PERCENT){
            addResult((Element)checkChild.getParentNode(), checkChildString, results, difference, checkChildStrength + titleStrength);
        }
    }
    
    private void addResult(Element matchedNode, String title, Map<String, QueryResult> results, float score, int multiplier){
        String id = title + '_' + matchedNode.hashCode();
        QueryResult result = results.get(id);
        
        if(result == null){
            result = new QueryResult(title);
            result.setNode(matchedNode);
        }
        
        result.incrementScore(score * (float)multiplier);
        result.setMatch(true);
        results.put(id, result);
    }
    
    @Override
    public List<QueryResult> search(CWRCDataSource mainData, Element inputNode) throws CWRCException {
        try{
            Map<String, QueryResult> results = new HashMap<String, QueryResult>();
            
            // Use search on each title. Alternatives are matched based on a property
            NodeList titles = inputNode.getElementsByTagName("titleInfo");
            
            if(titles.getLength() == 0){
                return Collections.EMPTY_LIST;
            }
            
            NodeList entities = mainData.getAllEntities();
            
            for(int i = 0; i < entities.getLength(); ++i){
                Element entity = (Element)entities.item(i);
                NodeList children = entity.getElementsByTagName(("titleInfo"));
                
                for(int j = 0; j < children.getLength(); ++j){
                    Element checkChild = (Element)children.item(j);
                    
                    for(int k = 0; k < titles.getLength(); ++k){
                        searchName(checkChild, (Element)titles.item(k), inputNode, results, mainData);
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
    
    private void recordAllMatches(Element inputNode, List<QueryResult> matchesResults){
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
    public Node performMerge(QueryResult result, Element inputNode) throws CWRCException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
