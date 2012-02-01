/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
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
