/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.fs;

import de.truezip.kernel.TestConfig;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import static de.truezip.kernel.cio.Entry.Type.FILE;
import de.truezip.kernel.fs.FsArchiveEntry;
import de.truezip.kernel.fs.FsCovariantEntry;
import de.truezip.kernel.addr.FsEntryName;
import static de.truezip.kernel.addr.FsEntryName.ROOT;
import static de.truezip.kernel.addr.FsEntryName.SEPARATOR;
import de.truezip.kernel.fs.mock.MockArchive;
import de.truezip.kernel.fs.mock.MockArchiveDriver;
import de.truezip.kernel.fs.mock.MockArchiveDriverEntry;
import de.truezip.kernel.util.UriBuilder;
import java.util.TooManyListenersException;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public final class FsArchiveFileSystemTest {

    @Before
    public void setUp() {
        TestConfig.push();
    }

    @After
    public void tearDown() {
        TestConfig.pop();
    }

    @Test
    public void testListeners() throws TooManyListenersException {
        final FsArchiveFileSystem<?>
                fs = FsArchiveFileSystem.newEmptyFileSystem(
                    new MockArchiveDriver());

        try {
            fs.addFsArchiveFileSystemTouchListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(fs.getFsArchiveFileSystemTouchListeners(), notNullValue());
        assertThat(fs.getFsArchiveFileSystemTouchListeners().length, is(0));

        final Listener listener1 = new Listener(fs);
        fs.addFsArchiveFileSystemTouchListener(listener1);
        assertThat(fs.getFsArchiveFileSystemTouchListeners().length, is(1));

        try {
            fs.addFsArchiveFileSystemTouchListener(new Listener(fs));
            fail();
        } catch (TooManyListenersException expected) {
        }
        assertThat(fs.getFsArchiveFileSystemTouchListeners().length, is(1));

        try {
            fs.removeFsArchiveFileSystemTouchListener(null);
            fail();
        } catch (NullPointerException expected) {
        }
        assertThat(fs.getFsArchiveFileSystemTouchListeners().length, is(1));

        fs.removeFsArchiveFileSystemTouchListener(listener1);
        fs.removeFsArchiveFileSystemTouchListener(listener1);
        assertThat(fs.getFsArchiveFileSystemTouchListeners().length, is(0));
    }

    private static class Listener
    implements FsArchiveFileSystemTouchListener<FsArchiveEntry> {
        final FsArchiveFileSystem<?> fileSystem;

        Listener(final FsArchiveFileSystem<?> fileSystem) {
            this.fileSystem = fileSystem;
        }

        @Override
        public void beforeTouch(FsArchiveFileSystemEvent<?> event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance((Object) fileSystem));
        }

        @Override
        public void afterTouch(FsArchiveFileSystemEvent<?> event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance((Object) fileSystem));
        }
    }

    @Test
    @SuppressWarnings("AssignmentToForLoopParameter")
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
        final TestConfig config = TestConfig.get();
        config.setNumEntries(paramss.length);
        final MockArchive archive = MockArchive.create(config);
        final MockArchiveDriver driver = new MockArchiveDriver(config);
        for (final String[] params : paramss) {
            final String aen = params[0];
            final Type type = aen.endsWith(SEPARATOR) ? DIRECTORY : FILE;
            final MockArchiveDriverEntry ae = driver.newEntry(aen, type, null);
            assertEquals(aen, ae.getName());
            archive   .newOutputService()
                        .getOutputSocket(ae)
                        .newOutputStream()
                        .close();
            assertSame(ae, archive.getEntry(aen));
        }
        assertEquals(paramss.length, archive.size());

        // Populate file system.
        final FsArchiveFileSystem<MockArchiveDriverEntry>
                fileSystem = FsArchiveFileSystem.newPopulatedFileSystem(
                    driver, archive, null, false);

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
            for (FsCovariantEntry<MockArchiveDriverEntry> ce : fileSystem)
                for (MockArchiveDriverEntry ae : ce.getEntries())
                    if (aen.equals(ae.getName()))
                        continue params;
            assert false : "No entry found with this name: " + aen;
        }
    }
}