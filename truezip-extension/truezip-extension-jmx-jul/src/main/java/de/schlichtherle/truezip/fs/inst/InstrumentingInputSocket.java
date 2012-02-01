/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public abstract class InstrumentingInputSocket<E extends Entry>
extends DecoratingInputSocket<E> {

    protected final InstrumentingDirector director;

    protected InstrumentingInputSocket(
            final InputSocket<? extends E> delegate,
            final InstrumentingDirector director) {
        super(delegate);
        if (null == director)
            throw new NullPointerException();
        this.director = director;
    }
}
