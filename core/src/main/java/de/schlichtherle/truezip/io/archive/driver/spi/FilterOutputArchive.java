/*
 * Copyright 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.driver.spi;

import de.schlichtherle.truezip.io.archive.controller.OutputArchiveMetaData;
import de.schlichtherle.truezip.io.archive.driver.ArchiveOutputStreamSocket;
import de.schlichtherle.truezip.io.archive.driver.OutputArchive;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A decorator for output archives.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <AE> The run time type of the archive entries in this container.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilterOutputArchive<AE extends ArchiveEntry>
extends FilterArchiveEntryContainer<AE>
implements OutputArchive<AE> {

    protected OutputArchive<AE> target;

    public FilterOutputArchive(OutputArchive<AE> target) {
        this.target = target;
    }

    @Override
    protected final OutputArchive<AE> getTarget() {
        return target;
    }

    @Override
    public ArchiveOutputStreamSocket<AE> getOutputStreamSocket(AE entry)
    throws FileNotFoundException {
        return getTarget().getOutputStreamSocket(entry);
    }

    @Override
    public OutputArchiveMetaData getMetaData() {
        return getTarget().getMetaData();
    }

    @Override
    public void setMetaData(OutputArchiveMetaData metaData) {
        getTarget().setMetaData(metaData);
    }

    @Override
    public void close() throws IOException {
        getTarget().close();
    }
}
