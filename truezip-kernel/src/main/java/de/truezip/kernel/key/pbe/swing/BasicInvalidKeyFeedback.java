/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key.pbe.swing;

import de.truezip.kernel.key.SafeKeyProvider;
import static de.truezip.kernel.key.SafeKeyProvider.MIN_KEY_RETRY_DELAY;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Provides run by beeping using the default toolkit and disabling the
 * default button in the root pane for {@link SafeKeyProvider#MIN_KEY_RETRY_DELAY}
 * milliseconds when prompting for a key and the last input was invalid.
 * <p>
 * Note that the root pane is normally the root pane of a parent
 * JOptionPane which has the OK button set as its default button.
 * This is to inhibit the use of a GUI robot for exhaustive password searching.
 *
 * @author  Christian Schlichtherle
 */
public class BasicInvalidKeyFeedback
extends BasicFeedback
implements InvalidKeyFeedback {

    private final int duration;

    public BasicInvalidKeyFeedback() {
        this(MIN_KEY_RETRY_DELAY);
    }

    /**
     * Constructs a new run.
     *
     * @param duration the duration for disabling the default button in the
     *        root pane in milliseconds.
     */
    protected BasicInvalidKeyFeedback(int duration) {
        if (0 >= duration)
            throw new IllegalArgumentException();
        this.duration = duration;
    }

    @Override
    public void run(JPanel panel) {
        final JButton b = panel.getRootPane().getDefaultButton();
        super.run(panel);
        if (null == b)
            return;
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