/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.charset;

import java.nio.charset.Charset;

/**
 * @author Christian Schlichtherle
 */
public class Ibm437CharsetTest extends OctetCharsetTestSuite {

    @Override
    protected Charset newCharset() {
        return new Ibm437Charset();
    }
}