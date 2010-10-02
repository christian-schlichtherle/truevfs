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

import de.schlichtherle.truezip.io.FileBusyException;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.output.CommonOutputBusyException;

/**
 * Indicates that a {@link CommonInputSocket common input socket} or its
 * {@link CommonInputProvider common input provider} is busy on input.
 * This exception is guaranteed to be recoverable, meaning it should be
 * possible to read the same common entry again as soon as the source is
 * not busy anymore and unless another exceptional condition applies.
 *
 * @see CommonOutputBusyException
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class CommonInputBusyException
extends FileBusyException {

    private static final long serialVersionUID = 1983745618753823654L;

    /**
     * Constructs a new {@code CommonInputBusyException} with the specified
     * common entry.
     * 
     * @param entry The common entry which was tried to read while the
     *        destination was busy.
     */
    public CommonInputBusyException(CommonEntry entry) {
        super(entry.getName());
    }
}
