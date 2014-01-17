package org.ualberta.arc.mergecwrc.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Vector;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author mpm1
 */
public class MODSFile extends CWRCDataSource {

    private volatile File file;
    private volatile Document doc;
    private volatile Element docElement;

    public MODSFile(String fileName) throws CWRCException {
        loadFile(fileName);
        docElement = doc.getDocumentElement();
    }

    @Override
    public void triggerMerge(Node entity, String id) {
        // We do not need to do anything in this data source.
    }

    @Override
    public void appendNode(Node node) {
        Node entity = doc.adoptNode(node);
        docElement.appendChild(entity);

        childrenFound = false;
    }

    @Override
    public NodeList getAllEntities() {
        if (!childrenFound) {
            synchronized (childrenKey) {
                if (!childrenFound) {
                    List<Element> output = new Vector<Element>();

                    for (Node child = docElement.getFirstChild(); child != null; child = child.getNextSibling()) {
                        if (child.getNodeType() == Node.ELEMENT_NODE
                                && TitleModsMerger.ENTITY_NODE.equals(child.getNodeName())) {
                            output.add((Element) child);
                        }
                    }
                    
                    this.children.setChildren(output);

                    childrenFound = true;
                }
            }
        }

        return children; // Testing if this works correctly by only grabbing the top layer.
        //return docElement.getElementsByTagName(TitleModsMerger.ENTITY_NODE);
    }

    /**
     * Writes the DOM information back to a file.
     * 
     * @throws CWRCException 
     */
    public void writeFile() throws CWRCException {
        childrenFound = false;
        
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
        childrenFound = false;
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
