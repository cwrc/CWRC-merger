package org.ualberta.arc.mergecwrc.ui.applet;

import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory.MergeType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mpm1
 */
public class CwrcApi {
    private String cwrcApiName;
    
    public CwrcApi(String cwrcApiName){
        this.cwrcApiName = cwrcApiName;
    }
    
    public NodeList search(MergeType type, String query){
        SearchResultList list = new SearchResultList(type, query, cwrcApiName);
        
        return list;
    }
    
    public static class SearchResultList implements NodeList{
        private String objectName = null;
        public SearchResultList(MergeType type, String query, String cwrcApiName){
            objectName = cwrcApiName + "." + type.getShortType();
        }
        
        public Node item(int i) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getLength() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}
