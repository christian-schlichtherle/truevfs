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
 * Adds a start value to the given offset.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class IrregularOffsetMapper extends OffsetMapper {
    final long start;

    IrregularOffsetMapper(final long start) {
        this.start = start;
    }

    @Override
    long map(long offset) {
        return offset + start;
    }

    @Override
    long unmap(long offset) {
        return offset - start;
    }
}
