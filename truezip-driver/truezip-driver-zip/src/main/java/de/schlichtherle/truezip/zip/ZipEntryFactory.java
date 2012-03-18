/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

/**
 * A factory for {@link ZipEntry}s.
 *
 * @param   <E> The type of the created ZIP entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ZipEntryFactory<E extends ZipEntry> extends ZipParameters {

    /**
     * Returns a new ZIP entry with the given {@code name}.
     *
     * @param  name the entry name.
     * @return A new ZIP entry with the given {@code name}.
     */
    E newEntry(String name);
}
