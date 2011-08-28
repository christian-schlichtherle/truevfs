/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.zip;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.charset.Charset;

/**
 * Defines the default character set for accessing ZIP files.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
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
