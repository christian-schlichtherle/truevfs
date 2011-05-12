/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.RaesOutputStream;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.fs.archive.zip.ZipArchiveEntry;
import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;
import de.schlichtherle.truezip.fs.archive.zip.ZipInputShop;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.LazyOutputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.Immutable;

/**
 * A paranoid archive driver which builds RAES encrypted ZIP files.
 * This driver <em>always</em> checks the cipher text of input archive files
 * using the RAES Message Authentication Code (MAC), which makes it slower than
 * the {@link SafeZipRaesDriver} for archive files larger than 512 KB and
 * may pause the client application on the first access to the archive file
 * for a while if the file is large.
 * Note that the CRC-32 value of the plain text ZIP file is never checked
 * because this is made redundant by the MAC verification.
 * <p>
 * In addition, this driver limits the number of concurrent entry output
 * streams to one, so that writing unencrypted temporary files is inhibited.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see SafeZipRaesDriver
 */
@Immutable
public class ParanoidZipRaesDriver extends ZipRaesDriver {

    public ParanoidZipRaesDriver(   IOPoolProvider ioPoolProvider,
                                    KeyManagerProvider keyManagerProvider) {
        super(ioPoolProvider, keyManagerProvider);
    }

    @Override
    public final long getAuthenticationTrigger() {
        return Long.MAX_VALUE;
    }

    /**
     * This implementation calls {@link #getRaesParameters}, with which it
     * initializes a new {@link RaesOutputStream}, and finally passes the
     * resulting stream to {@link ZipDriver#newZipOutputShop}.
     * <p>
     * Note that this limits the number of concurrent output entry streams
     * to one in order to inhibit writing unencrypted temporary files for
     * buffering the written entries.
     */
    @Override
    public final OutputShop<ZipArchiveEntry>
    newOutputShop(  final FsConcurrentModel model,
                    final OutputSocket<?> output,
                    final InputShop<ZipArchiveEntry> source)
    throws IOException {
        final OutputStream out = new LazyOutputSocket<Entry>(output)
                .newOutputStream();
        try {
            final RaesOutputStream ros = RaesOutputStream.getInstance(
                    out, getRaesParameters(model));
            return newZipOutputShop(model, ros, (ZipInputShop) source);
        } catch (IOException cause) {
            try {
                out.close();
            } catch (IOException ex) {
                throw (IOException) ex.initCause(cause);
            }
            throw cause;
        }
    }
}
