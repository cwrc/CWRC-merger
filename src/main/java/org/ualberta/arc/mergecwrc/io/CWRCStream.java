package org.ualberta.arc.mergecwrc.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author mpm1
 */
public class CWRCStream extends CWRCDataSource {

    private volatile Document doc;

    public CWRCStream(String data) throws CWRCException {
        this(new ByteArrayInputStream(data.getBytes()));
    }

    public CWRCStream(InputStream data) throws CWRCException {
        try {
            // Create the dom
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.parse(data);
        } catch (SAXException ex) {
            throw new CWRCException(ex);
        } catch (IOException ex) {
            throw new CWRCException(ex);
        } catch (ParserConfigurationException ex) {
            throw new CWRCException(ex);
        }
    }

    @Override
    public void appendNode(Node node) {
        Node entity = ((Document) doc).importNode(node, true);
        ((Document) doc).getDocumentElement().appendChild(entity);
    }

    @Override
    public NodeList getAllEntities() {
        return doc.getDocumentElement().getElementsByTagName("entity");
    }

    /*
    @Override
    public NodeList executeQuery(String query, VariableResolver resolver) throws CWRCException {
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
    }*/

    @Override
    public void triggerMerge(Node entity, String id) {
        // We do not need to do anything in this data source.
    }

    @Override
    public String getId(Node node) {
        return node.toString();
    }
}
