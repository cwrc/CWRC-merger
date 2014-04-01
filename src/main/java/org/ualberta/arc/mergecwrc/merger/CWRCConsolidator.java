package org.ualberta.arc.mergecwrc.merger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang3.StringUtils;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.io.CWRCDataSource;
import org.ualberta.arc.mergecwrc.utils.ScoringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author mpm1
 */
public class CWRCConsolidator extends DefaultHandler{
    public static final String ELEMENT_MAIN = "CWRCConsolidation";
    public static final String ELEMENT_FIELD = "CompareField";
    public static final String ATTRIBUTE_TYPE = "type";
    public static final String ATTRIBUTE_PATH = "path";
    public static final String ATTRIBUTE_PERCENT = "percent";
    public static final String ATTRIBUTE_RESULT = "result";

    private Document inputData = null;
    private CWRCDataSource outputData = null;
    private CompareField mainCompareField = null;
    private FileType type = FileType.MODS;
    
    public CWRCConsolidator(InputStream configuration, InputStream inputStream, File outputFile) throws CWRCException {
        try {
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            
            // Load the configuration
            readConfiguration(saxFactory, configuration);
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Load the input file
            inputData = builder.parse(inputStream);
            
        } catch(IOException ex){
            throw new CWRCException(ex);
        }catch(ParserConfigurationException ex){
            throw new CWRCException(ex);
        }catch(SAXException ex){
            throw new CWRCException(ex);
        }finally {
            
        }
    }
    
    public void consolidate(){
        // Create Element List
        LinkedList<Element> inputElements = new LinkedList<Element>();
        Element checkElement = inputData.getDocumentElement();
        NodeList list = checkElement.getChildNodes();
        
        for(int i = 0; i < list.getLength(); ++i){
            Node node = list.item(i);
            
            if(node.getNodeType() == Node.ELEMENT_NODE){
                inputElements.push((Element)node);
            }
        }
        
        int count = 0;
        while(!inputElements.isEmpty()){
            checkElement = inputElements.poll();
            
            mainCompareField.compareFields(checkElement, inputElements);
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="read-configuration">
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        System.out.println("Reading element " + qName + "...");
        
        if(StringUtils.equalsIgnoreCase(ELEMENT_MAIN, qName)){
            String value = attributes.getValue(ATTRIBUTE_TYPE);
            
            if(value != null){
                type = FileType.valueOf(value);
            }
        }else if(StringUtils.equalsIgnoreCase(ELEMENT_FIELD, qName)){
            CompareField parent = mainCompareField;
            mainCompareField = new CompareField(parent, attributes);
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        System.out.println("Finished reading element " + qName + ".");
        
        if(StringUtils.equalsIgnoreCase(ELEMENT_MAIN, qName)){
            
        }else if(StringUtils.equalsIgnoreCase(ELEMENT_FIELD, qName)){
            CompareField parent = mainCompareField.getParent();
            
            if(parent != null){
                parent.addChild(mainCompareField);
                mainCompareField = parent;
            }
        }
    }
    
    private void readConfiguration(SAXParserFactory factory, InputStream configuration) throws CWRCException{
        System.out.println("Reading configuration...");
        
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(configuration, this);
            
        } catch (IOException ex) {
            throw new CWRCException(ex);
        } catch (ParserConfigurationException ex) {
            throw new CWRCException(ex);
        } catch (SAXException ex) {
            throw new CWRCException(ex);
        }
    }
    // </editor-fold>

    /**
     * An object used to compare fields.
     * 
     * @param <T> Type of fields being compared
     * @param <R> Return type of the comparison result.
     */
    private static interface CompareType<T, R extends Number> {

        public R compare(T o1, T o2);
    }

    private static class CompareLevenshteinPercent implements CompareType<String, Float> {

        public Float compare(String o1, String o2) {
            return ScoringUtil.computeLevenshteinPercent(o1, o2);
        }
    }

    private static enum Result {
        DROP,
        DROP_IF_EXISTS,
        RANK;
    }
    
    private static enum FileType {
        MODS
    }

    private static class CompareField {

        private String path;
        private List<CompareField> compareFields = new ArrayList<CompareField>();
        private CompareType compareType = null;
        private Result result;
        private CompareField parent;
        private Float percent;

        public CompareField(CompareField parent, Attributes attributes) {
            this.path = attributes.getValue(ATTRIBUTE_PATH);
            this.result = Result.valueOf(attributes.getValue(ATTRIBUTE_RESULT));
            
            String value = attributes.getValue(ATTRIBUTE_PERCENT);
            if(value != null){
                percent = Float.parseFloat(value);
            }
            
            value = attributes.getValue(ATTRIBUTE_TYPE);
            if(StringUtils.equalsIgnoreCase("LevenshteinPercent", value)){
                compareType = new CompareLevenshteinPercent();
            }else if(StringUtils.equalsIgnoreCase("Date", value)){
                // TODO: Date compare type
            }
        }
        
        public CompareField getParent() {
            return parent;
        }
        
        public void addChild(CompareField child){
            compareFields.add(child);
        }

        /**
         * 
         * @param inputElements The elements to compare with.
         * @return The elements matching the result.
         */
        public List<Element> compareFields(Element initialElement, List<Element> inputElements) {
            XPathFactory factory = XPathFactory.newInstance();
            
            return compareFields(initialElement, inputElements, factory);
        }
        
        protected List<Element> compareFields(Element initialElement, List<Element> inputElements, XPathFactory factory){
            // First find the field.
            String field = null;
            XPath xpath = factory.newXPath();
            try {
                XPathExpression expression = xpath.compile(path);
                field = expression.evaluate(initialElement);
                
                System.out.println("Field: " + field);
            } catch (XPathExpressionException ex) {
                Logger.getLogger(CWRCConsolidator.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Compare the field

            return null;
        }
    }
}
