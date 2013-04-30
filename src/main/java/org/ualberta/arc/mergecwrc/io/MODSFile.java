package org.ualberta.arc.mergecwrc.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.merger.custom.TitleModsMerger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author mpm1
 */
public class MODSFile extends CWRCDataSource{
    private volatile File file;
    private volatile Document doc;
    
    public MODSFile(String fileName) throws CWRCException {
        loadFile(fileName);
    }
    
    @Override
    public void triggerMerge(Node entity, String id){
        // We do not need to do anything in this data source.
    }
    
    @Override
    public void appendNode(Node node) {
        Node entity = ((Document) doc).importNode(node, true);
        ((Document) doc).getDocumentElement().appendChild(entity);
    }
    
    @Override
    public NodeList getAllEntities() {
        return doc.getDocumentElement().getElementsByTagName(TitleModsMerger.ENTITY_NODE);
    }
    
    /**
     * Writes the DOM information back to a file.
     * 
     * @throws CWRCException 
     */
    public void writeFile() throws CWRCException {
        try {
            FileOutputStream fileOut = new FileOutputStream(file);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(fileOut);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException ex) {
            throw new CWRCException(CWRCException.Error.DATASOURCE_SAVE, ex);
        } catch (IOException ex) {
            throw new CWRCException(CWRCException.Error.DATASOURCE_SAVE, ex);
        } catch (TransformerException ex) {
            throw new CWRCException(CWRCException.Error.DATASOURCE_SAVE, ex);
        }
    }

    private void loadFile(String fileName) throws CWRCException {
        file = new File(fileName);

        if (file.exists()) {
        } else {
            try {
                FileOutputStream fileOut = new FileOutputStream(file);
                XMLStreamWriter out = XMLOutputFactory.newInstance().createXMLStreamWriter(new OutputStreamWriter(fileOut, "UTF-8"));

                out.writeStartDocument();
                out.writeStartElement(TitleModsMerger.MAIN_NODE);
                out.writeEndElement();
                out.writeEndDocument();

                out.close();
            } catch (FileNotFoundException ex) {
                throw new CWRCException(CWRCException.Error.DATASOURCE_CREATE, ex);
            } catch (IOException ex) {
                throw new CWRCException(CWRCException.Error.DATASOURCE_CREATE, ex);
            } catch (XMLStreamException ex) {
                throw new CWRCException(CWRCException.Error.DATASOURCE_CREATE, ex);
            }
        }

        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            String systemId = file.toURI().toURL().toString();
            doc = docBuilder.parse(new InputSource(systemId));
        } catch (ParserConfigurationException ex) {
            throw new CWRCException(CWRCException.Error.DATASOURCE_LOAD, ex);
        } catch (MalformedURLException ex) {
            throw new CWRCException(CWRCException.Error.DATASOURCE_LOAD, ex);
        } catch (SAXException ex) {
            throw new CWRCException(CWRCException.Error.DATASOURCE_LOAD, ex);
        } catch (IOException ex) {
            throw new CWRCException(CWRCException.Error.DATASOURCE_LOAD, ex);
        }
    }

    @Override
    public String getId(Node node) {
        return node.toString();
    }
}
