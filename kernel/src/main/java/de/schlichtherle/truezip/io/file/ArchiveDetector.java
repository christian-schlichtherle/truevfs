/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.file;

import de.schlichtherle.truezip.io.fs.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.fs.FsScheme;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.Immutable;

/**
 * Detects archive files solely by scanning file paths -
 * usually by testing for file name suffixes like <i>.zip</i> or the
 * like.
 * If the method {@link #getScheme(String)} detects an archive file, it
 * returns a {@link FsScheme} for accessing files of this type.
 * Next, for any scheme returned by this method, the method
 * {@link #getDriver(FsScheme)} returns an {@link ArchiveDriver} for accessing
 * files of this scheme type.
 * <p>
 * An archive file which has been recognized by an {@code ArchiveDetector} is
 * said to be a <i>prospective archive file</i>.
 * On the first read or write access to a prospective archive file, TrueZIP
 * checks its <i>true state</i> in cooperation with the {@link ArchiveDriver}.
 * If the true state of the file turns out to be actually a directory or not
 * to be compatible to the archive file format, it's said to be a <i>false
 * positive</i> archive file.
 * TrueZIP implements the appropriate behavior for all read or write
 * operations according to the true state, so it handles all kinds of false
 * positives correctly.
 * <p>
 * Implementations must be (virtually) immutable and hence thread safe.
 * <p>
 * Rather than implementing {@code ArchiveDetector} directly, it's easier
 * to instantiate or subclass the {@link DefaultArchiveDetector} class.
 * This class provides a registry for archive file suffixes and archive drivers
 * which can be easily customized via configuration files or Java code.
 * <p>
 * Although not strictly required, it's recommended for implementations to
 * implement the {@link java.io.Serializable} interface too, so that
 * {@link File} instances which use it can be serialized.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public interface ArchiveDetector {

    /**
     * Never recognizes archive files in a path.
     * This can be used as the end of a chain of
     * {@code DefaultArchiveDetector} instances or if archive files
     * shall be treated like ordinary files rather than (virtual) directories.
     */
    DefaultArchiveDetector NULL = new DefaultArchiveDetector("");

    /**
     * Recognizes all archive file suffixes registered in the global archive
     * driver registry by the configuration file(s).
     * <p>
     * This requires <a href="{@docRoot}/overview.html#defaults">additional JARs</a>
     * on the run time class path.
     */
    DefaultArchiveDetector ALL = new DefaultArchiveDetector();

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

    /**
     * Returns an archive driver for accessing archive files of the
     * given {@code type} or {@code null} if an appropriate archive driver
     * is unknown.
     * If the given {@code type} has been returned by {@link #getScheme},
     * then the return value is never {@code null}. In other words, if an
     * archive detector names a scheme for a given path, it must also provide
     * an appropriate archive driver for this scheme.
     *
     * @param  type the scheme to look up an appropriate archive driver for.
     * @return Returns an archive driver for accessing archive files of the
     *         given {@code type} or {@code null} if an appropriate archive
     *         driver is unknown.
     * @throws RuntimeException A subclass is thrown if loading or
     *         instantiating an archive driver class fails.
     */
    @Nullable ArchiveDriver<?> getDriver(@NonNull FsScheme type);
}
