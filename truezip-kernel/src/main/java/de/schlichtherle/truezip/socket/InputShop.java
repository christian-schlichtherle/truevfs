/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.Closeable;
import java.io.IOException;

/**
 * A closable input service.
 *
 * @param  <E> the type of the entries.
 * @see    OutputShop
 * @author Christian Schlichtherle
 */
@CleanupObligation
public interface InputShop<E extends Entry> extends Closeable, InputService<E> {

    @Override
    @DischargesObligation
    void close() throws IOException;
}
