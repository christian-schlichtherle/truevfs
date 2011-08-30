/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.OutputShop;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.Immutable;

/**
 * An archive driver which supports building archive files according to
 * the OpenDocument Format Specification (ODF), version 1.1.
 * This driver ensures that the entry named {@code mimetype}, if present at
 * all, is always written as the first entry and uses the {@code STORED} method
 * rather than the {@code DEFLATED} method in the resulting archive file in
 * order to meet the requirements of section 17.4 of the
 * <a href="http://www.oasis-open.org/committees/download.php/20847/OpenDocument-v1.1-cs1.pdf" target="_blank">OpenDocument Specification</a>,
 * version 1.1.
 * <p>
 * Other than this, ODF files are treated like regular JAR files.
 * In particular, this class does <em>not</em> check an ODF file for the
 * existance of the {@code META-INF/manifest.xml} entry or any other entry.
 * <p>
 * When using this driver to create or modify an ODF file, then in order to
 * achieve best performance, the {@code mimetype} entry should be created or
 * modified first in order to avoid temp file buffering of all other entries.
 *
 * @see     OdfOutputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class OdfDriver extends JarDriver {

    public OdfDriver(IOPoolProvider ioPoolProvider) {
        super(ioPoolProvider);
    }

    @Override
    protected OutputShop<ZipArchiveEntry> newOutputShop(
            final FsModel model,
            final OutputStream out,
            final @CheckForNull ZipInputShop source)
    throws IOException {
        final IOPool<?> pool = getPool();
        final ZipOutputShop shop = new ZipOutputShop(this, model, out, source);
        return null != source && source.isAppendee()
                ? new FsMultiplexedOutputShop<ZipArchiveEntry>(shop, pool)
                : new OdfOutputShop(shop, pool);
    }
}
