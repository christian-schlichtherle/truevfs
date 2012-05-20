/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import net.truevfs.kernel.FsModel;
import net.truevfs.kernel.cio.*;
import net.truevfs.kernel.io.Sink;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
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

    @Override
    protected OutputService<ZipDriverEntry> newOutput(
            final FsModel model,
            final Sink sink,
            final @CheckForNull @WillNotClose InputService<ZipDriverEntry> input)
    throws IOException {
        final OptionOutputSocket oos = (OptionOutputSocket) sink;
        final ZipInputService zis = (ZipInputService) input;
        final ZipOutputService zos = new ZipOutputService(model, oos, zis, this);
        final IOPool<?> pool = getIOPool();
        return null != zis && zis.isAppendee()
                ? new MultiplexingOutputService<>(pool, zos)
                : new OdfOutputService(pool, zos);
    }
}