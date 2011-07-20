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

import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import net.jcip.annotations.Immutable;

/**
 * An archive driver which reads Self Executable (SFX/EXE) ZIP files,
 * but doesn't support to create or update them because this would spoil the
 * SFX code in its preamble.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class ReadOnlySfxDriver extends ZipDriver {

    /**
     * The character set used in SFX archives by default, which is determined
     * by calling {@code System.getProperty("file.encoding")}.
     */
    public static final Charset SFX_CHARSET
            = Charset.forName(System.getProperty("file.encoding"));

    public ReadOnlySfxDriver(IOPoolProvider provider) {
        super(provider, SFX_CHARSET);
    }

    @Override
    public final boolean getPreambled() {
        return true;
    }

    @Override
    protected ZipOutputShop newZipOutputShop(
            FsModel model, OutputStream out, ZipInputShop source)
    throws IOException {
        throw new FileNotFoundException(
                "driver class does not support creating or modifying SFX archives");
    }
}
