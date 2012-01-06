/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * An output socket which provides a property for its output options.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class OptionOutputSocket
extends DecoratingOutputSocket<Entry> {
    final BitField<FsOutputOption> options;

    public OptionOutputSocket(
            final OutputSocket<?> output,
            final BitField<FsOutputOption> options) {
        super(output);
        this.options = options;
    }

    public BitField<FsOutputOption> getOptions() {
        return options;
    }

    @Override
    public Entry getLocalTarget() throws IOException {
        // Same implementation as super class, but makes stack trace nicer.
        return getBoundSocket().getLocalTarget();
    }

    @Override
    public Entry getPeerTarget() throws IOException {
        // Same implementation as super class, but makes stack trace nicer.
        return getBoundSocket().getPeerTarget();
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        // Same implementation as super class, but makes stack trace nicer.
        return getBoundSocket().newSeekableByteChannel();
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        // Same implementation as super class, but makes stack trace nicer.
        return getBoundSocket().newOutputStream();
    }
}
