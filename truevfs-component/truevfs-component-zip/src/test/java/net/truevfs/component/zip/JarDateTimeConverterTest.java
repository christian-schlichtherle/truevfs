/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.component.zip;

import net.truevfs.component.zip.DateTimeConverter;

/**
 * @author Christian Schlichtherle
 */
public final class JarDateTimeConverterTest extends DateTimeConverterTestSuite {

    @Override
    DateTimeConverter getInstance() {
        return DateTimeConverter.JAR;
    }
}
