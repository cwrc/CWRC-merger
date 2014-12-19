package org.ualberta.arc.mergecwrc.ui.applet;

import java.util.Collection;
import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory.MergeType;
import org.ualberta.arc.mergecwrc.merger.QueryResult;
import org.w3c.dom.Element;

/**
 *
 * @author mpm1
 */
public abstract class AdvancedAppletMerger {    
    protected CwrcApi cwrc;
    
    public static AdvancedAppletMerger getMerger(MergeType type, CwrcApi cwrc){
        switch(type){
            case AUTHOR:
                return new AuthorMerger(cwrc);
                
            case TITLE:
                return new TitleMerger(cwrc);
                
            case ORGANIZATION:
                return new OrganizationMerger(cwrc);
        }
        
        return null;
    }
    
    public abstract Collection<QueryResult> findMatches(Element input);
    public abstract Element merge(Element input, QueryResult result);
    
    public static class AuthorMerger extends AdvancedAppletMerger{

        public AuthorMerger(CwrcApi cwrc){
            this.cwrc = cwrc;
        }
        
        @Override
        public Collection<QueryResult> findMatches(Element input) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Element merge(Element input, QueryResult result) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    
    }
    
    public static class TitleMerger extends AdvancedAppletMerger{
        public TitleMerger(CwrcApi cwrc){
            this.cwrc = cwrc;
        }
        
        @Override
        public Collection<QueryResult> findMatches(Element input) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Element merge(Element input, QueryResult result) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    
    }
    
    public static class OrganizationMerger extends AdvancedAppletMerger{
        public OrganizationMerger(CwrcApi cwrc){
            this.cwrc = cwrc;
        }
        
        @Override
        public Collection<QueryResult> findMatches(Element input) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Element merge(Element input, QueryResult result) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    
    }
}
