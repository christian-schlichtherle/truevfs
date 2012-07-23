/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.jar;

import net.truevfs.component.zip.driver.AbstractZipDriverEntry;
import net.truevfs.component.zip.DateTimeConverter;
import net.truevfs.component.zip.ZipEntry;

/**
 * JAR archive entries apply the date/time conversion rules as defined by
 * {@link DateTimeConverter#JAR}.
 *
 * @see    #getDateTimeConverter()
 * @see    JarDriver
 * @author Christian Schlichtherle
 */
public class JarDriverEntry extends AbstractZipDriverEntry {

    public JarDriverEntry(String name) {
        super(name);
    }

    protected JarDriverEntry(String name, ZipEntry template) {
        super(name, template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link JarDriverEntry} returns
     * {@link DateTimeConverter#JAR}.
     *
     * @return {@link DateTimeConverter#JAR}
     */
    @Override
    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.JAR;
    }
}
