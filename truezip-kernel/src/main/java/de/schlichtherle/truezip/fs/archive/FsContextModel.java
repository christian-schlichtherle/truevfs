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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.FsConcurrentController;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.fs.FsModel;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * A concurrent file system model which provides the original values of
 * selected parameters for the {@link FsContextController} operation in scope
 * as a context.
 *
 * @see     FsContextController
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsContextModel extends FsConcurrentModel {

    private volatile FsOperationContext context = new FsOperationContext();

    FsContextModel(FsModel model) {
        super(model);
    }

    /**
     * Returns a JavaBean which represents the original values of selected
     * parameters for the {@link FsContextController} operation in scope.
     * It's an error to call this method if an {@code FsContextController}
     * operation is not in scope and the result is undefined.
     * 
     * @return A JavaBean which represents the original values of selected
     *         parameters for the {@link FsContextController} operation in
     *         scope.
     */
    public FsOperationContext getContext() {
        return context;
    }

    /**
     * Sets the JavaBean which represents the original values of selected
     * parameters for the {@link FsContextController} operation in scope.
     * This method should only get called by the class
     * {@link FsContextController}.
     * <p>
     * TODO: The current implementation is only virtually thread-safe: It
     * benefits from the fact that the current {@link FsOperationContext}
     * API encapsulates only parameters for output operations and output
     * operations are effectively single-threaded by obtaining a write-lock in
     * {@link FsConcurrentController}.
     * As soon as any input operation parameters are added to
     * {@link FsOperationContext}, this implementation needs to get changed
     * so that it uses a {@link ThreadLocal} or similar feature to make it
     * truly thread-safe.
     * 
     * @param context the JavaBean which represents the original values of
     *        selected parameters for the {@link FsContextController}
     *        operation in scope.
     * @see   #getContext()
     */
    void setContext(final FsOperationContext context) {
        assert null != context;
        this.context = context;
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
                .append(",operation=")
                .append(getContext())
                .append("]")
                .toString();
    }
}
