/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymgr.swing.feedback;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import net.truevfs.keymgr.spec.SafeKeyProvider;
import static net.truevfs.keymgr.spec.SafeKeyProvider.MIN_KEY_RETRY_DELAY;

/**
 * Provides feedback by beeping using the default toolkit and disabling the
 * default button in the root pane for {@link SafeKeyProvider#MIN_KEY_RETRY_DELAY}
 * milliseconds when prompting for a key and the last input was invalid.
 * <p>
 * Note that the root pane is normally the root pane of a parent
 * {@link JOptionPane} which has the OK button set as its default button.
 * This is to inhibit the use of a GUI robot for exhaustive password searching.
 *
 * @author Christian Schlichtherle
 */
public class BasicInvalidKeyFeedback
extends BasicFeedback
implements InvalidKeyFeedback {

    private final int duration;

    public BasicInvalidKeyFeedback() {
        this(MIN_KEY_RETRY_DELAY);
    }

    /**
     * Constructs a new feedback.
     *
     * @param duration the duration for disabling the default button in the
     *        root pane in milliseconds.
     * @throws IllegalArgumentException if the duration is not positive.
     */
    protected BasicInvalidKeyFeedback(int duration) {
        if (0 >= (this.duration = duration))
            throw new IllegalArgumentException();
    }

    @Override
    public void run(JPanel panel) {
        super.run(panel);
        final JButton b = panel.getRootPane().getDefaultButton();
        if (null == b) return;
        b.setEnabled(false);
        final Timer t = new Timer(duration, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b.setEnabled(true);
            }
        });
        t.setRepeats(false);
        t.start();
    }
}
