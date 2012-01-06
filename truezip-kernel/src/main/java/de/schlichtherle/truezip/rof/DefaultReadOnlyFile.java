/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import net.jcip.annotations.NotThreadSafe;

/**
 * A {@link ReadOnlyFile} implementation derived from {@link RandomAccessFile}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class DefaultReadOnlyFile
extends RandomAccessFile
implements ReadOnlyFile {

    public DefaultReadOnlyFile(File file) throws FileNotFoundException {
        super(file, "r");
    }
}
