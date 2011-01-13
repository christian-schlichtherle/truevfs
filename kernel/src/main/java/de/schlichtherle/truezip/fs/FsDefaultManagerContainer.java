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

import de.schlichtherle.truezip.util.ServiceLocator;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import net.jcip.annotations.Immutable;

/**
 * Contains a file system manager of a class with a name which is resolved by
 * querying a system property or searching the class path, whatever yields a
 * result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.schlichtherle.truezip.fs.FsManager"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.FsManagerContainer"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, the expression
 * {@code new FsFailSafeManager(new FsFederatingManager())} is used to create
 * the file system manager in this container.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class FsDefaultManagerContainer extends FsManagerContainer {

    public static final FsManagerContainer
            INSTANCE = new FsDefaultManagerContainer();

    private final FsManager manager;

    /** You cannot instantiate this class. */
    @SuppressWarnings("unchecked")
    private FsDefaultManagerContainer() {
        final ServiceLocator locator = new ServiceLocator(
                FsDefaultManagerContainer.class.getClassLoader());
        final FsManagerContainer
                container = locator.getService(FsManagerContainer.class, null);
        if (null != container) {
            manager = container.getManager();
        } else {
            final Iterator<FsManagerContainer>
                    i = locator.getServices(FsManagerContainer.class);
            manager = i.hasNext()
                    ? i.next().getManager()
                    : new FsFailSafeManager(new FsFederatingManager());
        }
    }

    @Override
    public FsManager getManager() {
        return manager;
    }
}
