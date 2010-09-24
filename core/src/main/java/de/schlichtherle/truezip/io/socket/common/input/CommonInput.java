/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.socket.common.input;

import de.schlichtherle.truezip.io.socket.common.entry.CommonEntryContainer;
import de.schlichtherle.truezip.io.socket.common.output.CommonOutput;
import de.schlichtherle.truezip.io.socket.common.entry.CommonEntry;
import java.io.Closeable;

/**
 * A closeable common entry container which provides input sockets for
 * reading common entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <CE> The type of the common entries.
 * @see     CommonOutput
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface CommonInput<CE extends CommonEntry>
extends Closeable,
        CommonEntryContainer<CE>,
        CommonInputSocketProvider<CE> {
}
