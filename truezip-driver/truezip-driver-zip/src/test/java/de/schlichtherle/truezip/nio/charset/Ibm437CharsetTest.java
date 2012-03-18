/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.nio.charset;

import java.nio.charset.Charset;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Ibm437CharsetTest extends OctetCharsetTestSuite {

    @Override
    protected Charset newCharset() {
        return new Ibm437Charset();
    }
}
