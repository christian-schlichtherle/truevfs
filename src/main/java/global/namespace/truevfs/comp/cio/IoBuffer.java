/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import java.io.IOException;
import global.namespace.truevfs.comp.shed.Releasable;

/**
 * A releasable I/O entry.
 * <p>
 * Implementations should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public interface IoBuffer extends Releasable<IOException>, IoEntry<IoBuffer> {
}
