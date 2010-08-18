/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.key.passwd.swing;

import de.schlichtherle.key.KeyProvider;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.Timer;

/**
 * Provides feedback by beeping using the default toolkit and disabling the
 * default button in the root pane for three seconds
 * when prompting for a key
 * and the last input was invalid.
 * <p>
 * Note that the root pane is normally the root pane of an enclosing
 * JOptionPane which has the OK button set as its default button.
 * This is to inhibit the use of a GUI robot for exhaustive password searching.
 * <p>
 * If you would like to play a nice sound for feedback, you need to override
 * the {@link #startSound} method.
 * <p>
 * <b>Warning:</b> Playing a <code>java.applet.AudioClip</code> on J2SE
 * 1.4.2_12 causes a client application not to terminate until System.exit(0)
 * is called explicitly - hence this feature is currently not implemented in
 * this class!
 * This issue is fixed in JSE 1.5.0_07 (and probably earlier versions).
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.4
 */
public abstract class BasicInvalidKeyFeedback extends BasicFeedback {

    private int delay = KeyProvider.MIN_KEY_RETRY_DELAY;

    /**
     * Returns the delay for reenabling the default button in the root pane.
     *
     * @return The delay in milliseconds.
     *         Defaults to {@link KeyProvider#MIN_KEY_RETRY_DELAY}.
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Sets the delay for reenabling the default button in the root pane.
     *
     * @param delay The delay in milliseconds.
     */
    public void setDelay(final int delay) {
        this.delay = delay;
    }

    @Override
    protected void startAnimation() {
        startAnimation(getPanel(), getDelay());
    }

    static void startAnimation(final JPanel panel, final int delay) {
        final JRootPane rp = panel.getRootPane();
        final JButton b = rp.getDefaultButton();
        if (b == null)
            return;

        b.setEnabled(false);

        final Timer t = new Timer(delay, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                b.setEnabled(true);
            }
        });
        t.setRepeats(false);
        t.start();
    }
}
