/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.odf;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.zipdriver.JarDriverEntry;
import global.namespace.truevfs.comp.zipdriver.ZipOutputContainer;
import global.namespace.truevfs.kernel.api.cio.MultiplexingOutputContainer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;

import static global.namespace.truevfs.comp.cio.Entry.UNKNOWN;
import static global.namespace.truevfs.comp.zip.ZipEntry.STORED;

/**
 * Created by {@link OdfDriver} to meet the special requirements of
 * OpenDocument Format (ODF) files.
 *
 * @author Christian Schlichtherle
 */
public class OdfOutputContainer extends MultiplexingOutputContainer<JarDriverEntry> {

    /** The name of the entry to receive tender, loving care. */
    private static final String MIMETYPE = "mimetype";

    /** Whether we have started to write the <i>mimetype</i> entry or not. */
    private boolean mimetype;

    /**
     * Constructs a new ODF output service.
     *
     * @param output the decorated output service.
     * @param pool the pool for buffering entry data.
     */
    public OdfOutputContainer(
            IoBufferPool pool,
            ZipOutputContainer<JarDriverEntry> output) {
        super(pool, output);
    }

    @Override
    public OutputSocket<JarDriverEntry> output(final JarDriverEntry entry) {
        Objects.requireNonNull(entry);

        final class Output extends DecoratingOutputSocket<JarDriverEntry> {
            Output() { super(OdfOutputContainer.super.output(entry)); }

            @Override
            public JarDriverEntry getTarget() throws IOException {
                return entry;
            }

            @Override
            public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                if (MIMETYPE.equals(entry.getName())) {
                    mimetype = true;
                    if (UNKNOWN == entry.getMethod())
                        entry.setMethod(STORED);
                }
                return getSocket().stream(peer);
            }
        } // Output

        return new Output();
    }

    @Override
    public boolean isBusy() {
        return !mimetype || super.isBusy();
    }

    @Override
    public void close() throws IOException {
        mimetype = true; // trigger writing temps
        super.close();
    }
}
