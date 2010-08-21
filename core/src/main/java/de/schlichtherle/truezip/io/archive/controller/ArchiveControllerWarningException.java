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

package de.schlichtherle.truezip.io.archive.controller;


import java.io.IOException;

/**
 * Represents a chain of exceptions thrown to indicate an error condition which
 * does <em>not</em> incur loss of data and may be ignored.
 *
 * <p>Some public methods in this package operate on multiple archive files
 * consecutively. To ensure that all archive files are processed, they catch
 * any exception occuring throughout their processing of an archive file and
 * store it in an exception chain of this type before continuing with the next
 * archive file.
 * Finally, if all archive files have been processed and the exception chain
 * is not empty, it's reordered and thrown so that if its head is an instance
 * of {@code ArchiveControllerWarningException}, only instances of this class or its
 * subclasses are in the chain, but no instances of {@code ArchiveControllerException}
 * or its subclasses (except {@code ArchiveControllerWarningException}, of course).
 *
 * <p>This enables client applications to do a simple case distinction with a
 * try-catch-block like this to react selectively:</p>
 * <pre>{@code
 * try {
 *     ArchiveControllers.umount("", false, true, false, true, true);
 * } catch (ArchiveControllerWarningException warning) {
 *     // Only warnings have occured and no data has been lost - ignore this.
 * } catch (ArchiveControllerException error) {
 *     // Some data has been lost - panic!
 *     error.printStackTrace();
 * }
 * }</pre>
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveControllerWarningException extends ArchiveControllerException {
    private static final long serialVersionUID = 2302357394858347366L;
    
    // TODO: Make this constructor package private!
    public ArchiveControllerWarningException(
            ArchiveControllerException priorException,
            String message) {
        super(priorException, message);
    }

    // TODO: Make this constructor package private!
    public ArchiveControllerWarningException(
            ArchiveControllerException priorException,
            String message,
            IOException cause) {
        super(priorException, message, cause);
    }

    // TODO: Make this constructor package private!
    public ArchiveControllerWarningException(
            ArchiveControllerException priorException,
            IOException cause) {
        super(priorException, cause);
    }
    
    @Override
    public int getPriority() {
        return -1;
    }
}
