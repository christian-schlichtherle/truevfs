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

import java.util.Iterator;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.io.filesystem.SyncOption.*;

/**
 * A container which manages the lifecycle of controllers for federated file
 * systems. A file system is federated if and only if it's a member of a parent
 * file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public abstract class FileSystemManager
implements Iterable<FileSystemController<?>> {

    /**
     * Returns a file system controller for the given mount point.
     * If and only if the given mount point addresses a federated file system,
     * the returned file system controller is remembered for life cycle
     * management, i.e. future lookup and {@link #sync synchronization}
     * operations.
     *
     * @param  mountPoint the mount point of the file system.
     * @param  driver the file system driver which will be used to create a
     *         new file system controller if required.
     * @return A non-{@code null} file system controller.
     * @throws NullPointerException if {@code mountPoint} is {@code null}
     */
    @NonNull
    public abstract FileSystemController<?> getController(
            @NonNull MountPoint mountPoint,
            @NonNull FileSystemDriver<?> driver);

    /**
     * Returns the number of federated file systems managed by this instance.
     *
     * @return The number of federated file systems managed by this instance.
     */
    public abstract int size();

    /**
     * Returns an iterator for the controller of all federated file systems
     * managed by this instance.
     * <p>
     * <strong>Important:</strong> The iterated file system controllers must be
     * ordered so that all file systems appear before any of their parent file
     * systems.
     *
     * @return An iterator for the controller of all federated file systems
     *         managed by this instance.
     */
    @Override
    @NonNull
    public abstract Iterator<FileSystemController<?>> iterator();

    /**
     * A visitor for file system controllers.
     *
     * @see #visit(Visitor, ExceptionBuilder)
     */
    public interface Visitor {
        void visit(FileSystemController<?> controller) throws IOException;
    }

    /**
     * Visits the controller of all federated file systems managed by this
     * instance.
     *
     * @param  <X> the type of the assembled {@code IOException} to throw.
     * @param  builder the exception builder to use for the assembly of an
     *         {@code IOException} from one or more input {@code IOException}s.
     * @throws IOException at the discretion of the exception {@code builder}.
     */
    public <X extends IOException>
    void visit( @NonNull Visitor visitor,
                @NonNull ExceptionBuilder<? super IOException, X> builder)
    throws X {
        for (FileSystemController<?> controller : this) {
            try {
                visitor.visit(controller);
            } catch (IOException ex) {
                // Visiting the file system controller resulted in an I/O
                // exception for some reason.
                // We are bullheaded and store the exception for later
                // throwing and continue updating the rest.
                builder.warn(ex);
            }
        }
        builder.check();
    }

    /**
     * Writes all changes to the contents of the federated file systems managed
     * by this instance to their respective parent file system.
     * This will reset the state of the respective file system controllers.
     *
     * @param  <X> the type of the assembled {@code IOException} to throw.
     * @param  options the synchronization options.
     * @param  builder the exception builder to use for the assembly of an
     *         {@code IOException} from one or more input {@code IOException}s.
     * @throws IOException at the discretion of the exception {@code builder}.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FORCE_CLOSE_INPUT} is cleared
     *         and {@code FORCE_CLOSE_OUTPUT} is set or if the synchronization
     *         option {@code ABORT_CHANGES} is set.
     */
    public <X extends IOException>
    void sync(  @NonNull final BitField<SyncOption> options,
                @NonNull final ExceptionBuilder<? super IOException, X> builder)
    throws X {
        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT)
                || options.get(ABORT_CHANGES))
            throw new IllegalArgumentException();
        class Sync implements Visitor {
            @Override
            public void visit(FileSystemController<?> controller)
            throws IOException {
                controller.sync(options, builder);
            }
        }
        visit(new Sync(), builder);
    }

    /**
     * Two file system managers are considered equal if and only if they are
     * identical. This can't get overriden.
     */
    @Override
    @SuppressWarnings(value = "EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
