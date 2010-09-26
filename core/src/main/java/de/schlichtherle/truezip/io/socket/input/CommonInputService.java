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
package de.schlichtherle.truezip.io.socket.input;

import de.schlichtherle.truezip.io.socket.entry.CommonEntryContainer;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.output.CommonOutputService;

/**
 * A common entry container which provides common input sockets.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <CE> The type of the common entries.
 * @see     CommonOutputService
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface CommonInputService<CE extends CommonEntry>
extends CommonEntryContainer<CE>, CommonInputProvider<CE> {
}
