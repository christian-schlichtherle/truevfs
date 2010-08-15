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

package de.schlichtherle.swing.event;

import de.schlichtherle.swing.EnhancedPanel;

import java.awt.AWTEvent;
import java.awt.Window;

/**
 * Fired when the ancestor window of an {@link EnhancedPanel} is shown or
 * hidden.
 * <p>
 * Note that since TrueZIP 6.1, this class has been refactored to subclass
 * {@link AWTEvent} (which subclasses {@link java.util.EventObject}) instead
 * of <code>EventObject</code> directly.
 * This has been done in order to allow coalescing multiple events for the
 * same cause by posting them to the AWT's Event Queue, from which the
 * coalesced event would then be dispatched by AWT's Event Dispatching Thread.
 * <p>
 * However, since TrueZIP 6.4, these events are fired <em>synchronously</em>
 * again, whereby it is ensured that only a single event is fired for each
 * cause. The super class is kept for backwards compatibility only.
 *
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 5.1
 */
public class PanelEvent extends AWTEvent {

    private static final long serialVersionUID = -7614118389065035365L;

    /** The id for Ancestor Window Shown Event. */
    public static final int ANCESTOR_WINDOW_SHOWN  = RESERVED_ID_MAX + 1;

    /** The id for Ancestor Window Hidden Event. */
    public static final int ANCESTOR_WINDOW_HIDDEN = RESERVED_ID_MAX + 2;

    /**
     */
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
     * Returns the source of this event as an <code>EnhancedPanel</code>
     * or <code>null</code> if it's not an instance of this class.
     */
    public EnhancedPanel getSourcePanel() {
        return source instanceof EnhancedPanel ? (EnhancedPanel) source : null;
    }

    /**
     * Returns the ancestor window for this event or <code>null</code> if the
     * source of this event is not an <code>EnhancedPanel</code>.
     */
    public Window getAncestorWindow() {
        EnhancedPanel panel = getSourcePanel();
        return panel != null ? panel.getAncestorWindow() : null;
    }

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
