/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

import javax.swing.JPanel;

/**
 * Provides visual and/or audible run to the user when prompting
 * for a key in its {@link #run} method.
 * <p>
 * Note that the {@link #run} method of this class is called when the
 * panel is just showing. This implies that the panel is fully initialized and
 * the implementation of this interface is not expected to do anything in
 * particular.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface Feedback {

    /**
     * Starts the visual/audible run.
     * This method is called when the panel is shown in its containing window.
     * It is run on AWT's Event Dispatch Thread, so it must complete fast
     * in order not to block the GUI.
     * If an implementation is going to do animations, the
     * {@link javax.swing.Timer} class should be used to schedule timer events
     * for the animation.
     *
     * @param panel the panel to provide visual/audible run to.
     */
    void run(JPanel panel);
}
