/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.util.Releasable;
import java.io.IOException;

/**
 * A releasable I/O entry.
 *
 * @param  <B> the type of this I/O buffer.
 * @author Christian Schlichtherle
 */
public interface IOBuffer<B extends IOBuffer<B>>
extends Releasable<IOException>, IOEntry<B> {
}