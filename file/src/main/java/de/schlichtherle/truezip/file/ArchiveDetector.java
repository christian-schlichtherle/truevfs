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

import de.schlichtherle.truezip.fs.FsFederatingDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * Detects archive files solely by scanning file paths -
 * usually by testing for file name suffixes like <i>.zip</i> or the
 * like.
 * <p>
 * An archive file which has been recognized by an {@code ArchiveDetector} is
 * said to be a <i>prospective archive file</i>.
 * On the first read or write access to a prospective archive file, TrueZIP
 * checks its <i>true state</i> in cooperation with the respective archive
 * driver .
 * If the true state of the file turns out to be actually a directory or not
 * to be compatible to the archive file format, it's said to be a <i>false
 * positive</i> archive file.
 * TrueZIP implements the appropriate behavior for all read or write
 * operations according to the true state, so it handles all kinds of false
 * positives correctly.
 * <p>
 * Rather than implementing {@code ArchiveDetector} directly, it's easier
 * to instantiate or subclass the {@link DefaultArchiveDetector} class.
 * This class provides a map for archive file suffixes and archive drivers.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public interface ArchiveDetector extends FsFederatingDriver {

    /**
     * Detects whether the given {@code path} identifies a prospective
     * archive file or not by applying heuristics to it and returns a
     * scheme for accessing archive file of this type or {@code null}
     * if the path does not denote a prospective archive file or an
     * appropriate scheme is unknown.
     * <p>
     * Please note that implementations <em>must not</em> check the actual
     * contents of the file identified by {@code path}!
     * This is because this method may be used to detect prospective archive
     * files by their path names before they are actually created or to detect
     * prospective archive files which are contained in other federated file
     * systems, in which case there is no way to check the file contents in the
     * parent file systems.
     *
     * @param  path The path name of the file in the federated file system.
     *         This does not need to be absolute and it does not need to be
     *         actually accessible in the parent file system!
     * @return A {@code scheme} for accessing the archive file or {@code null}
     *         if the path does not denote an archive file (i.e. the path does
     *         not have a known suffix) or an appropriate {@code scheme} is
     *         unknown.
     */
    @CheckForNull FsScheme getScheme(@NonNull String path);
}
