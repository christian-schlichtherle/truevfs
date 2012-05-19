/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.swing;

import javax.annotation.Nullable;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Objects;
import javax.swing.JPanel;

/**
 * Provides methods to fire {@link PanelEvent}s.
 * 
 * @author  Christian Schlichtherle
 */
public class EnhancedPanel extends JPanel  {

    private static final long serialVersionUID = 6984576810262891640L;

    /**
     * Creates a new {@code EnhancedPanel} with the specified layout
     * manager and buffering strategy.
     *
     * @param layout The {@link LayoutManager} to use.
     * @param isDoubleBuffered A boolean, true for double-buffering, which
     *        uses additional memory space to achieve fast, flicker-free 
     *        updates.
     */
    public EnhancedPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        super.addHierarchyListener(new EnhancedPanelHierarchyListener());
    }

    /**
     * Create a new buffered {@code EnhancedPanel} with the specified
     * layout manager.
     *
     * @param layout The {@link LayoutManager} to use.
     */
    public EnhancedPanel(LayoutManager layout) {
        super(layout);
        super.addHierarchyListener(new EnhancedPanelHierarchyListener());
    }

    /**
     * Creates a new {@code EnhancedPanel} with {@code FlowLayout}
     * and the specified buffering strategy.
     * If {@code isDoubleBuffered} is true, the {@code EnhancedPanel}
     * will use a double buffer.
     *
     * @param isDoubleBuffered A boolean, true for double-buffering, which
     *        uses additional memory space to achieve fast, flicker-free 
     *        updates.
     */
    public EnhancedPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        super.addHierarchyListener(new EnhancedPanelHierarchyListener());
    }

    /**
     * Creates a new {@code EnhancedPanel} with a double buffer
     * and a flow layout.
     */
    public EnhancedPanel() {
        super.addHierarchyListener(new EnhancedPanelHierarchyListener());
    }

    /**
     * Calls {@link #fireAncestorWindowShown} or
     * {@link #fireAncestorWindowHidden}, depending on the ID of the given
     * {@code event}.
     */
    protected void processPanelEvent(final PanelEvent event) {
        switch (event.getID()) {
            case PanelEvent.ANCESTOR_WINDOW_SHOWN:
                fireAncestorWindowShown(event);
                break;

            case PanelEvent.ANCESTOR_WINDOW_HIDDEN:
                fireAncestorWindowHidden(event);
                break;

            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the ancestor {@link Window} of this {@code Panel} or
     * {@code null} if the component is not (yet) placed in a
     * {@code Window}.
     */
    public @Nullable Window getAncestorWindow() {
        return getAncestorWindow(this);
    }

    private static @Nullable Window getAncestorWindow(Component c) {
        while (null != c && !(c instanceof Window))
            c = c.getParent();

        return (Window) c;
    }

    /**
     * Adds the {@code listener} to the list of receivers for
     * {@link PanelEvent}s.
     * <p>
     * Note that the listener doesn't get serialized with this component!
     *
     * @param listener The listener to add.
     *        If this method is called {@code n} times with the same
     *        listener, any events generated will be delivered to this
     *        listener {@code n} times.
     */
    public void addPanelListener(final PanelListener listener) {
        listenerList.add(PanelListener.class, Objects.requireNonNull(listener));
    }

    /**
     * Removes the {@code listener} from the list of receivers for
     * {@link PanelEvent}s.
     *
     * @param listener The listener to remove.
     *        If this listener has been {@link #addPanelListener added}
     *        multiple times, it is removed from the list only once.
     */
    public void removePanelListener(final PanelListener listener) {
        listenerList.remove(PanelListener.class, Objects.requireNonNull(listener));
    }

    /**
     * Returns an array of all the panel listeners
     * registered on this component.
     *
     * @return All of this panel's {@code PanelListener}s or an empty
     *         array if no panel listeners are currently registered.
     *
     * @see #addPanelListener
     * @see #removePanelListener
     */
    public PanelListener[] getPanelListeners() {
        return getListeners(PanelListener.class);
    }

    /**
     * Notifies all registered listeners about the event.
     * This is a synchronous operation.
     * 
     * @param event The event to be fired.
     */
    void fireAncestorWindowShown(final PanelEvent event) {
        final Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2)
            if (listeners[i] == PanelListener.class)
                ((PanelListener) listeners[i+1]).ancestorWindowShown(event);
    }

    /**
     * Notifies all registered listeners about the event.
     * This is a synchronous operation.
     * 
     * @param event The event to be fired.
     */
    void fireAncestorWindowHidden(final PanelEvent event) {
        final Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2)
            if (listeners[i] == PanelListener.class)
                ((PanelListener) listeners[i+1]).ancestorWindowHidden(event);
    }

    private final class EnhancedPanelHierarchyListener
    implements HierarchyListener {
        @Override
        public void hierarchyChanged(final HierarchyEvent e) {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED)
                    != HierarchyEvent.SHOWING_CHANGED)
                return;

            final Window window = getAncestorWindow();
            assert null != window : "A showing panel must have a containing window!";
            final boolean windowShown = window.isShowing();
            if (windowShown != isShowing())
                return;

            processPanelEvent(new PanelEvent(EnhancedPanel.this,
                    windowShown
                        ? PanelEvent.ANCESTOR_WINDOW_SHOWN
                        : PanelEvent.ANCESTOR_WINDOW_HIDDEN));
        }
    } // class EnhancedPanelHierarchyListener
}