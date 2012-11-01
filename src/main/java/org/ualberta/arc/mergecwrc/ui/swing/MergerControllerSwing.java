package org.ualberta.arc.mergecwrc.ui.swing;

import java.io.PrintStream;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import org.ualberta.arc.mergecwrc.ui.MergerController;
import org.ualberta.arc.mergecwrc.ui.MultipleMatchModel;
import org.ualberta.arc.mergecwrc.ui.swing.view.Console;
import org.ualberta.arc.mergecwrc.ui.swing.view.MergerView;

/**
 *
 * @author mpm1
 */
public class MergerControllerSwing implements MergerController {
    private Console console = new Console();
    private MergerView view;
    private DefaultListModel currentMerge = new DefaultListModel();
    
    public MergerControllerSwing(){
        view = new MergerView(console, currentMerge);
        view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        System.setOut(new PrintStream(console));
        
        view.setVisible(true);
    }
    
    @Override
    public synchronized void addMerge(MultipleMatchModel match){
        currentMerge.addElement(match);
    }

    public void setTotalEntities(int total) {
        // TODO: Nothing at the moment.
    }

    public void incrementCurrentEntities() {
        // TODO: Nothing at the moment.
    }
}
