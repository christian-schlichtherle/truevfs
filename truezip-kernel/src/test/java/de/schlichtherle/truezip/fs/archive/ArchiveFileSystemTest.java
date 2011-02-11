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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveFileSystemTest {

    @Test
    public void testAddRemoveArchiveFileSystemListeners() {
        final FsArchiveFileSystem<?> model
                = FsArchiveFileSystem.newArchiveFileSystem(new DummyArchiveDriver());

        try {
            model.addFsArchiveFileSystemTouchListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getFsArchiveFileSystemTouchListeners(), notNullValue());
        assertThat(model.getFsArchiveFileSystemTouchListeners().size(), is(0));

        final Listener listener1 = new Listener(model);
        model.addFsArchiveFileSystemTouchListener(listener1);
        assertThat(model.getFsArchiveFileSystemTouchListeners().size(), is(1));

        final Listener listener2 = new Listener(model);
        model.addFsArchiveFileSystemTouchListener(listener2);
        assertThat(model.getFsArchiveFileSystemTouchListeners().size(), is(2));

        model.getFsArchiveFileSystemTouchListeners().clear();
        assertThat(model.getFsArchiveFileSystemTouchListeners().size(), is(2));

        try {
            model.removeFsArchiveFileSystemTouchListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getFsArchiveFileSystemTouchListeners().size(), is(2));

        model.removeFsArchiveFileSystemTouchListener(listener1);
        model.removeFsArchiveFileSystemTouchListener(listener1);
        assertThat(model.getFsArchiveFileSystemTouchListeners().size(), is(1));

        model.removeFsArchiveFileSystemTouchListener(listener2);
        model.removeFsArchiveFileSystemTouchListener(listener2);
        assertThat(model.getFsArchiveFileSystemTouchListeners().size(), is(0));
    }

    private static class Listener
    implements FsArchiveFileSystemTouchListener<FsArchiveEntry> {
        final FsArchiveFileSystem<?> model;
        int before;
        int after;

        @SuppressWarnings("LeakingThisInConstructor")
        Listener(final FsArchiveFileSystem<?> model) {
            this.model = model;
            model.addFsArchiveFileSystemTouchListener(this);
        }

        @Override
        public void beforeTouch(FsArchiveFileSystemEvent<?> event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance((Object) model));
            before++;
        }

        @Override
        public void afterTouch(FsArchiveFileSystemEvent<?> event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance((Object) model));
            after++;
        }
    }
}
