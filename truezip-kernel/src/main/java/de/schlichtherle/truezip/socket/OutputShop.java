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
import java.io.Closeable;

/**
 * A closable output service.
 * <p>
 * All methods of this interface must reflect all entries, including those
 * which have just been partially written yet, i.e. which have not already
 * received a call to their {@code close()} method.
 *
 * @param   <E> The type of the entries.
 * @see     InputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputShop<E extends Entry> extends Closeable, OutputService<E> {
}
