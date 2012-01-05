/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.socket.IOPool;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
final class JulOutputByteChannel<E extends Entry>
extends DecoratingSeekableByteChannel {
    private static final Logger logger = Logger
            .getLogger(JulOutputByteChannel.class.getName());

    private final JulOutputSocket<E> socket;

    JulOutputByteChannel(final SeekableByteChannel model, final JulNio2OutputSocket<E> socket)
    throws IOException {
        super(model);
        if (null == model)
            throw new NullPointerException();
        this.socket = socket;
        E target = socket.getLocalTarget();
        Level level = target instanceof IOPool.Entry ? Level.FINER : Level.FINEST;
        logger.log(level, "Randomly writing " + target, new NeverThrowable());
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } finally {
            E target = socket.getLocalTarget();
            Level level = target instanceof IOPool.Entry ? Level.FINER : Level.FINEST;
            logger.log(level, "Closed " + target, new NeverThrowable());
        }
    }
    
}
