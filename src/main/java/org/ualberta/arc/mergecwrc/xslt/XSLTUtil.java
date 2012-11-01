package org.ualberta.arc.mergecwrc.xslt;

import java.io.InputStream;
import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.ualberta.arc.mergecwrc.CWRCException;

/**
 *
 * @author mpm1
 */
public class XSLTUtil {    
    public static String transformStream(InputStream xslt, InputStream xml) throws CWRCException{
        try {
            //TransformerFactory factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", net.sf.saxon.TransformerFactoryImpl.class.getClassLoader());
            net.sf.saxon.TransformerFactoryImpl factory = new net.sf.saxon.TransformerFactoryImpl();
            Transformer transformer = factory.newTransformer(new StreamSource(xslt));
            
            StringWriter writer = new StringWriter();
            
            transformer.transform(new StreamSource(xml), new StreamResult(writer));
            
            return writer.toString();
        } catch (TransformerException ex) {
            System.err.println(ex.getMessageAndLocation());
            throw new CWRCException(ex);
        }
    }
}
