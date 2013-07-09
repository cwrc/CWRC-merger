package org.ualberta.arc.mergecwrc.ui;

import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.ualberta.arc.mergecwrc.merger.QueryResult;
import org.ualberta.arc.mergecwrc.utils.ScoringUtil;
import org.ualberta.arc.mergecwrc.utils.ScoringUtil.SectionDiff;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An entry used to display multiple matches.
 * @author mmckella
 */
public class MultipleMatchModel {

    private List<QueryResult> possibleMatches;
    private String name;
    private Element inputNode;
    private boolean selected = false;
    private QueryResult selection = null;
    private List<SectionDiff> difference= null;
    boolean layerIn;
    
    public MultipleMatchModel(String name, List<QueryResult> possibleMatches, Element inputNode, boolean layerIn) {
        this.possibleMatches = possibleMatches;
        this.name = name;
        this.inputNode = inputNode;
        this.layerIn = layerIn;
    }
    
    public boolean isSelected(){
        return selected;
    }
    
    public void setSelection(QueryResult selection) {
        this.selection = selection;
        selected = true;
    }
    
    public QueryResult getSelection(){
        return selection;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getInputNode(){
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            // This is done to allow an even comparison.
            Node node = layerIn ? inputNode.getFirstChild() : inputNode;
            int index = 0;
            while(node != null && Node.ELEMENT_NODE != node.getNodeType()){
                System.err.println(index + " Node Type: " + node.getNodeType());
                ++index;
                node = node.getNextSibling();
            }
            
            DOMSource source = new DOMSource(node);
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
            
            return out.toString();
        } catch (TransformerException ex) {
            return ex.getMessage();
        }
    }
    
    public synchronized List<SectionDiff> getPossibleMatchDifference(QueryResult result) throws IndexOutOfBoundsException{
        return ScoringUtil.getSimpleDiff(getInputNode(), result.getNodeAsText());
    }

    public List<QueryResult> getPossibleMatches() {
        return possibleMatches;
    }
}
