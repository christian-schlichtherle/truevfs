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

package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.archive.driver.registry.GlobalArchiveDriverRegistry;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;

/**
 * Detects archive files solely by scanning file paths -
 * usually by testing for file name suffixes like <i>.zip</i> or the
 * like.
 * If the method {@link #getArchiveDriver(String)} detects an archive file, it
 * returns an instance of the {@link ArchiveDriver} interface for subsequent
 * access to it.
 * <p>
 * {@code ArchiveDetector} instances are assigned to {@code File}
 * instances in the following way:
 * <ol>
 * <li>If an archive detector is explicitly provided as a parameter to the
 *     constructor of the {@code File} class or any other method which
 *     creates {@code File} instances (e.g. {@code listFiles(*)}),
 *     then this archive detector is used.
 * <li>Otherwise, the archive detector returned by
 *     {@link File#getDefaultArchiveDetector} is used.
 *     This is initially set to the predefined instance {@link #DEFAULT}.
 *     Both the class property and the predefined instance can be customized.
 * </ol>
 * <p>
 * An archive file which has been recognized by an {@code ArchiveDetector} is
 * said to be a <i>prospective archive file</i>.
 * On the first read or write access to a prospective archive file, TrueZIP
 * checks its <i>true state</i> in cooperation with the {@link ArchiveDriver}.
 * If the true state of the file turns out to be actually a directory or not
 * to be compatible to the archive file format, it's said to be a <i>false
 * positive</i> archive file.
 * TrueZIP implements the appropriate behavior for all read or write
 * operations according to the true state.
 * Thanks to this design, TrueZIP detects and handles all kinds of false
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
 * @see ArchiveDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveDetector
        extends de.schlichtherle.truezip.io.archive.detector.ArchiveDetector,
                FileFactory {

    /**
     * Never recognizes archive files in a path.
     * This can be used as the end of a chain of
     * {@code DefaultArchiveDetector} instances or if archive files
     * shall be treated like ordinary files rather than (virtual) directories.
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    DefaultArchiveDetector NULL = new DefaultArchiveDetector(""); // or null

    /**
     * Recognizes the archive file suffixes registered in the global archive
     * driver registry which have been defined by the key {@code DEFAULT}
     * in the configuration file(s).
     * <p>
     * If only TrueZIP's original configuration file is used, then this is
     * defined so that no additional JARs are required on the runtime class
     * path.
     * Please check the <a href="{@docRoot}/overview.html#defaults">Overview</a>
     * to see which archive file suffixes are detected by default.
     *
     * @see GlobalArchiveDriverRegistry
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    DefaultArchiveDetector DEFAULT = new DefaultArchiveDetector(
            GlobalArchiveDriverRegistry.INSTANCE.defaultSuffixes);

    /**
     * Recognizes all archive file suffixes registered in the global archive
     * driver registry by the configuration file(s).
     * <p>
     * This requires <a href="{@docRoot}/overview.html#defaults">additional JARs</a>
     * on the runtime class path.
     *
     * @see GlobalArchiveDriverRegistry
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    DefaultArchiveDetector ALL = new DefaultArchiveDetector(
            GlobalArchiveDriverRegistry.INSTANCE.allSuffixes);
}
