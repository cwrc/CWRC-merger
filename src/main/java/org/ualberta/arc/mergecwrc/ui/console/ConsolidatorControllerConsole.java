package org.ualberta.arc.mergecwrc.ui.console;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.ualberta.arc.mergecwrc.merger.CWRCConsolidator;

/**
 *
 * @author mpm1
 */
public class ConsolidatorControllerConsole {

    public static void main(String args[]) throws Exception{
        String config = "<CWRCConsolidation type='MODS'>"
                + "<CompareField path='/mods/titleInfo[not(@type)]/title' type='LevenshteinPercent' percent='0.8' result='DROP'>"
                + "<CompareField path='/mods/name/namePart' type='LevenshteinPercent' percent='0.9' result='DROP_IF_EXISTS'/>"
                + "<CompareField path='/mods/relatedItem[type=\"host\"]/titleInfo/title' type='LevenshteinPercent' percent='0.9' result='DROP_IF_EXISTS'/>"
                + "<CompareField path='/mods/dateIssues' type='Date' result='RANK'/>"
                + "</CompareField>"
                + "</CWRCConsolidation>";

        InputStream inputFile = new FileInputStream(new File("C:/Users/default.default-PC/Documents/cwrc-conversion/title_build/ceww.MGXML"));
        File outputFile = null;

        CWRCConsolidator consolidator = new CWRCConsolidator(new ByteArrayInputStream(config.getBytes("UTF-8")), inputFile, outputFile);
        
        consolidator.consolidate();
    }
}