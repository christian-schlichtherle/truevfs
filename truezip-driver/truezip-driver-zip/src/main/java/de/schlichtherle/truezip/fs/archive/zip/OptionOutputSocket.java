/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
