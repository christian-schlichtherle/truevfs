/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.util.PriorityExceptionBuilder;
import java.util.Comparator;
import javax.annotation.concurrent.NotThreadSafe;

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
            return o1.getPriority() - o2.getPriority();
        }
    }
}
