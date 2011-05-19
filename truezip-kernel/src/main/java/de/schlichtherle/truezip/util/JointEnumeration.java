/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.util;

import java.util.*;
import net.jcip.annotations.NotThreadSafe;

/**
 * Concatenates two enumerations.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class JointEnumeration<E> implements Enumeration<E> {
    private Enumeration<? extends E> e1;
    private final Enumeration<? extends E> e2;

    public JointEnumeration(
            final Enumeration<? extends E> e1,
            final Enumeration<? extends E> e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    @Override
	public boolean hasMoreElements() {
        return e1.hasMoreElements()
           || (e1 != e2 && (e1 = e2).hasMoreElements());
    }

    @Override
	public E nextElement() {
        try {
            return e1.nextElement();
        } catch (NoSuchElementException ex) {
            if (e1 == e2)
                throw ex;
            return (e1 = e2).nextElement();
        }
    }
}
