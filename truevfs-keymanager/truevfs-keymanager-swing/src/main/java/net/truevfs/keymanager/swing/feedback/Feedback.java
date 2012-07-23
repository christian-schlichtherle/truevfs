/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymanager.swing.feedback;

import javax.swing.JPanel;

/**
 * Provides visual and/or audible feedback to the user when prompting
 * for a key in its {@link #run} method.
 * <p>
 * Note that the {@link #run} method of this class is called when the
 * panel is just showing. This implies that the panel is fully initialized and
 * the implementation of this interface is not expected to do anything in
 * particular.
 *
 * @author Christian Schlichtherle
 */
public interface Feedback {

    /**
     * Starts the visual/audible feedback.
     * This method is called when the panel is shown in its containing window.
     * It is run on AWT's Event Dispatch Thread, so it must complete fast
     * in order not to block the GUI.
     * If an implementation is going to do animations, the
     * {@link javax.swing.Timer} class should be used to schedule timer events
     * for the animation.
     *
     * @param panel the panel to provide visual/audible feedback to.
     */
    void run(JPanel panel);
}
