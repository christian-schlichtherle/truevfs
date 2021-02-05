/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes;

import global.namespace.truevfs.commons.cio.InputContainer;
import global.namespace.truevfs.commons.zipdriver.JarDriverEntry;
import global.namespace.truevfs.commons.zipdriver.ZipInputContainer;
import global.namespace.truevfs.commons.zipdriver.ZipOutputContainer;
import global.namespace.truevfs.kernel.api.FsModel;
import global.namespace.truevfs.kernel.api.FsOutputSocketSink;

import javax.annotation.CheckForNull;
import java.io.IOException;

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
 * Subclasses must be thread-safe.
 * 
 * @see    SafeZipRaesDriver
 * @author Christian Schlichtherle
 */
public class ParanoidZipRaesDriver extends ZipRaesDriver {

    @Override
    public final long getAuthenticationTrigger() {
        return Long.MAX_VALUE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ParanoidZipRaesDriver} returns a
     * new {@link ZipOutputContainer}.
     * This restricts the number of concurrent sink entry streams to one in
     * order to inhibit writing unencrypted temporary files for buffering the
     * written entries.
     */
    @Override
    protected final ZipOutputContainer<JarDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull InputContainer<JarDriverEntry> input)
    throws IOException {
        final ZipInputContainer<JarDriverEntry> zis = (ZipInputContainer<JarDriverEntry>) input;
        return new ZipOutputContainer<>(model, new RaesSocketSink(model, sink), zis, this);
    }
}
