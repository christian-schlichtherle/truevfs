/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem.file;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.socket.InputCache;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputCache;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;

/**
 * Implements a caching strategy for input and output sockets.
 * Using this interface has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read from the local
 *     target and temporarily stored in this cache.
 *     Subsequent or concurrent read operations will be served from this cache
 *     without re-reading the data from the local target again until this cache
 *     gets {@link InputCache#clear cleared}.</li>
 * <li>At the discretion of the implementation, data written to this cache may
 *     not be written to the local target until this cache gets
 *     {@link OutputCache#flush flushed}.</li>
 * <li>After a write operation, the data will be temporarily stored in this
 *     cache for subsequent read operations until this cache gets
 *     {@link OutputCache#clear cleared}.
 * </ul>
 *
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface FileCache<LT extends Entry>
extends InputCache<LT>, OutputCache<LT> {

    /** Provides different cache strategies. */
    enum Strategy {
        /**
         * As the name implies, any attempt to create a new cache for output
         * will result in an {@link UnsupportedOperationException}.
         */
        READ_ONLY {
            @Override
            public <LT extends Entry>
            OutputCache<LT> newCache(OutputSocket <? extends LT> output) {
                throw new UnsupportedOperationException("read only cache!");
            }

            @Override
            public <LT extends Entry>
            FileCache<LT> newCache(   InputSocket<? extends LT> input,
                                    OutputSocket<? extends LT> output) {
                throw new UnsupportedOperationException("read only cache!");
            }

            @Override
            <LT extends Entry>
            Pool<DefaultCache<LT>.Buffer, IOException> newOutputStrategy(
                    DefaultCache<LT> cache) {
                throw new AssertionError();
            }
        },

        /**
         * A write-through cache flushes any written data as soon as the
         * output stream created by the provided output socket gets closed.
         */
        WRITE_THROUGH {
            @Override
            <LT extends Entry>
            Pool<DefaultCache<LT>.Buffer, IOException> newOutputStrategy(
                    DefaultCache<LT> cache) {
                return cache.new WriteThroughOutputStrategy();
            }
        },

        /**
         * A write-back cache flushes any written data if and only if it gets
         * explicitly flushed.
         */
        WRITE_BACK {
            @Override
            <LT extends Entry>
            Pool<DefaultCache<LT>.Buffer, IOException> newOutputStrategy(
                    DefaultCache<LT> cache) {
                return cache.new WriteBackOutputStrategy();
            }
        };

        /** Returns a new input cache. */
        public <LT extends Entry>
        InputCache<LT> newCache(InputSocket <? extends LT> input) {
            if (null == input)
                throw new NullPointerException();
            return new DefaultCache<LT>(input, null, this);
        }

        /** Returns a new output cache. */
        public <LT extends Entry>
        OutputCache<LT> newCache(OutputSocket <? extends LT> output) {
            if (null == output)
                throw new NullPointerException();
            return new DefaultCache<LT>(null, output, this);
        }

        /** Returns a new input / output cache. */
        public <LT extends Entry>
        FileCache<LT> newCache(   InputSocket<? extends LT> input,
                                OutputSocket<? extends LT> output) {
            if (null == input || null == output)
                throw new NullPointerException();
            return new DefaultCache<LT>(input, output, this);
        }

        abstract <LT extends Entry>
        Pool<DefaultCache<LT>.Buffer, IOException> newOutputStrategy(
                DefaultCache<LT> cache);
    }
}
