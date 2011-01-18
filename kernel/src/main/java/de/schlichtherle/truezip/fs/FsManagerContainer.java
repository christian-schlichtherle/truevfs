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
import net.jcip.annotations.Immutable;

/**
 * Contains a file system manager of a class with a name which is resolved by
 * querying a system property or searching the class path, whatever yields a
 * result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.schlichtherle.truezip.fs.FsManagerService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.FsManagerService"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, the expression
 * {@code new FsFailSafeManager(new FsDefaultManager())} is used to create the
 * file system manager in this container.
 * <p>
 * Note that the kernel classes have no dependency on this class; so using
 * this service locator is completely optional for a pure kernel application.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class FsManagerContainer implements FsManagerService {

    /** The singleton instance of this class. */
    public static final FsManagerContainer SINGLETON = new FsManagerContainer();

    private final FsManager manager;

    /** You cannot instantiate this class. */
    private FsManagerContainer() {
        final ServiceLocator locator = new ServiceLocator(
                FsManagerContainer.class.getClassLoader());
        final FsManagerService
                container = locator.getService(FsManagerService.class, null);
        if (null != container) {
            manager = container.getManager();
        } else {
            final Iterator<FsManagerService>
                    i = locator.getServices(FsManagerService.class);
            manager = i.hasNext()
                    ? i.next().getManager()
                    : new FsFailSafeManager(new FsDefaultManager());
        }
    }

    @Override
    public FsManager getManager() {
        return manager;
    }
}
