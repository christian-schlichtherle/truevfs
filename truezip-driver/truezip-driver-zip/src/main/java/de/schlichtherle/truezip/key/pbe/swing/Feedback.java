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

import edu.umd.cs.findbugs.annotations.NonNull;
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
 * @author Christian Schlichtherle
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
    void run(@NonNull JPanel panel);
}
