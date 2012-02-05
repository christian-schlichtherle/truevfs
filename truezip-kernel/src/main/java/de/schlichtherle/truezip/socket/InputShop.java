/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * @param   <E> The type of the entries.
 * @see     OutputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@CleanupObligation
public interface InputShop<E extends Entry> extends Closeable, InputService<E> {

    @Override
    @DischargesObligation
    void close() throws IOException;
}
