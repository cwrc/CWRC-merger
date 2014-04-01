package org.ualberta.arc.mergecwrc.ant;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.ualberta.arc.mergecwrc.MergeReport;
import org.ualberta.arc.mergecwrc.io.MODSFile;
import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory;
import org.ualberta.arc.mergecwrc.merger.CWRCMergerFactory.MergeType;
import org.ualberta.arc.mergecwrc.ui.MergerController;
import org.ualberta.arc.mergecwrc.ui.swing.MergerControllerSwing;

/**
 *
 * @author mpm1
 */
public class MergeTitles {
    private FileSet fileset;

    /**
     * @param fileset The fileset to be set.
     */
    public void addFileset(FileSet fileset) {
            this.fileset = fileset;
    }
    
    private List<InputStream> getFileInputCollection() throws CWRCException{
        
        if(fileset == null){
            return Collections.EMPTY_LIST;
        }
        
        List<InputStream> out = new ArrayList<InputStream>(fileset.size());
        
        Iterator iterator = fileset.iterator();
        while(iterator.hasNext()){
            FileResource file = (FileResource) iterator.next();
            
            try {
                out.add(new FileInputStream(file.getFile()));
            } catch (FileNotFoundException ex) {
                throw new CWRCException(ex);
            }
            
        }
        
        return out;
    }
    
    /**
     * Executes the ant task.
     */
    public void execute() throws IOException{
        MergerController controller = new MergerControllerSwing();
        CWRCMergerFactory.setMaxThreads(3);
        
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        MergeReport report = new MergeReport("Title Report", new FileOutputStream("report" + df.format(new Date()) + ".xml"));
        
        Iterator iterator = fileset.iterator();
        try {
            List<InputStream> inFiles = getFileInputCollection();
            
            CWRCMergerFactory factory = CWRCMergerFactory.getFactory(controller, MergeType.TITLE, report, inFiles, true);
            //((TitleModsMerger)factory.getMerger()).setReport(new MergeReport("Title Match Report", new FileOutputStream("match_report" + df.format(new Date()) + ".xml")));
            MODSFile mainSrc = new MODSFile("title_mods.xml");

            while (iterator.hasNext()) {
                try {
                    FileResource file = (FileResource) iterator.next();

                    System.out.println("Reading File: " + file.getFile().getAbsolutePath());
                    System.out.flush();
                    MODSFile cwrcFile = new MODSFile(file.getFile().getAbsolutePath());

                    factory.mergeFile(mainSrc, cwrcFile);
                } catch (CWRCException ex) {
                    ex.printStackTrace();
                }
            }
            
            mainSrc.writeFile();
        } catch (CWRCException ex) {
            ex.printStackTrace();
        }

        report.close();
    }
}
