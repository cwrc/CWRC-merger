package org.ualberta.arc.mergecwrc.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.merger.CWRCMerger;
import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory.MergeType;
import org.ualberta.arc.mergecwrc.ui.applet.CWRCNetworkThread;
import org.ualberta.arc.mergecwrc.ui.applet.CWRCNetworkThread.CWRCIdListRequest;
import org.ualberta.arc.mergecwrc.ui.applet.CWRCNetworkThread.CWRCRequest;
import org.ualberta.arc.mergecwrc.ui.applet.CWRCNetworkThread.CWRCXMLRequest;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mpm1
 */
public class CWRCJSOutput extends CWRCDataSource {
    private static final String ERROR_ID = "LAST_ERROR_NODE";
    private static final long WAIT_TIME = 5000l;
    private static TransformerFactory factory = TransformerFactory.newInstance();
    private String getAllUrl = "entity/list";
    private String getEntityUrl = "entity/";
    private String mergeUrl = "entity/save";
    private String addUrl = "entity/save";
    private MergeType mergeType;
    private NodeList allEntries = null;

    public CWRCJSOutput(String cwrcUrl, MergeType mergeType) {
        // Build needed urls
        cwrcUrl = cwrcUrl.trim();
        if (!cwrcUrl.endsWith("/")) {
            cwrcUrl += "/";
        }

        this.getAllUrl = cwrcUrl + this.getAllUrl;
        
        switch(mergeType){
            case AUTHOR:
                this.getAllUrl = this.getAllUrl + "?type=person";
                break;
                
            case TITLE:
                this.getAllUrl = this.getAllUrl + "?type=title";
                break;
                
            case ORGANIZATION:
                this.getAllUrl = this.getAllUrl + "?type=organization";
                break;
        }
        
        this.getEntityUrl = cwrcUrl + this.getEntityUrl;
        this.mergeUrl = cwrcUrl + this.mergeUrl;
        this.addUrl = cwrcUrl + this.addUrl;
        this.mergeType = mergeType;
    }

    private <T> CWRCRequest callPost(String url, List<NameValuePair> params, CWRCNetworkThread.RequestType type) throws CWRCException {
        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new UrlEncodedFormEntity(params));

            CWRCRequest request = null;
            UUID lock = UUID.randomUUID();
            synchronized (lock) {
                request = CWRCNetworkThread.getInstance().addRequest(lock, post, type);

                while (request.getResponse() == null) {
                    try {
                        lock.wait(WAIT_TIME);
                    } catch (InterruptedException ex) {
                        throw new CWRCException(ex);
                    }

                    if (request.getException() != null) {
                        throw new CWRCException(request.getException());
                    }
                }
            }

            return request;
        } catch (IOException ex) {
            throw new CWRCException(ex);
        }
    }

    private <T> CWRCRequest callGet(String url, CWRCNetworkThread.RequestType type) throws CWRCException {
        HttpGet get = new HttpGet(url);

        CWRCRequest request = null;
        UUID lock = UUID.randomUUID();
        synchronized (lock) {
            request = CWRCNetworkThread.getInstance().addRequest(lock, get, type);

            while (request.getResponse() == null) {
                try {
                    lock.wait(WAIT_TIME);
                } catch (InterruptedException ex) {
                    throw new CWRCException(ex);
                }

                if (request.getException() != null) {
                    throw new CWRCException(request.getException());
                }
            }
        }

        return request;
    }

    @Override
    public void appendNode(Node node) {
        try {
            String nodeAsString = getNodeAsText(node);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("xml", nodeAsString));

            callPost(this.addUrl, params, CWRCNetworkThread.RequestType.NONE);
        } catch (CWRCException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public synchronized NodeList getAllEntities() {
        if (allEntries == null) {
            try {
                List<String> ids = ((CWRCIdListRequest) this.callGet(this.getAllUrl, CWRCNetworkThread.RequestType.ID_LIST)).getResult();
                allEntries = new CWRCNodeList(ids, mergeType);
            } catch (CWRCException ex) {
                ex.printStackTrace();
                allEntries = CWRCMerger.emptyList;
            }
        }

        return allEntries;
    }

    @Override
    public void triggerMerge(Node entity, String id) {
        try {            
            String nodeAsString = getNodeAsText(entity);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("xml", nodeAsString));
            params.add(new BasicNameValuePair("id", id));
            
            callPost(this.mergeUrl, params, CWRCNetworkThread.RequestType.NONE);
        } catch (CWRCException ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public String getId(Node node) {
        try {
            return ((CWRCNodeList)allEntries).getEntry(node).getId();
        } catch (CWRCException ex) {
            ex.printStackTrace();
        }
        
        System.err.println("Error finding node " + node.toString());
        return ERROR_ID;
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

    private class CWRCNodeEntry {

        private String id;
        private volatile Element node = null;

        public CWRCNodeEntry(String id) {
            this.id = id;
        }
        
        public String getId() {
            return id;
        }

        public synchronized Element getNode() throws CWRCException {
            if (node == null) {
                try {
                    node = ((CWRCXMLRequest) callGet(getEntityUrl + new URLCodec().encode(id), CWRCNetworkThread.RequestType.XML)).getElement();
                } catch (EncoderException ex) {
                    throw new CWRCException(ex);
                }
            }

            return node;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof Node){
                return o.equals(node);
            }else if(o instanceof String){
                return this.id.equals(o);
            }
            
            return this.hashCode() == o.hashCode();
        }
    }

    private class CWRCNodeList implements NodeList {

        private final List<CWRCNodeEntry> nodes = new ArrayList<CWRCNodeEntry>();
        private final List<CWRCNodeEntry> sortedNodes = new ArrayList<CWRCNodeEntry>();

        public CWRCNodeList(Collection<String> ids, MergeType mergeType) {
            String checkVal = "";
            
            switch(mergeType){
                case ORGANIZATION:
                    checkVal = "organization";
                    break;
                    
                case AUTHOR:
                    checkVal = "person";
                    break;
                    
                case TITLE:
                    checkVal = "title";
                    break;
            }
            
            for (String id : ids) {
                // Only add values that are of the needed mergeType.
                if(id.startsWith(checkVal)){
                    nodes.add(new CWRCNodeEntry(id));
                }
            }
        }
        
        public CWRCNodeEntry getEntry(Node node) throws CWRCException{
            
            for(CWRCNodeEntry entry : nodes){
                if(entry.equals(node)){
                    return entry;
                }
            }
            
            throw new CWRCException();
        }

        @Override
        public Node item(int i) {
            try {
                return nodes.get(i).getNode();
            } catch (CWRCException ex) {
                ex.printStackTrace();
            }

            return null;
        }

        @Override
        public int getLength() {
            return nodes.size();
        }
    }
}
