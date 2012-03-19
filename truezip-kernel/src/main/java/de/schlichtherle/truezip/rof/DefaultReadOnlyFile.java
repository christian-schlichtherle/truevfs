/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A {@link ReadOnlyFile} implementation derived from {@link RandomAccessFile}.
 *
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
public class DefaultReadOnlyFile
extends RandomAccessFile
implements ReadOnlyFile {

    @CreatesObligation
    public DefaultReadOnlyFile(File file) throws FileNotFoundException {
        super(file, "r");
    }
}