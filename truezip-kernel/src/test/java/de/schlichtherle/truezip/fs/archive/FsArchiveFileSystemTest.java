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

import de.schlichtherle.truezip.util.UriBuilder;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveEntry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveEntryContainer;
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

    @Test
    public void testPopulation() throws Exception {
        final MockArchiveDriver driver = new MockArchiveDriver();
        final MockArchiveEntryContainer container = new MockArchiveEntryContainer();
        final Object[][] params = new Object[][] {
            { "sh:t", },
            { "foo/", },
            { "foo/bar", },
            { "bar", },
        };

        // Populate and check container.
        for (final Object[] param : params) {
            final String entryName = param[0].toString();
            final Type type = entryName.endsWith(SEPARATOR) ? DIRECTORY : FILE;
            final MockArchiveEntry
                    entry = driver.newEntry(entryName, type, null);
            container   .new Output()
                        .getOutputSocket(entry)
                        .newOutputStream()
                        .close();
            assertNotNull(container.getEntry(entryName));
        }
        assertEquals(params.length, container.getSize());

        // Populate and check file system.
        final FsArchiveFileSystem<MockArchiveEntry>
                fileSystem = FsArchiveFileSystem.newArchiveFileSystem(
                    driver, container, null, false);
        assert params.length <= fileSystem.getSize();
        assertNotNull(fileSystem.getEntry(ROOT));
        for (final Object[] param : params) {
            final FsEntryName entryName = new FsEntryName(
                    new UriBuilder().path(param[0].toString()).getUri(),
                    CANONICALIZE);
            final FsArchiveFileSystemEntry<MockArchiveEntry>
                    entry = fileSystem.getEntry(entryName);
            assertEquals(entryName.getPath(), entry.getName());
            assertEquals(param[0], entry.getEntry().getName());
        }
    }
}
