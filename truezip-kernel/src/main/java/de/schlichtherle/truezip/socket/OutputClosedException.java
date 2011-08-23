/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.socket;

import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Indicates that an output resource (output stream etc.) for an entry has been
 * forced to close.
 *
 * @see     InputClosedException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class OutputClosedException extends IOException {
    private static final long serialVersionUID = 4563928734723923649L;

    public OutputClosedException() {
        super("Output resource has been forced to close!");
    }

    public OutputClosedException(Throwable cause) {
        super("Output resource has been forced to close!", cause);
    }
}
