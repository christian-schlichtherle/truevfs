/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.entry.MultiplexedOutputService;
import de.schlichtherle.truezip.entry.IOPool;
import de.schlichtherle.truezip.entry.IOPoolProvider;
import de.schlichtherle.truezip.entry.OutputService;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.Immutable;

/**
 * An archive driver for application archive files according to the Open
 * Document Format (ODF) Specification, V1.0 and later.
 * This driver simply ensures that an entry named {@code mimetype}, if present
 * at all, is always written as the very first entry using the method
 * {@code STORED} rather than {@code DEFLATED} in the resulting archive file
 * in order to meet the requirements of the ODF Specification.
 * Other than this, ODF files are treated like regular JAR files.
 * In particular, this class does <em>not</em> check an ODF file for the
 * existence of the {@code META-INF/manifest.xml} entry or any other entry.
 * <p>
 * When using this driver to create or modify an ODF file, then in order to
 * achieve best performance, the {@code mimetype} entry should always get
 * written first in order to avoid temp file buffering of all other entries.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 *
 * @see    <a href="http://docs.oasis-open.org/office/v1.0/OpenDocument-v1.0-os.pdf">Open Document Format for Office Applications (OpenDocument) v1.0; Section 17.4: MIME Type Stream</a>
 * @see    <a href="http://docs.oasis-open.org/office/v1.1/OS/OpenDocument-v1.1.pdf">Open Document Format for Office Applications (OpenDocument) v1.1; Section 17.4: MIME Type Stream</a>
 * @see    <a href="http://docs.oasis-open.org/office/v1.2/OpenDocument-v1.2-part3.pdf">Open Document Format for Office Applications (OpenDocument) Version 1.2; Part 3: Packages; Section 3.3: MIME Type Stream</a>
 * @see    OdfOutputService
 * @author Christian Schlichtherle
 */
@Immutable
public class OdfDriver extends JarDriver {

    public OdfDriver(IOPoolProvider provider) {
        super(provider);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    protected OutputService<ZipDriverEntry> newOutputService(
            final FsModel model,
            final OutputStream out,
            final ZipInputService source)
    throws IOException {
        final ZipOutputService service = new ZipOutputService(this, model, out, source);
        final IOPool<?> pool = getPool();
        return null != source && source.isAppendee()
                ? new MultiplexedOutputService<ZipDriverEntry>(service, pool)
                : new OdfOutputService(service, pool);
    }
}
