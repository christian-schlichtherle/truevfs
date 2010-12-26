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
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import net.jcip.annotations.Immutable;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class FileSystemSyncEvent<X extends IOException> extends FileSystemEvent {

    private static final long serialVersionUID = 7656343108473675361L;

    private final BitField<SyncOption> options;

    @Nullable
    private final transient ExceptionHandler<? super SyncException, X> handler;

    /**
     * Constructs a new file system sync event.
     *
     * @param source the file system model source which caused this event.
     */
    public FileSystemSyncEvent(
            final FileSystemModel source,
            final BitField<SyncOption> options,
            final ExceptionHandler<? super SyncException, X> handler) {
        super(source);
        if (null == options || null == handler)
            throw new NullPointerException();
        this.options = options;
        this.handler = handler;
    }

    /**
     * Returns the options provided to the constructor.
     *
     * @return the options provided to the constructor.
     */
    public final BitField<SyncOption> getOptions() {
        return options;
    }

    /**
     * Returns the handler provided to the constructor or {@code null} if and
     * only if this event was deserialized.
     *
     * @return the handler provided to the constructor or {@code null} if and
     *         only if this event was deserialized.
     */
    @Nullable
    public final ExceptionHandler<? super SyncException, X> getHandler() {
        return handler;
    }
}
