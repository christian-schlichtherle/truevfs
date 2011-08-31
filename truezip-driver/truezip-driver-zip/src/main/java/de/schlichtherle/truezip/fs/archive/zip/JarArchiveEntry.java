/*
 * Copyright (C) 2009-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.zip.DateTimeConverter;
import de.schlichtherle.truezip.zip.ZipEntry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Respects the different date/time conversion in JAR files.
 *
 * @see JarDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class JarArchiveEntry extends ZipArchiveEntry {

    public JarArchiveEntry(String name) {
        super(name);
    }

    protected JarArchiveEntry(String name, ZipEntry template) {
        super(name, template);
    }

    @Override
    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.JAR;
    }
}
