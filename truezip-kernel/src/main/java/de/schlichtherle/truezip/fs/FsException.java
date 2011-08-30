/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * An abstract exception which indicates an exceptional condition discovered
 * by the TrueZIP Kernel.
 * <p>
 * ONLY THE TRUEZIP KERNEL IS ALLOWED TO THROW AN EXCEPTION OF THIS TYPE!
 * DO NOT CREATE OR THROW AN EXCEPTION OF THIS TYPE (INCLUDING SUB-CLASSES)
 * ANYWHERE ELSE!
 *
 * @see     FsController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class FsException extends IOException {
    private static final long serialVersionUID = 2941522346756372554L;

    FsException() {
    }

    FsException(Throwable cause) {
        super(cause);
    }
}
