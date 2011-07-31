/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.key.pbe.swing;

import de.schlichtherle.truezip.key.SafeKeyProvider;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

import static de.schlichtherle.truezip.key.SafeKeyProvider.*;

/**
 * Provides run by beeping using the default toolkit and disabling the
 * default button in the root pane for {@link SafeKeyProvider#MIN_KEY_RETRY_DELAY}
 * milliseconds when prompting for a key and the last input was invalid.
 * <p>
 * Note that the root pane is normally the root pane of a parent
 * JOptionPane which has the OK button set as its default button.
 * This is to inhibit the use of a GUI robot for exhaustive password searching.
 *
 * @author Christian Schlichtherle
 * @version $Id$
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
