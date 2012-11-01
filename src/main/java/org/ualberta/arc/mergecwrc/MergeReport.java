package org.ualberta.arc.mergecwrc;

import java.io.ByteArrayOutputStream;
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

    public MergeReport(String name, OutputStream output) {
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

    private String getNodeAsText(Element node) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            DOMSource source = new DOMSource(node);
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);

            return out.toString();
        } catch (TransformerException ex) {
            return ex.getMessage();
        }
    }

    private synchronized void printString(String output) {
        out.println(output);
    }
    
    private void addElement(StringBuilder builder, String entityName, String text){
        builder.append('\n');
        
        builder.append('<');
        builder.append(entityName);
        builder.append('>');
        
        builder.append(text);
        
        builder.append("</");
        builder.append(entityName);
        builder.append('>');
    }

    public void printMerge(Element inputNode, QueryResult mergeNode) {
        StringBuilder builder = new StringBuilder("<merge score=");
        builder.append(mergeNode.getScore());
        builder.append('>');
        
        addElement(builder, "inputNode", getNodeAsText(inputNode));
        addElement(builder, "resultNode", getNodeAsText(mergeNode.getNode()));

        builder.append("\n</merge>");
        
        printString(builder.toString());
    }

    public void printAppend(Element inputNode) {
        StringBuilder builder = new StringBuilder("<append>");
        
        addElement(builder, "inputNode", getNodeAsText(inputNode));

        builder.append("\n</append>");
        
        printString(builder.toString());
    }
    
    public void printCustomElement(Element element){
        printString(getNodeAsText(element));
    }
    
    public void printError(CWRCException ex, Element inputNode){
        StringBuilder builder = new StringBuilder("<error code='");
        builder.append(ex.getError().name());
        builder.append("'>");
        
        addElement(builder, "inputNode", getNodeAsText(inputNode));
        
        builder.append("\n</error>");
    }
}
