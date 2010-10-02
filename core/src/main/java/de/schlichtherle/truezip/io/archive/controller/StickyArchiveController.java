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

import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.REASSEMBLE;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class StickyArchiveController<AE extends ArchiveEntry>
extends FilterArchiveController<AE> {

    StickyArchiveController(
            ArchiveModel<AE> model,
            ArchiveController<AE> controller) {
        super(model, controller);
    }

    private void stick() {
        ArchiveControllers.scheduleSync(getMountPoint(), true);
    }

    private void unstick() {
        ArchiveControllers.scheduleSync(getMountPoint(), false);
    }

    @Override
    public Icon getOpenIcon()
    throws FalsePositiveEntryException {
        return controller.getOpenIcon();
    }

    @Override
    public Icon getClosedIcon()
    throws FalsePositiveEntryException {
        return controller.getClosedIcon();
    }

    @Override
    public boolean isReadOnly()
    throws FalsePositiveEntryException {
        return controller.isReadOnly();
    }

    @Override
    public boolean isReadable(String path)
    throws FalsePositiveEntryException {
        return controller.isReadable(path);
    }

    @Override
    public boolean isWritable(String path)
    throws FalsePositiveEntryException {
        return controller.isWritable(path);
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        controller.setReadOnly(path);
        stick();
    }

    @Override
    public Entry<?> getEntry(String path)
    throws FalsePositiveEntryException {
        Entry<?> entry = controller.getEntry(path);
        stick();
        return entry;
    }

    @Override
    public void setTime(String path, BitField types, long value)
    throws IOException {
        controller.setTime(path, types, value);
        stick();
    }

    @Override
    public CommonInputSocket<?> newInputSocket(String path)
    throws IOException {
        CommonInputSocket<?> input = controller.newInputSocket(path);
        stick(); // FIXME: Make this redundant!
        return input;
    }

    @Override
    public CommonOutputSocket<?> newOutputSocket(String path, BitField options)
    throws IOException {
        CommonOutputSocket<?> output = controller.newOutputSocket(path, options);
        stick();
        return output;
    }

    @Override
    public void mknod(  String path, Type type, CommonEntry template,
                        BitField options)
    throws IOException {
        controller.mknod(path, type, template, options);
        stick();
    }

    @Override
    public void unlink(String path, BitField options)
    throws IOException {
        controller.unlink(path, options);
        stick();
    }

    @Override
    public void sync(ArchiveSyncExceptionBuilder builder, BitField<SyncOption> options)
    throws ArchiveSyncException {
        controller.sync(builder, options);
        if (options.get(REASSEMBLE))
            unstick();
    }
}
