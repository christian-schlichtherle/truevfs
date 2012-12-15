/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Comparator;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.shed.PriorityExceptionBuilder;

/**
 * Assembles an {@link FsSyncException} from one or more sync exceptions by
 * {@linkplain Exception#addSuppressed(Throwable) suppressing} and optionally
 * {@linkplain FsSyncException#getPriority() prioritizing} them.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class FsSyncExceptionBuilder
extends PriorityExceptionBuilder<FsSyncException> {

    public FsSyncExceptionBuilder() {
        super(FsSyncExceptionComparator.INSTANCE);
    }

    private static final class FsSyncExceptionComparator
    implements Comparator<FsSyncException> {
        static final FsSyncExceptionComparator
                INSTANCE = new FsSyncExceptionComparator();

        @Override
        public int compare(FsSyncException o1, FsSyncException o2) {
            final int p1 = o1.getPriority();
            final int p2 = o2.getPriority();
            return p1 < p2 ? -1 : p1 == p2 ? 0 : 1;
        }
    }
}
