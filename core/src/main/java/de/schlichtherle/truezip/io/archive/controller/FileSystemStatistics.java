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
package de.schlichtherle.truezip.io.archive.controller;

/**
 * Provides statistics about the total set of archive files accessed.
 * Client applications should never implement this interface because (a)
 * there is no need to and (b) it may get extended over time.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface FileSystemStatistics {

    /**
     * Returns the total number of bytes read from all <em>non-enclosed</em>
     * archive files which have been updated by a call to
     * {@link FileSystemControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)}.
     * <p>
     * Please note that this method counts input from top level archive
     * files which require an update only, i.e. archive files which are
     * actually updated and are not enclosed in other archive
     * files and hence are present in the real file system.
     * <p>
     * This method is intended to be used for progress monitors and is a rough
     * indicator about what is going on inside the TrueZIP API.
     * The return value will be reset automatically where required,
     * so if this value is going to {@code 0} again you know that a knew
     * update cycle has begun.
     * Other than this, you should not rely on its actual value.
     * <p>
     * For an example how to use this please refer to the source
     * code for {@code nzip.ProgressMonitor} in the base package.
     *
     * @see FileSystemControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)
     */
    long getSyncTotalByteCountRead();
    
    /**
     * Returns the total number of bytes written to all <em>non-enclosed</em>
     * archive files which have been updated by a call to
     * {@link FileSystemControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)}.
     * <p>
     * Please note that this method counts output to top level archive
     * files which require an update only, i.e. archive files which are
     * actually updated and are not enclosed in other archive
     * files and hence are present in the real file system.
     * <p>
     * This method is intended to be used for progress monitors and is a rough
     * indicator about what is going on inside the TrueZIP API.
     * The return value will be reset automatically where required,
     * so if this value is going to {@code 0} again you know that a knew
     * update cycle has begun.
     * Other than this, you should not rely on its actual value.
     * <p>
     * For an example how to use this please refer to the source
     * code for {@code nzip.ProgressMonitor} in the base package.
     *
     * @see FileSystemControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)
     */
    long getSyncTotalByteCountWritten();

    /**
     * Returns the total number of archives operated by this package.
     */
    int getFileSystemsTotal();
    
    /**
     * Returns the number of archives which have been changed and
     * hence need to be updated when calling
     * {@link FileSystemControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)}.
     * Note that you should <em>not</em> use the returned value to call this
     * method conditionally - this is unreliable!
     * Instead, you should always call one of those methods unconditionally.
     */
    int getFileSystemsTouched();

    /**
     * Returns the total number of top level archives operated by this package.
     */
    int getTopLevelFileSystemsTotal();
    
    /**
     * Returns the number of top level archives which have been changed and
     * hence need to be updated when calling
     * {@link FileSystemControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)}.
     * Note that you should <em>not</em> use the returned value to call this
     * method conditionally - this is unreliable!
     * Instead, you should always call one of those methods unconditionally.
     */
    int getTopLevelFileSystemsTouched();
}
