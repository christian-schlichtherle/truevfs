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

import de.schlichtherle.truezip.io.filesystem.ComponentFileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemModel;
import de.schlichtherle.truezip.io.filesystem.FileSystemStatistics;

final class LiveFileSystemStatistics implements FileSystemStatistics {

    /** The singleton instance of this class. */
    public static final LiveFileSystemStatistics SINGLETON
            = new LiveFileSystemStatistics();

    private LiveFileSystemStatistics() {
    }

    @Override
    public long getSyncTotalByteCountRead() {
        return CountingReadOnlyFile.getTotal();
    }

    @Override
    public long getSyncTotalByteCountWritten() {
        return CountingOutputStream.getTotal();
    }

    @Override
    public int getFileSystemsTotal() {
        return Controllers.getControllers().size();
    }

    @Override
    public int getFileSystemsTouched() {
        int result = 0;
        for (ComponentFileSystemController<?> controller : Controllers.getControllers())
            if (controller.getModel().isTouched())
                result++;
        return result;
    }

    @Override
    public int getTopLevelFileSystemsTotal() {
        int result = 0;
        for (ComponentFileSystemController<?> controller : Controllers.getControllers())
            if (null == controller.getModel().getParent())
                result++;
        return result;
    }

    @Override
    public int getTopLevelFileSystemsTouched() {
        int result = 0;
        for (ComponentFileSystemController<?> controller : Controllers.getControllers()) {
            final FileSystemModel model = controller.getModel();
            if (null == model.getParent() && model.isTouched())
                result++;
        }
        return result;
    }
}
