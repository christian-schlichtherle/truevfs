/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing.feedback;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import net.java.truecommons.key.spec.util.SuspensionPenalty;

/**
 * Provides feedback by disabling the default button of the root pane for
 * {@link SuspensionPenalty#MIN_KEY_RETRY_DELAY} milliseconds when prompting for
 * a key and the last input was invalid.
 * <p>
 * Note that the root pane is normally the root pane of a parent
 * {@link JOptionPane} which has the OK button set as its default button.
 * This is to inhibit the use of a GUI robot for exhaustive password searching.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
final class TemporarilyDisableDefaultButtonFeedback
extends DecoratingFeedback implements Feedback {

    TemporarilyDisableDefaultButtonFeedback(Feedback feedback) {
        super(feedback);
    }

    @Override
    public void run(JPanel panel) {
        feedback.run(panel);
        final JButton b = panel.getRootPane().getDefaultButton();
        if (null == b) return;
        b.setEnabled(false);
        final Timer t = new Timer(SuspensionPenalty.MIN_KEY_RETRY_DELAY,
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        b.setEnabled(true);
                    }
                });
        t.setRepeats(false);
        t.start();
    }
}
