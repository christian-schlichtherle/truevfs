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
package de.schlichtherle.truezip.fs;

import java.net.URI;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ObservableModelTest {

    private FsObservableModel model;

    @Before
    public void setUp() {
        model = new FsObservableModel(
            new FsDefaultModel(FsMountPoint.create(URI.create("foo:/")), null));
    }

    @Test
    public void testAddRemoveFileSystemListeners() {
        try {
            model.addFsTouchedListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getFsTouchedListeners(), notNullValue());
        assertThat(model.getFsTouchedListeners().size(), is(0));

        final Listener listener1 = new Listener(model);
        model.addFsTouchedListener(listener1);
        assertThat(model.getFsTouchedListeners().size(), is(1));

        final Listener listener2 = new Listener(model);
        model.addFsTouchedListener(listener2);
        assertThat(model.getFsTouchedListeners().size(), is(2));

        model.getFsTouchedListeners().clear();
        assertThat(model.getFsTouchedListeners().size(), is(2));

        try {
            model.removeFsTouchedListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getFsTouchedListeners().size(), is(2));

        model.removeFsTouchedListener(listener1);
        model.removeFsTouchedListener(listener1);
        assertThat(model.getFsTouchedListeners().size(), is(1));

        model.removeFsTouchedListener(listener2);
        model.removeFsTouchedListener(listener2);
        assertThat(model.getFsTouchedListeners().size(), is(0));
    }

    @Test
    public void testNotifyFileSystemListeners() {
        final Listener listener1 = new Listener(model);
        final Listener listener2 = new Listener(model);

        model.setTouched(false);
        assertThat(listener1.changes, is(0));
        assertThat(listener2.changes, is(0));

        model.setTouched(true);
        assertThat(listener1.changes, is(1));
        assertThat(listener2.changes, is(1));

        model.setTouched(true);
        assertThat(listener1.changes, is(1));
        assertThat(listener2.changes, is(1));

        model.setTouched(false);
        assertThat(listener1.changes, is(2));
        assertThat(listener2.changes, is(2));
    }

    private static class Listener implements FsTouchedListener {
        final FsObservableModel model;
        int changes;

        @SuppressWarnings("LeakingThisInConstructor")
        Listener(final FsObservableModel model) {
            this.model = model;
            model.addFsTouchedListener(this);
        }

        @Override
        public void touchedChanged(FsEvent event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance((FsModel) model));
            changes++;
        }
    }
}
