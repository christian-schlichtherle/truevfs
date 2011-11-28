/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package de.schlichtherle.truezip.fs.archive.zip;

import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputSocket;
import static de.schlichtherle.truezip.zip.ZipEntry.STORED;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * Created by {@link OdfDriver} to meet the special requirements of
 * OpenDocument Format (ODF) files.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public class OdfOutputShop extends FsMultiplexedOutputShop<ZipArchiveEntry> {

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
    public OdfOutputShop(ZipOutputShop output, IOPool<?> pool) {
        super(output, pool);
    }

    @Override
    public OutputSocket<ZipArchiveEntry> getOutputSocket(final ZipArchiveEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends DecoratingOutputSocket<ZipArchiveEntry> {
            Output() {
                super(OdfOutputShop.super.getOutputSocket(entry));
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
        } // class Output

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
