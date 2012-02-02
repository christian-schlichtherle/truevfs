/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.swing;

import java.util.EventListener;

/**
 * Used to notify listeners of an {@link de.schlichtherle.truezip.swing.EnhancedPanel}
 * that its ancestor window is shown or hidden.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface PanelListener extends EventListener {
    
    /**
     * Invoked when the ancestor window of an
     * {@link de.schlichtherle.truezip.swing.EnhancedPanel} is shown.
     */
    void ancestorWindowShown(PanelEvent evt);

    /**
     * Invoked when the ancestor window of an
     * {@link de.schlichtherle.truezip.swing.EnhancedPanel} is hidden.
     */
    void ancestorWindowHidden(PanelEvent evt);
}
