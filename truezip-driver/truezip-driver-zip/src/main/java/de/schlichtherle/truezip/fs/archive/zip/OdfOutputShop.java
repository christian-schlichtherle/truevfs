/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import de.schlichtherle.truezip.fs.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputSocket;
import static de.schlichtherle.truezip.zip.ZipEntry.STORED;
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
public class OdfOutputShop extends FsMultiplexedOutputShop<ZipDriverEntry> {

    /** The name of the entry to receive tender, loving care. */
    private static final String MIMETYPE = "mimetype";

    /** Whether we have started to write the <i>mimetype</i> entry or not. */
    private boolean mimetype;

    /**
     * Constructs a new ODF output shop.
     * 
     * @param output the decorated output shop.
     * @param pool the pool for buffering entry data.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public OdfOutputShop(@WillCloseWhenClosed ZipOutputShop output, IOPool<?> pool) {
        super(output, pool);
    }

    @Override
    public OutputSocket<ZipDriverEntry> getOutputSocket(final ZipDriverEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends DecoratingOutputSocket<ZipDriverEntry> {
            Output() {
                super(OdfOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public ZipDriverEntry getLocalTarget() throws IOException {
                return entry;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                if (MIMETYPE.equals(entry.getName())) {
                    mimetype = true;
                    if (UNKNOWN == entry.getMethod())
                        entry.setMethod(STORED);
                }
                return super.newOutputStream();
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
