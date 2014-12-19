package org.ualberta.arc.mergecwrc.io;

import java.io.ByteArrayOutputStream;
import javax.swing.JApplet;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import netscape.javascript.JSObject;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory.MergeType;
import org.ualberta.arc.mergecwrc.utils.JavaScriptUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mpm1
 */
public class CWRCAPIOutput extends CWRCDataSource {
    private static TransformerFactory factory = TransformerFactory.newInstance();
    private String apiObject;
    private JApplet applet;
    private MergeType mergeType;

    public CWRCAPIOutput(String apiObject, MergeType mergeType, JApplet applet) {
        this.apiObject = apiObject + "['" + mergeType.getShortType() + "']";
        this.applet = applet;
        this.mergeType = mergeType;
    }
    
    private String getNodeAsText(Node node) throws CWRCException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            DOMSource source = new DOMSource(node);
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);

            return out.toString();
        } catch (TransformerException ex) {
            throw new CWRCException(ex);
        }
    }

    @Override
    public void appendNode(Node node){
        try {
            JSObject jso = JSObject.getWindow(applet);
            String nodeAsString = getNodeAsText(node);
            
            JavaScriptUtil.callFunction(jso, apiObject + ".newEntity", new Object[]{node});
        } catch (Exception ex) {
            //throw new CWRCException(ex);
        }
    }

    @Override
    public NodeList getAllEntities() {
        try {
            JSObject jso = JSObject.getWindow(applet);
            
            Object result = JavaScriptUtil.callFunction(jso, apiObject + ".searchEntity", new Object[]{""});
            
        } catch (Exception ex) {
            //throw new CWRCException(ex);
        }
        
        return new NodeList(){

                public Node item(int i) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                public int getLength() {
                    return 0;
                }
                
            }; //TODO: Send proper list
    }

    @Override
    public String getId(Node node) {
        if(node instanceof Element){
            if(mergeType == mergeType.TITLE){
                
            }else{
                
            }
        }
        
        return null;
    }

    @Override
    public void triggerMerge(Node entity, String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public class CWRCSearch {
        public int limit = 100;
        public int page = 0;
        public String query = "";
        
        public CWRCSearch(String query, int limit, int page){
            this.query = query;
            this.limit = limit;
            this.page = page;
        }
    }
}
