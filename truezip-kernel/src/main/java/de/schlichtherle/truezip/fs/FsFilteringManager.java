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

import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.util.FilteringIterator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;

/**
 * Filters the list of federated file systems managed by the decorated file
 * system manager so that their mount point starts with the prefix provided
 * to the {@link #FsFilteringManager constructor}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsFilteringManager
extends FsDecoratingManager<FsManager> {

    private final URI prefix;

    /**
     * Constructs a new prefix filter file system manager from the given file
     * system manager and mount point prefix.
     *
     * @param manager the decorated file system manager.
     * @param prefix the prefix of the mount point used to filter all federated
     *        file systems of the decorated file system manager.
     */
    public FsFilteringManager(
            @NonNull final FsManager manager,
            @NonNull final FsMountPoint prefix) {
        super(manager);
        this.prefix = prefix.toHierarchicalUri();
    }

    @Override
    public int getSize() {
        int size = 0;
        for (FsController<?> controller : this)
            size++;
        return size;
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return new FilteredControllerIterator();
    }

    private final class FilteredControllerIterator
    extends FilteringIterator<FsController<?>> {
        final String ps = prefix.getScheme();
        final String pp = prefix.getPath();
        final int ppl = pp.length();
        final boolean pps = SEPARATOR_CHAR == pp.charAt(ppl - 1);

        FilteredControllerIterator() {
            super(delegate.iterator());
        }

        @Override
        protected boolean accept(final FsController<?> controller) {
            final URI mp = controller.getModel().getMountPoint().toHierarchicalUri();
            final String mpp;
            return mp.getScheme().equals(ps)
                    && (mpp = mp.getPath()).startsWith(pp)
                    && (pps 
                        || mpp.length() == ppl
                        || SEPARATOR_CHAR == mpp.charAt(ppl));
        }
    }
}
