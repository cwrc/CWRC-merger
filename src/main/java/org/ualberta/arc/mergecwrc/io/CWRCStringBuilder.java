package org.ualberta.arc.mergecwrc.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.merger.CWRCMerger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author mpm1
 */
public class CWRCStringBuilder extends CWRCDataSource {

    public final StringBuilder builder = new StringBuilder();
    public volatile Document doc;
    private boolean isFileClosed = false;
    private String name;

    public CWRCStringBuilder(String name) {
        this.name = name;
    }
    
    public InputStream getAsStream() throws CWRCException{
        try {
            return new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new CWRCException(ex);
        }
    }

    // These functions help create the initial file by adding in information for each chunk
    public void addToFile(CharSequence data) {
        synchronized (builder) {
            if (!isFileClosed) {
                builder.append(data);
            }
        }
    }

    public void closeFile() throws CWRCException {
        synchronized (builder) {
            if (!isFileClosed) {
                try {
                    // Create the dom
                    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    
                    // This is in place to solve the error: MalformedByteSequenceException: Invalid Byte 1 Of 1-Byte UTF-8 Sequence.
                    InputSource is = new InputSource(getAsStream());
                    is.setEncoding("UTF-8");
                    
                    doc = docBuilder.parse(is);
                } catch (SAXException ex) {
                    throw new CWRCException(ex);
                } catch (IOException ex) {
                    throw new CWRCException(ex);
                } catch (ParserConfigurationException ex) {
                    throw new CWRCException(ex);
                }
            }

            isFileClosed = true;
        }
    }

    // Basic CWRCDataSource Functions;
    @Override
    public void appendNode(Node node) {
        if (isFileClosed) {
            Node entity = ((Document) doc).importNode(node, true);
            ((Document) doc).getDocumentElement().appendChild(entity);
        }
    }

    @Override
    public NodeList getAllEntities() {
        if (isFileClosed) {
            return doc.getDocumentElement().getElementsByTagName("entity");
        }

        return CWRCMerger.emptyList;
    }

    /*
    @Override
    public NodeList executeQuery(String query, VariableResolver resolver) throws CWRCException {
        if (isFileClosed) {
            try {
                XPathExpression expr = null;
                if (resolver != null) {
                    XPath xpath = getFactory().newXPath();
                    xpath.setXPathVariableResolver(resolver);
                    expr = xpath.compile(query);
                } else {
                    expr = CWRCFile.getExpression(query);
                }

                synchronized (doc) {
                    Object result = expr.evaluate(doc, XPathConstants.NODESET);

                    return (NodeList) result;
                }
            } catch (XPathExpressionException ex) {
                throw new CWRCException(CWRCException.Error.QUERY_ERROR, ex);
            }
        }
        
        return CWRCMerger.emptyList;
    }
     */

    @Override
    public void triggerMerge(Node entity, String id) {
        // We do not need to do anything in this data source.
    }

    @Override
    public String getId(Node node) {
        return node.toString();
    }
}
