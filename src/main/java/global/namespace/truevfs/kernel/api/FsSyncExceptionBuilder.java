/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.comp.util.PriorityExceptionBuilder;

import java.util.Comparator;

/**
 * Assembles an {@link FsSyncException} from one or more sync exceptions by
 * {@linkplain Exception#addSuppressed(Throwable) suppressing} and optionally
 * {@linkplain FsSyncException#getPriority() prioritizing} them.
 *
 * @author Christian Schlichtherle
 */
public final class FsSyncExceptionBuilder
extends PriorityExceptionBuilder<FsSyncException> {

    private static final Comparator<FsSyncException> comp = Comparator.comparingInt(FsSyncException::getPriority);

    public FsSyncExceptionBuilder() {
        super(comp);
    }
}
