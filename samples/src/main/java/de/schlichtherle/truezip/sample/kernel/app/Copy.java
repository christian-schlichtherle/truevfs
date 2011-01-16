/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.sample.kernel.app;

import de.schlichtherle.truezip.fs.FsDefaultDriver;
import de.schlichtherle.truezip.fs.FsDriverContainer;
import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsSyncExceptionBuilder;
import de.schlichtherle.truezip.fs.FsUriModifier;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Copies the contents of the first URI to the second URI.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Copy {

    public static void main(String[] args)
    throws IOException, URISyntaxException {
        // Obtain a manager for file system controller life cycle management.
        FsManager manager = new FsDefaultManager();

        // Search the class path for the set of all supported file system
        // drivers and build a federating file system driver from it.
        FsCompositeDriver driver = new FsDefaultDriver(
                FsDriverContainer.SINGLETON);

        // Resolve the source socket.
        // Note that an absolute URI is required, so we may need to use the
        // File class as a helper.
        URI srcUri = new URI(args[0]);
        srcUri = srcUri.isAbsolute() ? srcUri : new File(args[0]).toURI();
        FsPath srcPath = new FsPath(srcUri, FsUriModifier.CANONICALIZE);
        InputSocket<?> srcSocket = manager
                .getController(     srcPath.getMountPoint(), driver)
                .getInputSocket(    srcPath.getEntryName(),
                                    BitField.noneOf(FsInputOption.class));

        // Resolve the destination socket. Again, we need an absolute URI.
        URI dstUri = new URI(args[1]);
        dstUri = dstUri.isAbsolute() ? dstUri : new File(args[1]).toURI();
        FsPath dstPath = new FsPath(dstUri, FsUriModifier.CANONICALIZE);
        OutputSocket<?> dstSocket = manager
                .getController(     dstPath.getMountPoint(), driver)
                .getOutputSocket(   dstPath.getEntryName(),
                                    BitField.of(FsOutputOption.CREATE_PARENTS,
                                                FsOutputOption.EXCLUSIVE),
                                    srcSocket.getLocalTarget());

        // Copy the data.
        IOSocket.copy(srcSocket, dstSocket);

        // Commit all changes to federated file systems, if any were accessed.
        manager.umount();
    }
}
