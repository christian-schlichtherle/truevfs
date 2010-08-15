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

package de.schlichtherle.key;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has been cancelled.
 * This is normally caused by user input, for example if the user has closed
 * the prompting dialog.
 *
 * @author Christian Schlichtherle
 * @since TrueZIP 6.1
 * @version $Revision$
 */
public class KeyPromptingCancelledException extends UnknownKeyException {
    
    public KeyPromptingCancelledException() {
        super("Key prompting has been cancelled!");
    }
}
