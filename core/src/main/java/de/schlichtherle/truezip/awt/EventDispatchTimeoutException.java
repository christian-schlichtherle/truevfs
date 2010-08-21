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

package de.schlichtherle.truezip.awt;

/**
 * Thrown to indicate a timeout while waiting for the AWT Event Dispatch
 * Thread (EDT).
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class EventDispatchTimeoutException extends Exception {
    private static final long serialVersionUID = 1286457456293876327L;

    private final long timeout;

    /**
     * Creates a new instance of {@code EventDispatchTimeoutException} without detail message.
     */
    EventDispatchTimeoutException(final long timeout) {
        super("Waiting for the EDT timed out after milliseconds: " + timeout);
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }
}
