/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.swing;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.awt.AWTEvent;

/**
 * Fired when the ancestor window of an {@link EnhancedPanel} is shown or
 * hidden.
 * <p>
 * Note that since TrueZIP 6.1, this class has been refactored to subclass
 * {@link AWTEvent} (which subclasses {@link java.util.EventObject}) instead
 * of {@code EventObject} directly.
 * This has been done in order to allow coalescing multiple events for the
 * same cause by posting them to the AWT's Event Queue, from which the
 * coalesced event would then be dispatched by AWT's Event Dispatching Thread.
 * <p>
 * However, since TrueZIP 6.4, these events are fired <em>synchronously</em>
 * again, whereby it is ensured that only a single event is fired for each
 * cause. The super class is kept for backwards compatibility only.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class PanelEvent extends AWTEvent {

    private static final long serialVersionUID = -7614118389065035365L;

    /** The id for Ancestor Window Shown Event. */
    public static final int ANCESTOR_WINDOW_SHOWN  = RESERVED_ID_MAX + 1;

    /** The id for Ancestor Window Hidden Event. */
    public static final int ANCESTOR_WINDOW_HIDDEN = RESERVED_ID_MAX + 2;

    public PanelEvent(EnhancedPanel source, int id) {
        super(source, id);

        switch (id) {
            case ANCESTOR_WINDOW_SHOWN:
            case ANCESTOR_WINDOW_HIDDEN:
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link  PanelEvent} always returns non-{@code null}.
     */
    @Override
    public EnhancedPanel getSource() {
        return (EnhancedPanel) source;
    }

    /**
     * {@inheritDoc}
     * <p>
     * @throws ClassCastException If {@code source} is not an instance of
     *         {@link EnhancedPanel}.
     */
    @Override
    public void setSource(Object source) {
        if (null == source)
            throw new NullPointerException();
        super.setSource((EnhancedPanel) source);
    }

    @Override
    public String paramString() {
        switch (id) {
            case ANCESTOR_WINDOW_SHOWN:
                return "id=ANCESTOR_WINDOW_SHOWN";

            case ANCESTOR_WINDOW_HIDDEN:
                return "id=ANCESTOR_WINDOW_HIDDEN";

            default:
                throw new AssertionError();
        }
    }
}
