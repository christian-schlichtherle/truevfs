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

import java.awt.Container;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.JOptionPane;

/**
 * A utility class for window management.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class Windows {
    private static final String PROPERTY_FOCUSED_WINDOW = "focusedWindow";

    private static Reference<KeyboardFocusManager> lastFocusManager
            = new WeakReference<KeyboardFocusManager>(null);
    private static Reference<Window> lastFocusedWindow
            = new WeakReference<Window>(null);

    private static PropertyChangeListener focusListener
            = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            Window w = (Window) evt.getNewValue();
            if (w != null)
                lastFocusedWindow = new WeakReference<Window>(w);
        }
    };

    /** You cannot instantiate this class. */
    Windows() {
    }

    /** @see PromptingKeyManager#getParentWindow */
    public static synchronized Window getParentWindow() {
        Window w = getLastFocusedWindow();
        if (w == null)
            w = getAnyShowingWindow();

        // Search the containment hierarchy updwards for the first showing
        // window.
        for (Container c = w; c != null; c = c.getParent())
            if (c instanceof Window && c.isShowing())
                return (Window) c;

        // No window is showing, use JOptionPane's default.
        return JOptionPane.getRootFrame();
    }

    /**
     * Returns the last window which received the focus.
     * If no window received the focus yet or is already made eligible for
     * finalization, {@code null} is returned instead.
     * Note that this is <em>not</em> the same as
     * {@code Windows.getCurrentKeyboardFocusManager().getFocusedWindow()}:
     * The latter may return {@code null} if no window in this JVM has
     * the focus, while this method will return the last window in this JVM
     * which had the focus (unless this is also the first call to this method).
     */
    public static Window getLastFocusedWindow() {
        observeFocusedWindow();
        return lastFocusedWindow.get();
    }

    /**
     * Ensures that the focused window managed by the current keyboard focus
     * manager is observed.
     */
    private static synchronized void observeFocusedWindow() {
        final KeyboardFocusManager lfm = lastFocusManager.get();
        final KeyboardFocusManager fm
                = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (fm == lfm)
            return;

        if (lfm != null)
            lfm.removePropertyChangeListener(PROPERTY_FOCUSED_WINDOW,
                    focusListener);
        fm.addPropertyChangeListener(PROPERTY_FOCUSED_WINDOW,
                focusListener);
        lastFocusManager = new WeakReference<KeyboardFocusManager>(fm);
        lastFocusedWindow = new WeakReference<Window>(fm.getFocusedWindow());
    }

    private static Window getAnyShowingWindow() {
        return getAnyShowingWindow(Frame.getFrames());
    }

    private static Window getAnyShowingWindow(final Window[] windows) {
        for (int i = 0, l = windows.length; i < l; i++) {
            Window window = windows[i];
            if (window.isShowing())
                return window;

            window = getAnyShowingWindow(window.getOwnedWindows());
            if (window != null)
                return window;
        }

        return null;
    }
}
