package org.ualberta.arc.mergecwrc.ui.swing.view;

import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * A custom Swing Console used to display output data on a JTextArea.
 * 
 * @author mpm1
 */
public class Console extends OutputStream {
    private JTextArea output = null;

    public void setOutput(JTextArea output) {
        this.output = output;
    }

    @Override
    public void write(final int b) {
        updateTextArea(String.valueOf((char) b));
    }

    @Override
    public void write(byte[] b, int off, int len) {
        updateTextArea(new String(b, off, len));
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(new UpdateTextArea(text));
    }

    private class UpdateTextArea implements Runnable {

        private String text = null;

        public UpdateTextArea(final String text) {
            this.text = text;
        }

        public void run() {
            Document doc = output.getDocument();
            try {
                doc.insertString(doc.getLength(), text, null);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }

            output.setCaretPosition(doc.getLength() - 1);
        }
    }
}
