/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.Archive;
import java.io.IOException;

/**
 * Indicates an exceptional condition detected by an {@link ArchiveController}
 * which is recoverable. Please consult
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class RecoverableArchiveControllerException
extends ArchiveControllerWarningException {

    private static final long serialVersionUID = 4893204620324852936L;

    public RecoverableArchiveControllerException(Archive archive) {
        super(archive);
    }

    public RecoverableArchiveControllerException(Archive archive, String message) {
        super(archive, message);
    }

    public RecoverableArchiveControllerException(Archive archive, IOException cause) {
        super(archive, cause);
    }

    public RecoverableArchiveControllerException(Archive archive, String message, IOException cause) {
        super(archive, message, cause);
    }
}
