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

package de.schlichtherle.truezip.util.zip;

/**
 * A factory for {@link ZipEntry}s.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @see BasicZipFile
 */
public interface ZipEntryFactory {

    //ZipEntryFactory DEFAULT = DefaultZipEntryFactory.SINGLETON;

    /**
     * Creates a new {@link ZipEntry} with the given name.
     *
     * @param name The entry name.
     * @return A newly created {@link ZipEntry} with the given name.
     *         {@code null} is not permitted.
     */
    ZipEntry newZipEntry(String name);
}
