/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * Detects prospective archive files (i.e. prospective federated file
 * systems) solely by scanning file path names - usually by testing for file
 * name suffixes like <i>.zip</i> etc.
 * <p>
 * Rather than implementing {@code TArchiveDetector} directly, please consider
 * instantiating or subclassing the {@link TDefaultArchiveDetector} class.
 * This class provides a map for archive file suffixes and archive drivers.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public interface TArchiveDetector extends FsCompositeDriver {

    /**
     * Detects whether the given {@code path} name identifies a prospective
     * archive file or not by applying heuristics to it and returns a
     * scheme for accessing archive files of this type or {@code null}
     * if the path does not denote a prospective archive file or an
     * appropriate scheme is unknown.
     * <p>
     * Please note that implementations <em>must not</em> check the actual
     * contents of the file identified by {@code path}!
     * This is because {@code path} may refer to a file which is not yet
     * existing or even an entry in a federated file system, in which case
     * there is no way to check the file contents in the parent file systems.
     *
     * @param  path the path name of the file in the federated file system.
     *         This does not need to be absolute and it does not need to be
     *         accessible in its containing virtual file system!
     * @return A {@code scheme} for accessing the archive file or {@code null}
     *         if the path does not denote an archive file (i.e. the path does
     *         not have a known suffix) or an appropriate {@code scheme} is
     *         unknown.
     */
    @CheckForNull FsScheme getScheme(@NonNull String path);
}
