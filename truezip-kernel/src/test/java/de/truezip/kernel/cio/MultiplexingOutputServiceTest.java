/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import static de.truezip.kernel.cio.Entry.Type.FILE;
import de.truezip.kernel.mock.MockArchive;
import de.truezip.kernel.mock.MockArchiveDriverEntry;
import java.io.IOException;
import java.util.Iterator;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class MultiplexingOutputServiceTest {

    private final MockArchive archive = MockArchive.create(null);
    private final MultiplexingOutputService<MockArchiveDriverEntry>
            service = new MultiplexingOutputService<>(archive.getIOPool(),
                archive.newOutputService());

    @Test
    public void testMultiplexing() throws IOException {
        assertThat(service.isBusy(), is(false));
        final String[] names = { "foo", "bar", "baz" };
        for (final String name : names) {
            final OutputSocket<MockArchiveDriverEntry> socket = service
                    .output(new MockArchiveDriverEntry(name, FILE));
            assertThat(service.entry(name), nullValue());
            socket.stream();
            assertThat(service.entry(name), notNullValue());
            assertThat(service.isBusy(), is(true));
        }
        assertThat(service.size(), is(names.length));
        final Iterator<MockArchiveDriverEntry> it = service.iterator();
        for (final String name : names) {
            assertThat(it.next().getName(), equalTo(name));
        }
    }
}
