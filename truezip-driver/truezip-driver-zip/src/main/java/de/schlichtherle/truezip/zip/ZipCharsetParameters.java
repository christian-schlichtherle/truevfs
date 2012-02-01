/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import java.nio.charset.Charset;

/**
 * Defines the default character set for accessing ZIP files.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ZipCharsetParameters extends ZipParameters {

    /**
     * Returns the default character set for comments and entry names in a ZIP
     * file.
     * Subsequent calls must return the same object.
     * <p>
     * When reading a ZIP file, this is used to decode comments and entry names
     * in a ZIP file unless an entry has bit 11 set in its General Purpose Bit
     * Flags.
     * In this case, the character set is ignored and "UTF-8" is used for
     * decoding the entry.
     * This is in accordance to Appendix D of PKWARE's
     * <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">ZIP File Format Specification</a>,
     * version 6.3.0 and later.
     * 
     * @return The default character set for comments and entry names in a ZIP
     *         file.
     */
    Charset getCharset();
}
