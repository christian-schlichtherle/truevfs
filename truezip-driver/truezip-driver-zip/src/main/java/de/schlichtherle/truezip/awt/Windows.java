/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.awt;

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
 * This class cannot get instantiated.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Windows {

    private Windows() {
    }

    private static final String PROPERTY_FOCUSED_WINDOW = "focusedWindow";

    private static Reference<KeyboardFocusManager> lastFocusManager
            = new WeakReference<KeyboardFocusManager>(null);
    private static Reference<Window> lastFocusedWindow
            = new WeakReference<Window>(null);

    private static PropertyChangeListener focusListener
            = new PropertyChangeListener() {
        @Override
		public void propertyChange(PropertyChangeEvent evt) {
            setLastFocusedWindow((Window) evt.getNewValue());
        }
    };

    /**
     * Returns a suitable parent window, which is the last explicitly set
     * and still showing parent window or any of its showing parent windows,
     * the last focused and still showing window or any of its showing parent
     * windows or any other showing window  - whichever is found first.
     * If no showing window is found, {@link JOptionPane}'s root frame is
     * returned.
     */
    public static synchronized Window getParentWindow() {
        Window w;
        if ((w = findFirstShowingWindow(getLastFocusedWindow())) == null)
            w = getAnyShowingWindow();
        return w != null ? w : JOptionPane.getRootFrame();
    }

    /**
     * Search the containment hierarchy updwards for the first showing
     * window.
     */
    private static Window findFirstShowingWindow(final Window w) {
        for (Container c = w; c != null; c = c.getParent())
            if (c instanceof Window && c.isShowing())
                return (Window) c;
        return null;
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
    private static void observeFocusedWindow() {
        final KeyboardFocusManager lfm = lastFocusManager.get();
        final KeyboardFocusManager fm
                = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (fm == lfm)
            return;
        if (lfm != null)
            lfm.removePropertyChangeListener(
                    PROPERTY_FOCUSED_WINDOW, focusListener);
        fm.addPropertyChangeListener(
                PROPERTY_FOCUSED_WINDOW, focusListener);
        lastFocusManager = new WeakReference<KeyboardFocusManager>(fm);
        setLastFocusedWindow0(fm.getFocusedWindow());
    }

    private static synchronized void setLastFocusedWindow(Window w) {
        setLastFocusedWindow0(w);
    }

    private static void setLastFocusedWindow0(Window w) {
        if (w == null || !w.isShowing())
            if ((w = lastFocusedWindow.get()) != null && !w.isShowing())
                w = null;
        lastFocusedWindow = new WeakReference<Window>(w);
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
