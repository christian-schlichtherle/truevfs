/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import static de.truezip.driver.zip.io.ZipEntry.STORED;
import de.truezip.kernel.cio.DecoratingOutputSocket;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.cio.MultiplexingOutputService;
import de.truezip.kernel.cio.OutputSocket;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Created by {@link OdfDriver} to meet the special requirements of
 * OpenDocument Format (ODF) files.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class OdfOutputService extends MultiplexingOutputService<ZipDriverEntry> {

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
            IOPool<?> pool,
            @WillCloseWhenClosed ZipOutputService output) {
        super(pool, output);
    }

    @Override
    public OutputSocket<ZipDriverEntry> outputSocket(final ZipDriverEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        final class Output extends DecoratingOutputSocket<ZipDriverEntry> {
            Output() {
                super(OdfOutputService.super.outputSocket(entry));
            }

            @Override
            public ZipDriverEntry localTarget() throws IOException {
                return entry;
            }

            @Override
            public OutputStream stream() throws IOException {
                if (MIMETYPE.equals(entry.getName())) {
                    mimetype = true;
                    if (UNKNOWN == entry.getMethod())
                        entry.setMethod(STORED);
                }
                return super.stream();
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
