/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.archive.FsArchiveEntry;
import de.schlichtherle.truezip.zip.ZipEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZipArchiveEntryTest {

    @Test
    @SuppressWarnings("all")
    public void testClassInvariants() {
        assertTrue(ZipEntry.UNKNOWN == FsArchiveEntry.UNKNOWN);
    }
}
