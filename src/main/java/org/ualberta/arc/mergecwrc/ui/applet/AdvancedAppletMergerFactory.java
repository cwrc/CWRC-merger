package org.ualberta.arc.mergecwrc.ui.applet;

import java.io.InputStream;
import org.ualberta.arc.mergecwrc.MergeReport;
import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory.MergeType;
import org.ualberta.arc.mergecwrc.ui.MergerController;

/**
 *
 * @author mpm1
 */
public class AdvancedAppletMergerFactory {
    private MergerController controller = null;
    private MergeType type = null;
    private MergeReport report = null;
    private boolean autoMerge = false;
    
    public AdvancedAppletMergerFactory(MergerController controller, MergeType type, MergeReport report, boolean autoMerge){
        this.controller = controller;
        this.type = type;
        this.report = report;
        this.autoMerge = autoMerge;
    }
    
    
   /**
     * Performs the find and merge operations on the data specified in the file.
     * 
     * @param data The data to check against.
     */
    public void run(InputStream data){
        
    }
    
    
}
