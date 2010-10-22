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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.archive.controller.Controllers;

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
     * file systems which have been updated by a call to
     * {@link Controllers#sync(URI, ExceptionBuilder, BitField)}.
     * <p>
     * Please note that this method counts only input from top level file
     * systems which have been touched, i.e. archive files which are actually
     * updated and are not enclosed in other file systems and hence are present
     * in their host file system.
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
     * @see Controllers#sync(URI, ExceptionBuilder, BitField)
     */
    long getSyncTotalByteCountRead();
    
    /**
     * Returns the total number of bytes written to all <em>non-enclosed</em>
     * file systems which have been updated by a call to
     * {@link Controllers#sync(URI, ExceptionBuilder, BitField)}.
     * <p>
     * Please note that this method counts only output to top level file
     * systems which have been touched, i.e. archive files which are actually
     * updated and are not enclosed in other file systems and hence are present
     * in their host file system.
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
     * @see Controllers#sync(URI, ExceptionBuilder, BitField)
     */
    long getSyncTotalByteCountWritten();

    /**
     * Returns the total number of file systems processed.
     */
    int getFileSystemsTotal();
    
    /**
     * Returns the number of file systems which have been touched and
     * need synchronization by calling
     * {@link Controllers#sync(URI, ExceptionBuilder, BitField)}.
     * Note that you should <em>not</em> use the returned value to call this
     * method conditionally - this is unreliable!
     * Instead, you should always call one of those methods unconditionally.
     */
    int getFileSystemsTouched();

    /**
     * Returns the total number of top level file systems processed.
     */
    int getTopLevelFileSystemsTotal();
    
    /**
     * Returns the number of top level file systems which have been touched and
     * need synchronization by calling
     * {@link Controllers#sync(URI, ExceptionBuilder, BitField)}.
     * Note that you should <em>not</em> use the returned value to call this
     * method conditionally - this is unreliable!
     * Instead, you should always call one of those methods unconditionally.
     */
    int getTopLevelFileSystemsTouched();
}
