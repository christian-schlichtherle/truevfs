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
package de.schlichtherle.truezip.swing;

import edu.umd.cs.findbugs.annotations.NonNull;
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
    void ancestorWindowShown(@NonNull PanelEvent evt);

    /**
     * Invoked when the ancestor window of an
     * {@link de.schlichtherle.truezip.swing.EnhancedPanel} is hidden.
     */
    void ancestorWindowHidden(@NonNull PanelEvent evt);
}
