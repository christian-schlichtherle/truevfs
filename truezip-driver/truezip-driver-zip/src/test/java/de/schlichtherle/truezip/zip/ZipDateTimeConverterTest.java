/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * @author Christian Schlichtherle
 */
public final class ZipDateTimeConverterTest extends DateTimeConverterTestSuite {

    @Override
    DateTimeConverter getInstance() {
        return DateTimeConverter.ZIP;
    }
}