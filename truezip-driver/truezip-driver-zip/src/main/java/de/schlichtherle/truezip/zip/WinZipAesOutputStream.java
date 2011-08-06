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

import de.schlichtherle.truezip.io.LEDataOutputStream;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * Encrypts ZIP entry contents according the WinZip AES specification.
 * 
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2</a>
 * @see     RawZipOutputStream$WinZipAesOutputMethod
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class WinZipAesOutputStream extends OutputStream {

    WinZipAesOutputStream(
            final LEDataOutputStream out,
            final WinZipAesEntryParameters param) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void write(int b) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void finish() throws IOException {
    }
}
