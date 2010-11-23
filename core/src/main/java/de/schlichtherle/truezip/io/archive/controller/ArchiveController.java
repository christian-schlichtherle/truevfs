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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.filesystem.CompositeFileSystemController;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;

/**
 * @see     FileSystemController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveController<AE extends ArchiveEntry>
extends CompositeFileSystemController<AE> {

    @Override
    ArchiveModel getModel();

    @Override
    Entry<? extends AE> getEntry(String path)
    throws FileSystemException;

    @Override
    InputSocket<? extends AE> getInputSocket(   String path,
                                                BitField<InputOption> options);

    @Override
    OutputSocket<? extends AE> getOutputSocket( String path,
                                                BitField<OutputOption> options,
                                                CommonEntry template);
 }
