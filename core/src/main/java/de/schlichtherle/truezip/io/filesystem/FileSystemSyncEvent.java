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

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.Immutable;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class FileSystemSyncEvent<X extends IOException> extends FileSystemEvent {

    private static final long serialVersionUID = 7656343108473675361L;

    private final @NonNull BitField<SyncOption> options;
    private final @NonNull ExceptionHandler<? super SyncException, X> handler;

    /**
     * Constructs a new file system sync event.
     *
     * @param source the file system model source which caused this event.
     */
    public FileSystemSyncEvent(
            @NonNull final FileSystemModel source,
            @NonNull final BitField<SyncOption> options,
            @NonNull final ExceptionHandler<? super SyncException, X> handler) {
        super(source);
        if (null == options || null == handler)
            throw new NullPointerException();
        this.options = options;
        this.handler = handler;
    }

    @NonNull
    public final BitField<SyncOption> getOptions() {
        return options;
    }

    @NonNull
    public final ExceptionHandler<? super SyncException, X> getHandler() {
        return handler;
    }
}
