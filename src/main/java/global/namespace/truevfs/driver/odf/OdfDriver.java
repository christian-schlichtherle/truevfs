/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.odf;

import global.namespace.truevfs.comp.cio.InputService;
import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.comp.cio.OutputService;
import global.namespace.truevfs.comp.zipdriver.JarDriver;
import global.namespace.truevfs.comp.zipdriver.JarDriverEntry;
import global.namespace.truevfs.comp.zipdriver.ZipInputService;
import global.namespace.truevfs.comp.zipdriver.ZipOutputService;
import global.namespace.truevfs.kernel.api.FsModel;
import global.namespace.truevfs.kernel.api.FsOutputSocketSink;
import global.namespace.truevfs.kernel.api.cio.MultiplexingOutputService;

import javax.annotation.CheckForNull;
import java.io.IOException;

import static global.namespace.truevfs.kernel.api.FsAccessOption.GROW;

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
public class OdfDriver extends JarDriver {

    @Override
    protected OutputService<JarDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull InputService<JarDriverEntry> input)
    throws IOException {
        final ZipInputService<JarDriverEntry> zis = (ZipInputService<JarDriverEntry>) input;
        final ZipOutputService<JarDriverEntry> zos = new ZipOutputService<>(model, sink, zis, this);
        final IoBufferPool pool = getPool();
        return null != zis && sink.getOptions().get(GROW)
                ? new MultiplexingOutputService<>(pool, zos)
                : new OdfOutputService(pool, zos);
    }
}
