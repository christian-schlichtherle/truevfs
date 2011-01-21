/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.fs.FsCachingController;
import de.schlichtherle.truezip.fs.FsConcurrentController;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.socket.IOPool;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
final class HttpDriver implements FsDriver {

    private final IOPool<?> pool;

    public HttpDriver(final IOPool<?> pool) {
        if (null == pool)
            throw new NullPointerException();
        this.pool = pool;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link HttpDriver} always returns
     * {@code false}.
     */
    @Override
    public boolean isFederated() {
        return false;
    }

    @Override
    public FsController<?>
    newController(FsMountPoint mountPoint, @CheckForNull FsController<?> parent) {
        assert null == mountPoint.getParent()
                ? null == parent
                : mountPoint.getParent().equals(parent.getModel().getMountPoint());
        if (null != parent)
            throw new IllegalArgumentException();
        // Using FsCachingController allows to serve ReadOnlyFile objects
        // although the HttpController serves only InputStream objects.
        return  new FsConcurrentController(
                   new FsCachingController(
                        new HttpController<FsConcurrentModel>(
                            new FsConcurrentModel(mountPoint)),
                        pool));
    }
}
