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

package de.schlichtherle.truezip.key.passwd.swing;

import java.awt.Toolkit;
import javax.swing.JPanel;

/**
 * Provides feedback by beeping using the default toolkit.
 * If you would like to play a nice sound for feedback, you need to override
 * the {@link #startSound} method.
 * <p>
 * <b>Warning:</b> Playing a {@code java.applet.AudioClip} on J2SE
 * 1.4.2_12 causes a client application not to terminate until System.exit(0)
 * is called explicitly - hence this feature is currently not implemented in
 * this class!
 * This issue is fixed in JSE 1.5.0_07 (and probably earlier versions).
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class BasicFeedback implements Feedback {

    private JPanel panel;

    public JPanel getPanel() {
        return panel;
    }

    public void setPanel(final JPanel panel) {
        this.panel = panel;
    }

    /**
     * Starts the visual/audible feedback.
     * Subclasses should not override this method directly, but override
     * {@link #startSound} and/or {@link #startAnimation} instead.
     * <p>
     * The implementation in this class calls {@link #startSound} and then
     * {@link #startAnimation}.
     */
    public void run() {
        startSound();
        startAnimation();
    }

    /**
     * Starts the audible feedback.
     * Subclasses may override this method to play a sound of their liking.
     * <p>
     */
    protected void startSound() {
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Starts the visual feedback.
     * Subclasses may override this method to animate the GUI according to
     * their liking.
     * This method is called when the panel is shown in its containing window.
     * It is run on AWT's Event Dispatch Thread, so it must complete fast
     * in order not to block the GUI.
     * If an implementation is going to do animations, the
     * {@link javax.swing.Timer} class should be used to schedule timer events
     * for the animation.
     * <p>
     * The implementation in this class is a no-op.
     */
    protected void startAnimation() {
    }
}
