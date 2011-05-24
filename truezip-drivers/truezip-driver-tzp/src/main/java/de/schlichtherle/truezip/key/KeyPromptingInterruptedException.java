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
package de.schlichtherle.truezip.key;

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that prompting for a key to open or create a
 * protected resource has been interrupted.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class KeyPromptingInterruptedException extends UnknownKeyException  {
    private static final long serialVersionUID = 7656348607356445644L;

    public KeyPromptingInterruptedException() {
        super("Key prompting has been interrupted!");
    }

    public KeyPromptingInterruptedException(Throwable cause) {
        super("Key prompting has been interrupted!");
        super.initCause(cause);
    }
}
