package org.ualberta.arc.mergecwrc.merger;

import java.io.ByteArrayOutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;

/**
 * A result from the query search.
 * @author mpm1
 */
public class QueryResult {
    private float score = 0;
    private Element node = null;
    private boolean match = false;
    private String name;
    private String id;
    
    public QueryResult(String id){
        this.id = id;
    }

    public void incrementScore(float value){
        score += value;
    }
    
    public void setScore(float score) {
        this.score = score;
    }

    public float getScore() {
        return score;
    }

    public void setNode(Element node) {
        this.node = node;
    }
    
    public Element getNode() {
        return node;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public boolean isMatch() {
        return match;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return score + " " + name;
    }
    
    public String getNodeAsText() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            DOMSource source = new DOMSource(node);
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
            
            return out.toString();
        } catch (TransformerException ex) {
            return ex.getMessage();
        }
    }
}
