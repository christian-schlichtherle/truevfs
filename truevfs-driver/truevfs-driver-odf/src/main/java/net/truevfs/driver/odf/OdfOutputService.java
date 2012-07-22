/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.odf;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.comp.zip.driver.AbstractZipDriverEntry;
import net.truevfs.comp.zip.driver.ZipOutputService;
import static net.truevfs.comp.zip.ZipEntry.STORED;
import static net.truevfs.kernel.spec.cio.Entry.UNKNOWN;
import net.truevfs.kernel.spec.cio.*;

/**
 * Created by {@link OdfDriver} to meet the special requirements of
 * OpenDocument Format (ODF) files.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class OdfOutputService extends MultiplexingOutputService<AbstractZipDriverEntry> {

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
            IoBufferPool<?> pool,
            @WillCloseWhenClosed ZipOutputService output) {
        super(pool, output);
    }

    @Override
    public OutputSocket<AbstractZipDriverEntry> output(final AbstractZipDriverEntry entry) {
        Objects.requireNonNull(entry);

        final class Output extends DecoratingOutputSocket<AbstractZipDriverEntry> {
            Output() { super(OdfOutputService.super.output(entry)); }

            @Override
            public AbstractZipDriverEntry target() throws IOException {
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
