/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.ibm437;

import net.java.truevfs.comp.ibm437.Ibm437Charset;
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
