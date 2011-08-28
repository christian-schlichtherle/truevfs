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
package de.schlichtherle.truezip.zip;

import java.util.zip.ZipException;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that no suitable ZIP parameters have been provided
 * or something is wrong with these parameters.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ZipParametersException extends ZipException {
    private static final long serialVersionUID = 2032776586423467951L;

    /**
     * Constructs a ZIP parameters exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public ZipParametersException(String msg) {
        super(msg);
    }

    /**
     * Constructs a ZIP parameters exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public ZipParametersException(Throwable cause) {
        super.initCause(cause);
    }
}
