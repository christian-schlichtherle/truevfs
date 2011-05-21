/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class FsArchiveFileSystemTest {

    @Test
    public void testAddRemoveArchiveFileSystemListeners() {
        final FsArchiveFileSystem<?> fileSystem
                = FsArchiveFileSystem.newArchiveFileSystem(new MockArchiveDriver());

        try {
            fileSystem.addFsArchiveFileSystemTouchListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(fileSystem.getFsArchiveFileSystemTouchListeners(), notNullValue());
        assertThat(fileSystem.getFsArchiveFileSystemTouchListeners().size(), is(0));

        final Listener listener1 = new Listener(fileSystem);
        fileSystem.addFsArchiveFileSystemTouchListener(listener1);
        assertThat(fileSystem.getFsArchiveFileSystemTouchListeners().size(), is(1));

        final Listener listener2 = new Listener(fileSystem);
        fileSystem.addFsArchiveFileSystemTouchListener(listener2);
        assertThat(fileSystem.getFsArchiveFileSystemTouchListeners().size(), is(2));

        fileSystem.getFsArchiveFileSystemTouchListeners().clear();
        assertThat(fileSystem.getFsArchiveFileSystemTouchListeners().size(), is(2));

        try {
            fileSystem.removeFsArchiveFileSystemTouchListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(fileSystem.getFsArchiveFileSystemTouchListeners().size(), is(2));

        fileSystem.removeFsArchiveFileSystemTouchListener(listener1);
        fileSystem.removeFsArchiveFileSystemTouchListener(listener1);
        assertThat(fileSystem.getFsArchiveFileSystemTouchListeners().size(), is(1));

        fileSystem.removeFsArchiveFileSystemTouchListener(listener2);
        fileSystem.removeFsArchiveFileSystemTouchListener(listener2);
        assertThat(fileSystem.getFsArchiveFileSystemTouchListeners().size(), is(0));
    }

    /*@Test
    public void testPopulation() {
        final FsArchiveFileSystem<?> fileSystem
                = FsArchiveFileSystem.newArchiveFileSystem(new MockArchiveDriver(), null, null, false);
        
    }*/

    private static class Listener
    implements FsArchiveFileSystemTouchListener<FsArchiveEntry> {
        final FsArchiveFileSystem<?> fileSystem;
        int before;
        int after;

        @SuppressWarnings("LeakingThisInConstructor")
        Listener(final FsArchiveFileSystem<?> fileSystem) {
            this.fileSystem = fileSystem;
            fileSystem.addFsArchiveFileSystemTouchListener(this);
        }

        @Override
        public void beforeTouch(FsArchiveFileSystemEvent<?> event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance((Object) fileSystem));
            before++;
        }

        @Override
        public void afterTouch(FsArchiveFileSystemEvent<?> event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance((Object) fileSystem));
            after++;
        }
    }
}
