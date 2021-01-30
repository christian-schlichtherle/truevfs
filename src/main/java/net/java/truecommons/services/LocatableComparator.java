/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.services;

import java.util.Comparator;
import javax.annotation.concurrent.Immutable;

/**
 * Compares {@link LocatableService}s.
 *
 * @author Christian Schlichtherle
 */
@Immutable
class LocatableComparator implements Comparator<LocatableService> {
    @Override public int compare(final LocatableService o1, final LocatableService o2) {
        final int p1 = o1.getPriority();
        final int p2 = o2.getPriority();
        return p1 < p2 ? -1 : p1 == p2 ? 0 : 1;
    }
}
