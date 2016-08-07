/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truevfs.comp.zip.DateTimeConverter;

/**
 * @author Christian Schlichtherle
 */
public final class ZipDateTimeConverterTest extends DateTimeConverterTestSuite {
    @Override
    DateTimeConverter getInstance() {
        return DateTimeConverter.ZIP;
    }
}
