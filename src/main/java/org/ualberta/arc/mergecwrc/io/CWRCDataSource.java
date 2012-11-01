package org.ualberta.arc.mergecwrc.io;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The source used to obtain or store entity information.
 * @author mpm1
 */
public abstract class CWRCDataSource {

    private volatile static XPathFactory factory = null;
    private volatile static Map<String, XPathExpression> compiledExpressions = new HashMap<String, XPathExpression>();

    /**
     * Appends the specified node to the datasource.
     * 
     * @param node The node to append. 
     */
    public abstract void appendNode(Node node);

    /**
     * @return The list of all Elements of type "entity".
     */
    public abstract NodeList getAllEntities();
    
    public abstract String getId(Node node);

    /**
     * Executes an XPath query on the datasource.
     * @param query The XPath query.
     * @param resolver A map containing all variables and their value.
     * @return A list containing all results from the XPath.
     * @throws CWRCException 
     */
    //public abstract NodeList executeQuery(String query, VariableResolver resolver) throws CWRCException;

    /**
     * Submits the merge to the datasource.
     * 
     * @param entity The entity to merge into the database. 
     */
    public abstract void triggerMerge(Node entity, String id);

    /**
     * @param xpath The xpath to find the stored expression for.
     * @return Obtains a stored expression.
     * @throws CWRCException
     * @throws XPathExpressionException 
     */
    public synchronized static XPathExpression getExpression(String xpath) throws CWRCException, XPathExpressionException {
        XPathExpression expr = compiledExpressions.get(xpath);

        if (expr == null) {
            XPath xpathObj = getFactory().newXPath();
            expr = xpathObj.compile(xpath);

            compiledExpressions.put(xpath, expr);
        }

        return expr;
    }

    /**
     * @return Obtains the default XPathFactory.
     * @throws CWRCException 
     */
    public synchronized static XPathFactory getFactory() throws CWRCException {
        if (factory == null) {
            factory = new net.sf.saxon.xpath.XPathFactoryImpl();
            //XPathFactory.newInstance(NamespaceConstant.OBJECT_MODEL_SAXON, "net.sf.saxon.xpath.XPathFactoryImpl", net.sf.saxon.xpath.XPathFactoryImpl.class.getClassLoader());
        }

        return factory;
    }

    /**
     * The variable resolver used to map parameters for an xpath query.
     */
    public static class VariableResolver implements XPathVariableResolver {

        private Map<String, String> variables = new HashMap<String, String>();

        /**
         * Sets a parameter entry into the variable resolver.
         * @param key
         * @param value 
         */
        public void setVariable(String key, String value) {
            if (variables.containsKey(key)) {
                variables.remove(key);
            }

            variables.put(key, value);
        }

        @Override
        public Object resolveVariable(QName varName) {
            String key = varName.getLocalPart();
            return variables.get(key);
        }
    }
}
