/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.cio;

import net.java.truecommons.cio.OutputSocket;
import net.java.truevfs.kernel.spec.mock.MockArchive;
import net.java.truevfs.kernel.spec.mock.MockArchiveDriverEntry;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static net.java.truecommons.cio.Entry.Type.FILE;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Christian Schlichtherle
 */
public class MultiplexingOutputServiceTest {

    private final MockArchive archive = MockArchive.create(null);
    private final MultiplexingOutputService<MockArchiveDriverEntry>
            service = new MultiplexingOutputService<>(archive.getPool(),
                archive.newOutputService());

    @Test
    public void testMultiplexing() throws IOException {
        assertThat(service.isBusy(), is(false));
        final String[] names = { "foo", "bar", "baz" };
        for (final String name : names) {
            final OutputSocket<MockArchiveDriverEntry> socket = service
                    .output(new MockArchiveDriverEntry(name, FILE));
            assertThat(service.entry(name), nullValue());
            socket.stream(null);
            assertThat(service.entry(name), notNullValue());
            assertThat(service.isBusy(), is(true));
        }
        assertThat(service.size(), is(names.length));
        final Iterator<MockArchiveDriverEntry> it = service.iterator();
        for (final String name : names)
            assertThat(it.next().getName(), equalTo(name));
    }
}
