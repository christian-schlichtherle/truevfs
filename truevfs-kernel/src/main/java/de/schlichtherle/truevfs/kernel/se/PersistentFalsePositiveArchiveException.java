/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates that a file system is a false positive file system and that this
 * exception may get cached until the federated (archive) file system gets
 * {@linkplain FsController#sync(net.truevfs.kernel.util.BitField) synced}
 * again.
 * <p>
 * This exception type is reserved for non-local control flow in
 * {@linkplain FsDecoratingController file system controller chains} in order
 * to reroute file system operations to the parent file system of a false
 * positive federated (archive) file system.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing a control flow exception is nonsense!
final class PersistentFalsePositiveArchiveException extends FalsePositiveArchiveException {

    PersistentFalsePositiveArchiveException(IOException cause) {
        super(cause);
    }
}
