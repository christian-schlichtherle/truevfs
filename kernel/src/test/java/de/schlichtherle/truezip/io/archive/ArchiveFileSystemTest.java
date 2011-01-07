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
package de.schlichtherle.truezip.io.archive;

import de.schlichtherle.truezip.io.archive.driver.DummyArchiveDriver;
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
        final ArchiveFileSystem<?> model
                = ArchiveFileSystem.newArchiveFileSystem(new DummyArchiveDriver());

        try {
            model.addArchiveFileSystemTouchListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getArchiveFileSystemTouchListeners(), notNullValue());
        assertThat(model.getArchiveFileSystemTouchListeners().size(), is(0));

        final Listener listener1 = new Listener(model);
        model.addArchiveFileSystemTouchListener(listener1);
        assertThat(model.getArchiveFileSystemTouchListeners().size(), is(1));

        final Listener listener2 = new Listener(model);
        model.addArchiveFileSystemTouchListener(listener2);
        assertThat(model.getArchiveFileSystemTouchListeners().size(), is(2));

        model.getArchiveFileSystemTouchListeners().clear();
        assertThat(model.getArchiveFileSystemTouchListeners().size(), is(2));

        try {
            model.removeArchiveFileSystemTouchListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getArchiveFileSystemTouchListeners().size(), is(2));

        model.removeArchiveFileSystemTouchListener(listener1);
        model.removeArchiveFileSystemTouchListener(listener1);
        assertThat(model.getArchiveFileSystemTouchListeners().size(), is(1));

        model.removeArchiveFileSystemTouchListener(listener2);
        model.removeArchiveFileSystemTouchListener(listener2);
        assertThat(model.getArchiveFileSystemTouchListeners().size(), is(0));
    }

    private static class Listener
    implements ArchiveFileSystemTouchListener<ArchiveEntry> {
        final ArchiveFileSystem<?> model;
        int before;
        int after;

        @SuppressWarnings("LeakingThisInConstructor")
        Listener(final ArchiveFileSystem<?> model) {
            this.model = model;
            model.addArchiveFileSystemTouchListener(this);
        }

        @Override
        public void beforeTouch(ArchiveFileSystemEvent<?> event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance((Object) model));
            before++;
        }

        @Override
        public void afterTouch(ArchiveFileSystemEvent<?> event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance((Object) model));
            after++;
        }
    }
}
