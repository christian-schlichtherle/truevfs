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

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * A file system model which supports multiple concurrent reader threads.
 * It's {@link #getOperation() operation} property provides access to a
 * {@link FsConcurrentOperation} JavaBean which encapsulates the original
 * values of selected parameters for the {@link FsConcurrentController}
 * operation in scope.
 *
 * @see     FsConcurrentController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsConcurrentModel extends FsDecoratingModel<FsModel> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile Operation operation = new FsConcurrentOperation();

    public FsConcurrentModel(FsModel model) {
        super(model);
    }

    ReadLock readLock() {
        return lock.readLock();
    }

    WriteLock writeLock() {
        return lock.writeLock();
    }

    /**
     * Returns {@code true} if and only if the write lock is held by the
     * current thread.
     * This method should only get used for assert statements, not for lock
     * control!
     * 
     * @return {@code true} if and only if the write lock is held by the
     *         current thread.
     * @see    #assertWriteLockedByCurrentThread()
     */
    public boolean isWriteLockedByCurrentThread() {
        return lock.isWriteLockedByCurrentThread();
    }

    /**
     * Asserts that the write lock is held by the current thread.
     * Use this method for lock control.
     * 
     * @throws FsNotWriteLockedException if the <i>write lock</i> is not
     *         held by the current thread.
     * @see    #isWriteLockedByCurrentThread()
     */
    public void assertWriteLockedByCurrentThread()
    throws FsNotWriteLockedException {
        if (!lock.isWriteLockedByCurrentThread())
            throw new FsNotWriteLockedException(this);
    }

    /**
     * @param  ex the caught exception.
     * @throws FsNotWriteLockedException if the <i>read lock</i> is
     *         held by the current thread.
     */
    void assertNotReadLockedByCurrentThread(
            @CheckForNull FsNotWriteLockedException ex)
    throws FsNotWriteLockedException {
        if (0 < lock.getReadHoldCount())
            throw new FsNotWriteLockedException(this, ex);
    }

    /**
     * A JavaBean which encapsulates the original values of selected parameters
     * for the {@link FsConcurrentController} operation in scope.
     * <p>
     * TODO: When adding any input operation parameters, make
     * {@link #setOperation} really thread-safe.
     */
    @NotThreadSafe
    public interface Operation {
        /**
         * Returns the options for the output operation in scope.
         * If the operation in scope is not an output operation, then
         * {@code null} is returned.
         * Otherwise, if the output operation in scope does not accept options,
         * then an empty bit field is returned.
         * Otherwise, a bit field with the options for the output operation is
         * returned.
         * 
         * @return The options for the output operation in scope.
         * @see    FsConcurrentController#getOutputSocket(FsEntryName, BitField, Entry)
         * @see    FsConcurrentController#mknod(FsEntryName, Entry.Type, BitField, Entry)
         */
        @Nullable BitField<FsOutputOption> getOutputOptions();
    } // Operation

    /**
     * Returns a JavaBean which encapsulates the original values of selected
     * parameters for the {@link FsConcurrentController} operation in scope.
     * It's an error to call this method if an {@code FsConcurrentController}
     * operation is not in scope and the result is undefined.
     * 
     * @return A JavaBean which encapsulates the original values of selected
     *         parameters for the {@link FsConcurrentController} operation in
     *         scope.
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * Sets the JavaBean which encapsulates the original values of selected
     * parameters for the {@link FsConcurrentController} operation in scope.
     * This method should only get called by the class
     * {@link FsConcurrentController}.
     * <p>
     * TODO: The current implementation is only virtually thread-safe: It
     * benefits from the fact that the current {@link FsConcurrentOperation}
     * API encapsulates only parameters for output operations and output
     * operations are effectively single-threaded by obtaining a write-lock in
     * {@link FsConcurrentController}.
     * As soon as any input operation parameters are added to
     * {@link FsConcurrentOperation}, this implementation needs to get changed
     * so that it uses a {@link ThreadLocal} or similar feature to make it
     * truly thread-safe.
     * 
     * @param operation the JavaBean which encapsulates the original values of
     *        selected parameters for the {@link FsConcurrentController}
     *        operation in scope.
     * @see   #getOperation()
     */
    void setOperation(final Operation operation) {
        //assert (null != request) ^ (null != this.request);
        this.operation = operation;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append(",request=")
                .append(getOperation())
                .append("]")
                .toString();
    }
}
