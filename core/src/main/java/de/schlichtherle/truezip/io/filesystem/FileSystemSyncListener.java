/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.EventListener;

/**
 * Used to notify implementations of an event in a {@link FileSystemController}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface FileSystemSyncListener extends EventListener {

    /**
     * Called before a file system is going to get
     * {@link FileSystemController#sync synced}.
     */
    <X extends IOException>
    void beforeSync(@NonNull FileSystemSyncEvent<X> event)
    throws X, FileSystemException;
}
