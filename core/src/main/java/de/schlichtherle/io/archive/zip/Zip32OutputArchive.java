/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.io.archive.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * @deprecated Use {@link ZipOutputArchive} instead.
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.0
 */
public class Zip32OutputArchive extends ZipOutputArchive {

    /** @deprecated */
    public Zip32OutputArchive(
            final OutputStream out,
            final String charset,
            final Zip32InputArchive source)
    throws  NullPointerException,
            UnsupportedEncodingException,
            IOException {
        this(out, charset, Zip32Driver.DEFAULT_LEVEL, source);
    }

    public Zip32OutputArchive(
            final OutputStream out,
            final String charset,
            final int level,
            final Zip32InputArchive source)
    throws  NullPointerException,
            UnsupportedEncodingException,
            IOException {
        super(out, charset, level, source);
    }
}
