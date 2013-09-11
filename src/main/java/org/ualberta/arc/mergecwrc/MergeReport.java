package org.ualberta.arc.mergecwrc;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.ualberta.arc.mergecwrc.merger.QueryResult;
import org.w3c.dom.Element;

/**
 * A report displaying information about the merge preformed.
 * @author mpm1
 */
public class MergeReport {

    private volatile PrintStream out;
    private TransformerFactory factory = TransformerFactory.newInstance();
    private Transformer transformer;
    
    public MergeReport(String name, OutputStream output) {
        try{
            transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }catch (TransformerException ex) {
            //return ex.getMessage();
        }
        
        out = new PrintStream(output);

        out.println("<report>");

        out.print("<name>");
        out.print(name);
        out.println("</name>");

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        out.print("<date>");
        out.print(df.format(new Date()));
        out.println("</date>");
    }

    public void close() {
        out.print("</report>");
        out.close();
    }

    private void getNodeAsText(Element node) {
        if(out == null){
            return;
        }
        
        try {
            DOMSource source = new DOMSource(node);
            
            if(source == null){
                return;
            }
            
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
            
            //return outString;
        } catch (TransformerException ex) {
            //return ex.getMessage();
        }
    }

    private synchronized void printString(String output) {
        out.println(output);
    }
    
    private void addElement(String entityName, Element node){
        out.print('\n');
        
        out.print('<');
        out.print(entityName);
        out.print('>');
        
        if(node != null){
            getNodeAsText(node);
        }
        
        out.print("</");
        out.print(entityName);
        out.print('>');
    }

    public void printMerge(Element inputNode, QueryResult mergeNode) {
        out.print("<merge score=");
        out.print(mergeNode.getScore());
        out.print('>');
        
        addElement("inputNode", inputNode);
        addElement("resultNode", mergeNode.getNode());

        out.print("\n</merge>");
        
        out.print("\n");
    }

    public void printAppend(Element inputNode) {
        out.print("<append>");
        
        addElement("inputNode", inputNode);

        out.print("\n</append>");
        
        out.print("\n");
    }
    
    public void printCustomElement(Element element){
        getNodeAsText(element);
    }
    
    public void printError(CWRCException ex, Element inputNode){
        out.print("<error code='");
        out.print(ex.getError().name());
        out.print("'>");
        
        addElement("inputNode", inputNode);
        
        out.print("\n</error>");
        out.print("\n");
    }
}
