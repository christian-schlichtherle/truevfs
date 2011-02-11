/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import java.util.LinkedHashSet;
import net.jcip.annotations.ThreadSafe;

/**
 * An observable file system model.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsObservableModel extends FsDecoratingModel<FsModel> {

    private Set<FsTouchedListener> touchedListeners
            = new LinkedHashSet<FsTouchedListener>();

    public FsObservableModel(FsModel model) {
        super(model);
    }

    @Override
    public final void setTouched(final boolean newTouched) {
        final boolean oldTouched = delegate.isTouched();
        delegate.setTouched(newTouched);
        if (newTouched != oldTouched) {
            FsEvent event = new FsEvent(this);
            for (FsTouchedListener listener : getFsTouchedListeners())
                listener.touchedChanged(event);
        }
    }

    /**
     * Returns a protective copy of the set of file system touched listeners.
     *
     * @return A clone of the set of file system touched listeners.
     */
    final synchronized Set<FsTouchedListener> getFsTouchedListeners() {
        return new LinkedHashSet<FsTouchedListener>(touchedListeners);
    }

    /**
     * Adds the given listener to the set of file system touched listeners.
     *
     * @param listener the listener for file system touched events.
     */
    public final synchronized void addFsTouchedListener(
            FsTouchedListener listener) {
        if (null == listener)
            throw new NullPointerException();
        touchedListeners.add(listener);
    }

    /**
     * Removes the given listener from the set of file system touched listeners.
     *
     * @param listener the listener for file system touched events.
     */
    public final synchronized void removeFsTouchedListener(
            @CheckForNull FsTouchedListener listener) {
        touchedListeners.remove(listener);
    }
}
