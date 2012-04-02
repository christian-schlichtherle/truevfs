/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.MultiplexedOutputService;
import de.truezip.kernel.cio.DecoratingOutputSocket;
import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.cio.OutputSocket;
import static de.truezip.driver.zip.io.ZipEntry.STORED;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
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
public class OdfOutputService extends MultiplexedOutputService<ZipDriverEntry> {

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
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public OdfOutputService(@WillCloseWhenClosed ZipOutputService output, IOPool<?> pool) {
        super(output, pool);
    }

    @Override
    public OutputSocket<ZipDriverEntry> getOutputSocket(final ZipDriverEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends DecoratingOutputSocket<ZipDriverEntry> {
            Output() {
                super(OdfOutputService.super.getOutputSocket(entry));
            }

            @Override
            public ZipDriverEntry getLocalTarget() throws IOException {
                return entry;
            }

            @Override
            public OutputStream newStream() throws IOException {
                if (MIMETYPE.equals(entry.getName())) {
                    mimetype = true;
                    if (UNKNOWN == entry.getMethod())
                        entry.setMethod(STORED);
                }
                return super.newStream();
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
