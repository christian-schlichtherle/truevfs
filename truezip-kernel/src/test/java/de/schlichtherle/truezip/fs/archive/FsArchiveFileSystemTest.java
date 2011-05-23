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
    public void testListeners() {
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
        final String[][] paramss = new String[][] {
            // { $ARCHIVE_ENTRY_NAME [, $FILE_SYSTEM_ENTRY_NAME]* },
            { ".", "" }, // in case an adversary puts in a FILE entry with this name, then we could read it.
            { "\\t:st", null }, // illegal absolute Windows path
            { "/test", null }, // illegal absolute path
            { "f:ck" }, // strange, but legal
            { "täscht" }, // URI encoding test
            { "foo/", "foo" }, // directory
            { "foo/bar", "foo", "foo/bar" },
            { "foo//bar2", "foo", "foo/bar2" }, // strange, but legal
            { "foo/./bar3", "foo", "foo/bar3" }, // dito
            { "foo/../bar4", "bar4" }, // dito
            { "foo\\..\\bar5", "bar5" }, // dito from Windows
            { "./bar6", "bar6"}, // strange, but legal
            { ".\\bar7", "bar7" }, // dito from Windows
            { "../bar8", null }, // strange, but legal
            { "..\\bar9", null }, // dito from Windows
        };

        // Populate and check container.
        final MockArchiveEntryContainer container = new MockArchiveEntryContainer();
        final MockArchiveDriver driver = new MockArchiveDriver();
        for (final String[] params : paramss) {
            final String aen = params[0];
            final Type type = aen.endsWith(SEPARATOR) ? DIRECTORY : FILE;
            final MockArchiveEntry ae = driver.newEntry(aen, type, null);
            assertEquals(aen, ae.getName());
            container   .new Output()
                        .getOutputSocket(ae)
                        .newOutputStream()
                        .close();
            assertSame(ae, container.getEntry(aen));
        }
        assertEquals(paramss.length, container.getSize());

        // Populate file system.
        final FsArchiveFileSystem<MockArchiveEntry>
                fileSystem = FsArchiveFileSystem.newArchiveFileSystem(
                    driver, container, null, false);

        // Check file system.
        assert paramss.length <= fileSystem.getSize();
        assertNotNull(fileSystem.getEntry(ROOT));
        params: for (String[] params : paramss) {
            final String aen = params[0];
            if (1 == params.length)
                params = new String[] { aen, aen };

            // Test if a file system entry for any given name is present.
            for (int i = 1; i < params.length; i++) {
                final String cen = params[i];
                if (null == cen)
                    continue;
                final FsEntryName entryName = new FsEntryName(
                        new UriBuilder().path(cen).getUri());
                assertEquals(cen, entryName.getPath());
                assertEquals(cen, fileSystem.getEntry(entryName).getName());
            }

            // Test if an archive entry with a name matching path is present when iterating
            // the file system.
            for (FsCovariantEntry<MockArchiveEntry> ce : fileSystem)
                for (MockArchiveEntry ae : ce.getEntries())
                    if (aen.equals(ae.getName()))
                        continue params;
            assert false : "No entry found with this name: " + aen;
        }
    }
}
