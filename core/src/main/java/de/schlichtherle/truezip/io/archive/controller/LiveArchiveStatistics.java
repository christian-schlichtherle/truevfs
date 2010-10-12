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

final class LiveArchiveStatistics implements ArchiveStatistics {

    /** The singleton instance of this class. */
    public static final LiveArchiveStatistics SINGLETON
            = new LiveArchiveStatistics();

    private LiveArchiveStatistics() {
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
    public int getArchivesTotal() {
        return FileSystemControllers.getControllers().size();
    }

    @Override
    public int getArchivesTouched() {
        int result = 0;
        for (final ArchiveController controller : FileSystemControllers.getControllers())
            if (controller.isTouched())
                result++;
        return result;
    }

    @Override
    public int getTopLevelArchivesTotal() {
        int result = 0;
        for (ArchiveController controller : FileSystemControllers.getControllers())
            if (controller.getModel().getEnclModel() == null)
                result++;
        return result;
    }

    @Override
    public int getTopLevelArchivesTouched() {
        int result = 0;
        for (final ArchiveController controller : FileSystemControllers.getControllers())
            if (controller.getModel().getEnclModel() == null && controller.isTouched())
                result++;
        return result;
    }
}
