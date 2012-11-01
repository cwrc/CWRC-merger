package org.ualberta.arc.mergecwrc.ui.swing.view;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import org.apache.commons.lang3.StringUtils;
import org.ualberta.arc.mergecwrc.merger.QueryResult;
import org.ualberta.arc.mergecwrc.ui.MultipleMatchModel;
import org.ualberta.arc.mergecwrc.utils.ScoringUtil.SectionDiff;

/**
 * A view used to display merging information.
 * @author mmckella
 */
public class MergerView extends JFrame {

    private Console consoleStream;
    private JTextPane inputNode, mergeNode;
    private JTextArea console;
    private JList currentMerges, possibleMatches;
    private DefaultListModel currentListModel;
    private JButton mergeButton, newButton;
    private MultipleMatchModel currentMatch = null;

    public MergerView(Console consoleStream, DefaultListModel currentModel) {
        super("CWRC Data Merger");

        this.setSize(1024, 768);
        this.consoleStream = consoleStream;

        this.currentListModel = currentModel;

        //Add the console output
        console = new JTextArea();
        consoleStream.setOutput(console);

        Container container = this.getContentPane();
        container.setLayout(new GridLayout(0, 1));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(0, 3));

        topPanel.add(new JScrollPane(console));

        //Add the Selectors
        currentMerges = new JList(currentModel);
        topPanel.add(new JScrollPane(currentMerges));

        currentMerges.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent lse) {
                if (lse.getFirstIndex() >= currentListModel.getSize()) {
                    inputNode.setText("");
                    return;
                }

                currentMatch = (MultipleMatchModel) currentListModel.getElementAt(lse.getFirstIndex());
                inputNode.setText("");

                selectPossibleMatches(currentMatch);
            }
        });

        JPanel mergePanel = new JPanel();
        mergePanel.setLayout(new BoxLayout(mergePanel, BoxLayout.Y_AXIS));

        possibleMatches = new JList();
        mergePanel.add(new JScrollPane(possibleMatches));

        possibleMatches.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent lse) {
                if (currentMatch != null) {
                    QueryResult result = (QueryResult) possibleMatches.getSelectedValue();

                    if (result == null) {
                        mergeNode.setText("");
                        return;
                    }

                    //Create the text for both sections                
                    inputNode.setText(StringUtils.EMPTY);
                    mergeNode.setText(StringUtils.EMPTY);

                    for (SectionDiff diff : currentMatch.getPossibleMatchDifference(result)) {
                        if (diff.isDifference()) {
                            append(inputNode, Color.RED, diff.getOldStr());
                            append(mergeNode, Color.GREEN, diff.getNewStr());
                        } else {
                            append(inputNode, Color.WHITE, diff.getOldStr());
                            append(mergeNode, Color.WHITE, diff.getNewStr());
                        }
                    }
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setMaximumSize(new Dimension(5000, 64));

        buttonPanel.add(new JPanel());

        mergeButton = new JButton("Merge");
        buttonPanel.add(mergeButton);
        mergeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Container container = ((JButton) e.getSource()).getParent();
                int result = JOptionPane.showConfirmDialog(container, "Are you sure you wish to merge these elements?", "Confirm Merge", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    int selectedIndex = currentMerges.getSelectedIndex();
                    MultipleMatchModel matches = (MultipleMatchModel) currentListModel.getElementAt(selectedIndex);

                    matches.setSelection((QueryResult) possibleMatches.getSelectedValue());

                    currentMatch = null;
                    currentListModel.removeElementAt(selectedIndex);
                    possibleMatches.setListData(new Object[]{});
                }
            }
        });

        newButton = new JButton("New");
        buttonPanel.add(newButton);
        newButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Container container = ((JButton) e.getSource()).getParent();
                int result = JOptionPane.showConfirmDialog(container, "Are you sure you wish to create a new entry to this element?", "Confirm New Entry", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    int selectedIndex = currentMerges.getSelectedIndex();
                    MultipleMatchModel matches = (MultipleMatchModel) currentListModel.get(selectedIndex);

                    matches.setSelection(null);

                    currentMatch = null;
                    currentListModel.remove(selectedIndex);
                    possibleMatches.setListData(new Object[]{});
                }
            }
        });

        mergePanel.add(buttonPanel);
        topPanel.add(mergePanel);

        container.add(topPanel);

        //Add text output
        JPanel selectors = new JPanel();
        selectors.setLayout(new GridLayout(0, 2));

        inputNode = new JTextPane();
        selectors.add(new JScrollPane(inputNode));

        mergeNode = new JTextPane();
        selectors.add(new JScrollPane(mergeNode));

        container.add(selectors);
    }

    private void selectPossibleMatches(MultipleMatchModel model) {
        possibleMatches.setListData(model.getPossibleMatches().toArray());
    }

    private void append(JTextPane area, Color color, String text) {
        try {
            StyleContext context = StyleContext.getDefaultStyleContext();
            AttributeSet attributes = context.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Background, color);

            StyledDocument doc = area.getStyledDocument();
            doc.insertString(doc.getLength(), text, attributes);
        } catch (BadLocationException ex) {
            System.err.println("Bad location selected for appending.");
        }
    }
}
