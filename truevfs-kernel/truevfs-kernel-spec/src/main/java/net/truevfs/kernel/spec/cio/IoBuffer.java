/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.cio;

import java.io.IOException;
import net.truevfs.kernel.spec.util.Releasable;

/**
 * A releasable I/O entry.
 *
 * @param  <This> the type of this I/O buffer.
 * @author Christian Schlichtherle
 */
public interface IoBuffer<This extends IoBuffer<This>>
extends Releasable<IOException>, IoEntry<This> {
}
