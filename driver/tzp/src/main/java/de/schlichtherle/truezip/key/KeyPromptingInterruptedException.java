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

import de.schlichtherle.truezip.file.swing.TFileChooser;
import javax.swing.JFileChooser;

/**
 * Thrown to indicate that prompting for a key to open or create a
 * protected resource has been interrupted.
 * As an example, if {@link TFileChooser} is used to browse RAES encrypted ZIP
 * files, then this may be thrown in the Basic L&F File Loader Threads if they
 * have been interrupted by the {@link JFileChooser} code running in AWT's
 * Event Dispatch Thread (EDT).
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
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
