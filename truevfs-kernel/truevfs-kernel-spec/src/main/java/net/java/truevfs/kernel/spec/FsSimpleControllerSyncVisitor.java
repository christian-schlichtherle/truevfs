/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.Filter;

/**
 * A visitor which uses the options and the filter provided to its constructor
 * for performing a {@link FsController#sync} operation.
 * <p>
 * Subclasses should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class FsSimpleControllerSyncVisitor extends FsControllerSyncVisitor {
    final Filter<? super FsController> filter;
    final BitField<FsSyncOption> options;

    public FsSimpleControllerSyncVisitor(BitField<FsSyncOption> options) {
        this(options, Filter.ACCEPT_ANY);
    }

    public FsSimpleControllerSyncVisitor(
            final BitField<FsSyncOption> options,
            final Filter<? super FsController> filter) {
        this.filter = Objects.requireNonNull(filter);
        this.options = Objects.requireNonNull(options);
    }

    @Override
    public Filter<? super FsController> filter() { return filter; }

    @Override
    public BitField<FsSyncOption> options(FsController controller) {
        return options;
    }
}
