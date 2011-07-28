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

import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.fs.FsModel;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.ThreadSafe;

/**
 * A concurrent file system model which provides the original values of
 * selected parameters for the {@link FsContextController} operation in
 * progress.
 *
 * @see     FsContextController
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsContextModel extends FsConcurrentModel {

    private volatile ThreadLocal<FsOperationContext>
            context = new ThreadLocal<FsOperationContext>();

    FsContextModel(FsModel model) {
        super(model);
    }

    /**
     * Returns a JavaBean which represents the original values of selected
     * parameters for the {@link FsContextController} operation in progress.
     * If no {@code FsContextController} operation is in progress, then
     * {@code null} gets returned.
     * <p>
     * Note that this is a thread-local property!
     * 
     * @return A JavaBean which represents the original values of selected
     *         parameters for the {@link FsContextController} operation in
     *         progress.
     */
    @Nullable FsOperationContext getContext() {
        return context.get();
    }

    /**
     * Sets the JavaBean which represents the original values of selected
     * parameters for the {@link FsContextController} operation in progress.
     * This method should only get called by the class
     * {@link FsContextController}.
     * <p>
     * Note that this is a thread-local property!
     * 
     * @param context the JavaBean which represents the original values of
     *        selected parameters for the {@link FsContextController}
     *        operation in progress.
     * @see   #getContext()
     */
    void setContext(final @CheckForNull FsOperationContext context) {
        if (null != context)
            this.context.set(context);
        else
            this.context.remove();
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
