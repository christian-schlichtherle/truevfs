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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;

/**
 * Implements a caching strategy for input and output sockets.
 * Using this interface has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read from the local
 *     target and stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the data from the local target again until the cache
 *     gets cleared.
 * <li>Any data written to the cache will get written to the local target if
 *     and only if the cache gets flushed.
 * <li>After a write operation, the data will be stored in the cache for
 *     subsequent read operations until the cache gets cleared.
 * </ul>
 *
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface Cache<LT extends CommonEntry>
extends InputCache<LT>, OutputCache<LT> {

    enum Strategy {
        READ_ONLY {
            @Override
            public <LT extends CommonEntry>
            OutputCache<LT> newCache(OutputSocket <? extends LT> output) {
                throw new UnsupportedOperationException("read only cache!");
            }

            @Override
            public <LT extends CommonEntry>
            Cache<LT> newCache( InputSocket<? extends LT> input,
                                OutputSocket<? extends LT> output) {
                throw new UnsupportedOperationException("read only cache!");
            }

            @Override
            <LT extends CommonEntry>
            Pool<DefaultCache<LT>.Buffer, IOException> newOutputBufferPool(
                    DefaultCache<LT> cache) {
                throw new AssertionError();
            }
        },

        WRITE_THROUGH {
            @Override
            <LT extends CommonEntry>
            Pool<DefaultCache<LT>.Buffer, IOException> newOutputBufferPool(
                    DefaultCache<LT> cache) {
                return cache.new WriteThroughOutputBufferPool();
            }
        },

        WRITE_BACK {
            @Override
            <LT extends CommonEntry>
            Pool<DefaultCache<LT>.Buffer, IOException> newOutputBufferPool(
                    DefaultCache<LT> cache) {
                return cache.new WriteBackOutputBufferPool();
            }
        };

        public <LT extends CommonEntry>
        InputCache<LT> newCache(InputSocket <? extends LT> input) {
            if (null == input)
                throw new NullPointerException();
            return new DefaultCache<LT>(input, null, this);
        }

        public <LT extends CommonEntry>
        OutputCache<LT> newCache(OutputSocket <? extends LT> output) {
            if (null == output)
                throw new NullPointerException();
            return new DefaultCache<LT>(null, output, this);
        }

        public <LT extends CommonEntry>
        Cache<LT> newCache( InputSocket<? extends LT> input,
                            OutputSocket<? extends LT> output) {
            if (null == input || null == output)
                throw new NullPointerException();
            return new DefaultCache<LT>(input, output, this);
        }

        abstract <LT extends CommonEntry>
        Pool<DefaultCache<LT>.Buffer, IOException> newOutputBufferPool(
                DefaultCache<LT> cache);
    }
}
