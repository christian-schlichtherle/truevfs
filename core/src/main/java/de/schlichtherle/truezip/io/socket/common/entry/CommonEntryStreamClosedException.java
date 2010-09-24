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

package de.schlichtherle.truezip.io.socket.common.entry;

import java.io.IOException;

/**
 * Indicates that an input or output stream for a common entry has been
 * forced to close.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class CommonEntryStreamClosedException extends IOException {
    private static final long serialVersionUID = 4563928734723923649L;
    
    // TODO: Make this package private!
    public CommonEntryStreamClosedException() {
        super("common entry stream has been forced to close()");
    }
}
