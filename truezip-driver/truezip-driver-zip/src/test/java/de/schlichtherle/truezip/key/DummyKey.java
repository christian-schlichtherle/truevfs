/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key;

import de.schlichtherle.truezip.key.SafeKey;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class DummyKey implements SafeKey<DummyKey>, Cloneable {

    private static volatile int count;

    private final int key = count++;
    boolean reset;

    @Override
    public DummyKey clone() {
        try {
            return (DummyKey) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void reset() {
        reset = true;
    }

    @Override
    public boolean equals(@CheckForNull Object that) {
        return that instanceof DummyKey && this.key == ((DummyKey) that).key;
    }

    @Override
    public int hashCode() {
        return key;
    }
}
