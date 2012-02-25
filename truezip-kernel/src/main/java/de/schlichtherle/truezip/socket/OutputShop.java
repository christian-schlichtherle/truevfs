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
 * A closable output service.
 * <p>
 * All methods of this interface must reflect all entries, including those
 * which have just been partially written yet, i.e. which have not already
 * received a call to their {@code close()} method.
 *
 * @param   <E> the type of the entries.
 * @see     InputShop
 * @author  Christian Schlichtherle
 */
// TODO: Consider renaming to OutputArchive
@CleanupObligation
public interface OutputShop<E extends Entry> extends Closeable, OutputService<E> {

    @Override
    @DischargesObligation
    void close() throws IOException;
}
