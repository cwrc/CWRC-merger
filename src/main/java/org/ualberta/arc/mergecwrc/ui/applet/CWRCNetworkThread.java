package org.ualberta.arc.mergecwrc.ui.applet;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * This class has been implemented so that javascript functions do not directly make web calls. This is to get around java security implementations.
 * 
 * @author mpm1
 */
public class CWRCNetworkThread extends Thread {

    public static enum RequestType {
        NONE,
        XML,
        ID_LIST
    }
    private static final long WAIT_TIME = 10000l;
    public static CWRCNetworkThread instance = new CWRCNetworkThread();
    private final String requestLock = "Requests";
    private List<CWRCRequest> requests = new ArrayList<CWRCRequest>();
    private boolean running = false;
    
    private String username = null;
    private String password = null;

    public static CWRCNetworkThread getInstance() {
        return instance;
    }
    
    public void setCredentials(String username, String password){
        this.username = username;
        this.password = password;
    }
    
    public void triggerStop(){
        running = false;
    }

    @Override
    public void run() {
        running = true;
        
        while (running) {
            CWRCRequest request = null;
            synchronized (requestLock) {
                if (requests.size() > 0) {
                    request = requests.remove(0);
                } else {
                    try {
                        requestLock.wait(WAIT_TIME);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            if (request != null) {
                synchronized (request.getLock()) {
                    //AccessController.doPrivileged(new CallCWRCRequest(request));
                    new CallCWRCRequest(request).run();

                    request.getLock().notify();
                }
            }
        }
    }

    public CWRCRequest addRequest(Object lock, HttpUriRequest httpRequest, RequestType type) {
        synchronized (requestLock) {
            CWRCRequest request = null;

            switch (type) {
                case XML:
                    request = new CWRCXMLRequest(lock, httpRequest);
                    break;

                case ID_LIST:
                    request = new CWRCIdListRequest(lock, httpRequest);
                    break;
                            

                default:
                    request = new CWRCRequest(lock, httpRequest);
            }

            requests.add(request);

            requestLock.notifyAll();

            return request;
        }
    }

    public static class CWRCRequest {

        private Object lock;
        private HttpUriRequest request;
        private HttpResponse response = null;
        private Exception exception = null;

        public CWRCRequest(Object lock, HttpUriRequest request) {
            this.lock = lock;
            this.request = request;
        }

        public HttpUriRequest getRequest() {
            return request;
        }

        public Object getLock() {
            return lock;
        }

        public HttpResponse getResponse() {
            return response;
        }

        public void setResponse(HttpResponse response) throws CWRCException {
            this.response = response;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }
    }

    public static class CWRCXMLRequest extends CWRCRequest {

        private static DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        private static volatile Document xmlDoc = null;
        
        private Element element;
        
        public static void reset() throws CWRCException{
            try {
                xmlDoc = docFactory.newDocumentBuilder().newDocument();
                
                xmlDoc.appendChild(xmlDoc.createElement("cwrc"));
            } catch (ParserConfigurationException ex) {
                throw new CWRCException(ex);
            }
        }

        public CWRCXMLRequest(Object lock, HttpUriRequest request) {
            super(lock, request);
        }

        @Override
        public void setResponse(HttpResponse response) throws CWRCException {
            try {
                DocumentBuilder builder = docFactory.newDocumentBuilder();

                Document doc = builder.parse(response.getEntity().getContent());

                element = (Element)xmlDoc.importNode(doc.getDocumentElement(), true);
                
                element = (Element)xmlDoc.getDocumentElement().appendChild(element);
                
                super.setResponse(response);
            } catch (SAXException ex) {
                throw new CWRCException(ex);
            } catch (IOException ex) {
                throw new CWRCException(ex);                
            } catch (ParserConfigurationException ex) {
                throw new CWRCException(ex);
            }
        }

        public Element getElement() {
            return element;
        }
    }

    public static class CWRCIdListRequest extends CWRCRequest {

        private List<String> result;

        public CWRCIdListRequest(Object lock, HttpUriRequest request) {
            super(lock, request);
        }
        
        /**
         * Obtains a list of all the id fields in an array of JSON objects
         * @param input
         * @return
         * @throws IOException 
         */
        private List<String> convertJSON(InputStream input) throws IOException{
            List<String> output = new ArrayList<String>();
            boolean inList = false;
            boolean inEscape = false;
            boolean inObject = false;
            StringBuilder field = null;
            StringBuilder fieldData = null;
            StringBuilder current = null;

            int nextVal = 0;
            while ((nextVal = input.read()) > -1) {
                char character = (char)nextVal;
                if (inList) {
                    if(inObject){
                        if(inEscape){
                            if(current != null){
                                current.append(character);
                                inEscape = false;
                            }
                        }else if(character == '\\'){
                            inEscape = true;
                        }else if(character == '"'){
                            if(field == null){
                                field = new StringBuilder();
                                current = field;
                            }else if(fieldData == null){
                                if(current == null){
                                    fieldData = new StringBuilder();
                                    current = fieldData;
                                }else {
                                    current = null;
                                }
                            }else{
                                if(StringUtils.equalsIgnoreCase(field, "id")){
                                    output.add(fieldData.toString());
                                }
                                
                                fieldData = null;
                                field = null;
                                current = null;
                            }
                        }else if(current == null){
                            if(character == '}'){
                                inObject = false;
                            }
                        }else{
                            current.append(character);
                        }
                    }else{
                        if(character == '{'){
                            inObject = true;
                            field = null;
                            fieldData = null;
                            current = null;
                            inEscape = false;
                        }
                    }
                } else {
                    if(character == '['){
                        inList = true;
                        inObject = false;
                    }
                }
            }
            
            return output;
        }

        @Override
        public void setResponse(HttpResponse response) throws CWRCException {
            try {
                InputStream stream = response.getEntity().getContent();
                
                result = convertJSON(stream);
                
                stream.close();
                
                super.setResponse(response);
            } catch (IOException ex) {
                throw new CWRCException(ex);
            }
        }

        public List<String> getResult() {
            return result;
        }
    }

    private class CallCWRCRequest implements PrivilegedAction<CWRCRequest> {

        private CWRCRequest request;

        public CallCWRCRequest(CWRCRequest request) {
            this.request = request;
        }

        public CWRCRequest run() {
            try {
                HttpClient client = new DefaultHttpClient();
                
                //TODO: turn this into passed parameters
                if(username != null){
                    ((DefaultHttpClient)client).getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                }
                
                request.setResponse(client.execute(request.getRequest()));
            } catch (Exception ex) {
                request.setException(ex);
            }

            return request;
        }
    }
}
