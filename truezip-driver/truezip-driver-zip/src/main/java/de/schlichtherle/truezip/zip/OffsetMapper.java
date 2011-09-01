/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

/**
 * Maps a given offset to a file pointer position.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
class OffsetMapper {
    long map(long offset) {
        return offset;
    }

    long unmap(long offset) {
        return offset;
    }
}
