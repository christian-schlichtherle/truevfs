/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
