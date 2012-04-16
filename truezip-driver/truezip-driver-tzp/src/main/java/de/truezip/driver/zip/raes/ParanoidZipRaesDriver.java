/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import de.truezip.driver.zip.ZipDriverEntry;
import de.truezip.driver.zip.ZipInputService;
import de.truezip.driver.zip.ZipOutputService;
import de.truezip.driver.zip.raes.crypto.RaesOutputStream;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.cio.InputService;
import de.truezip.kernel.cio.OutputService;
import de.truezip.kernel.io.AbstractSink;
import de.truezip.kernel.io.Sink;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;

/**
 * A paranoid archive driver for RAES encrypted ZIP files.
 * This driver <em>always</em> checks the cipher text of input archive files
 * using the RAES Message Authentication Code (MAC), which makes it slower than
 * the {@link SafeZipRaesDriver} for archive files larger than 512 KB and
 * may pause the client application on the first access to the archive file
 * for a while if the file is large.
 * Note that the CRC-32 value of the plain text ZIP file is never checked
 * because this is made redundant by the MAC verification.
 * <p>
 * In addition, this driver limits the number of concurrent entry sink
 * streams to one, so that writing unencrypted temporary files is inhibited.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @see    SafeZipRaesDriver
 * @author Christian Schlichtherle
 */
@Immutable
public class ParanoidZipRaesDriver extends ZipRaesDriver {

    @Override
    public final long getAuthenticationTrigger() {
        return Long.MAX_VALUE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ParanoidZipRaesDriver} returns a
     * new {@link ZipOutputService}.
     * This restricts the number of concurrent sink entry streams to one in
     * order to inhibit writing unencrypted temporary files for buffering the
     * written entries.
     */
    @Override
    protected final OutputService<ZipDriverEntry> output(
            final FsModel model,
            final Sink sink,
            final @CheckForNull @WillNotClose InputService<ZipDriverEntry> input)
    throws IOException {
        final class Sink extends AbstractSink {
            @Override
            public OutputStream stream() throws IOException {
                return RaesOutputStream.create(raesParameters(model), sink);
            }
        } // Sink

        final ZipInputService zis = (ZipInputService) input;
        return new ZipOutputService(model, new Sink(), zis, this);
    }
}
