/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.zip.DateTimeConverter;
import de.schlichtherle.truezip.zip.ZipEntry;

/**
 * JAR archive entries apply the date/time conversion rules as defined by
 * {@link DateTimeConverter#JAR}.
 *
 * @see     #getDateTimeConverter()
 * @see     JarDriver
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class JarArchiveEntry extends ZipArchiveEntry {

    public JarArchiveEntry(String name) {
        super(name);
    }

    protected JarArchiveEntry(String name, ZipEntry template) {
        super(name, template);
    }

    /**
     * Returns a {@link DateTimeConverter} for the conversion of Java time
     * to DOS date/time fields and vice versa.
     * <p>
     * The implementation in the class {@link ZipArchiveEntry} returns
     * {@link DateTimeConverter#JAR}.
     *
     * @return {@link DateTimeConverter#JAR}
     */
    @Override
    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.JAR;
    }
}
