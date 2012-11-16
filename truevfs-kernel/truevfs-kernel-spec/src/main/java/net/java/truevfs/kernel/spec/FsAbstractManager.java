/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.UniqueObject;
import net.java.truecommons.shed.Visitor;

/**
 * An abstract file system manager.
 * <p>
 * Subclasses should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsAbstractManager
extends UniqueObject implements FsManager {

    @Override
    public void sync(
            final Filter<? super FsController> filter,
            final Visitor<? super FsController, FsSyncException> visitor)
    throws FsSyncException {
        final FsSyncExceptionBuilder b = new FsSyncExceptionBuilder();

        class AssembleExceptionVisitor
        implements Visitor<FsController, FsSyncException> {
            @Override
            public void visit(final FsController c) {
                try { visitor.visit(c); }
                catch (final FsSyncException ex) { b.warn(ex); }
            }
        } // AssembleExceptionVisitor

        visit(filter, new AssembleExceptionVisitor());
        b.check();
    }
}
