/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.odf;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import static net.java.truevfs.comp.zip.ZipEntry.STORED;
import net.java.truevfs.comp.zipdriver.JarDriverEntry;
import net.java.truevfs.comp.zipdriver.ZipOutputService;
import static net.java.truecommons.cio.Entry.UNKNOWN;
import net.java.truecommons.cio.*;
import net.java.truevfs.kernel.spec.cio.MultiplexingOutputService;

/**
 * Created by {@link OdfDriver} to meet the special requirements of
 * OpenDocument Format (ODF) files.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class OdfOutputService extends MultiplexingOutputService<JarDriverEntry> {

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
    public OdfOutputService(
            IoBufferPool pool,
            @WillCloseWhenClosed ZipOutputService<JarDriverEntry> output) {
        super(pool, output);
    }

    @Override
    public OutputSocket<JarDriverEntry> output(final JarDriverEntry entry) {
        Objects.requireNonNull(entry);

        final class Output extends DecoratingOutputSocket<JarDriverEntry> {
            Output() { super(OdfOutputService.super.output(entry)); }

            @Override
            public JarDriverEntry target() throws IOException {
                return entry;
            }

            @Override
            public OutputStream stream(InputSocket<? extends Entry> peer)
            throws IOException {
                if (MIMETYPE.equals(entry.getName())) {
                    mimetype = true;
                    if (UNKNOWN == entry.getMethod())
                        entry.setMethod(STORED);
                }
                return socket().stream(peer);
            }
        } // Output

        return new Output();
    }

    @Override
    public boolean isBusy() {
        return !mimetype || super.isBusy();
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        mimetype = true; // trigger writing temps
        super.close();
    }
}
