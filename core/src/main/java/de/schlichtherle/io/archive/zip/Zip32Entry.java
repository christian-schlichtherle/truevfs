/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.io.archive.zip;

/**
 * @deprecated Use {@link ZipEntry} instead.
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class Zip32Entry extends ZipEntry {
    public static final byte UNKNOWN = de.schlichtherle.util.zip.ZipEntry.UNKNOWN;

    public Zip32Entry(String entryName) {
        super(entryName);
    }

    public Zip32Entry(Zip32Entry blueprint) {
        super(blueprint);
    }
}
