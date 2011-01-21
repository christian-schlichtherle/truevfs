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

package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolService;
import java.io.IOException;
import net.jcip.annotations.Immutable;


/**
 * An archive driver for SFX/EXE files which checks the CRC-32 value for all
 * ZIP entries in input archives.
 * The additional CRC-32 computation makes this class slower than its super
 * class.
 * <p>
 * If there is a mismatch of the CRC-32 values for a ZIP entry in an input
 * archive, the {@link java.io.InputStream#close} method of the corresponding
 * stream for the archive entry will throw a
 * {@link de.schlichtherle.truezip.zip.CRC32Exception}.
 * Other than this, the archive entry will be processed normally.
 * So if just the CRC-32 value for the entry in the archive file has been
 * modified, you can still read its entire contents.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see CheckedZipInputShop
 */
@Immutable
public class CheckedReadOnlySfxDriver extends ReadOnlySfxDriver {

    public CheckedReadOnlySfxDriver(IOPoolService service) {
        super(service);
    }

    @Override
    protected ZipInputShop newZipInputShop(FsConcurrentModel model, ReadOnlyFile rof)
    throws IOException {
        return new CheckedZipInputShop(rof, this);
    }
}
