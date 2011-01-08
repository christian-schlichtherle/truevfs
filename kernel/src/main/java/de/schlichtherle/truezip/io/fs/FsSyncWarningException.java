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

package de.schlichtherle.truezip.io.fs;

import java.io.IOException;

/**
 * Indicates an exceptional condition when synchronizing the changes in a
 * virtual file system with its parent file system.
 * An exception of this class implies that no or only insignificant parts of
 * the data in the file system have been lost.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FsSyncWarningException extends FSSyncException1 {

    private static final long serialVersionUID = 2302357394858347366L;

    public FsSyncWarningException(FSModel1 model, IOException cause) {
        super(model, cause, -1);
    }
}
