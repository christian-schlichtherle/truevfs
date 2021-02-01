/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.cio;

import global.namespace.truevfs.comp.cio.OutputSocket;
import global.namespace.truevfs.kernel.api.mock.MockArchive;
import global.namespace.truevfs.kernel.api.mock.MockArchiveDriverEntry;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import static global.namespace.truevfs.comp.cio.Entry.Type.FILE;
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
            socket.stream(Optional.empty());
            assertThat(service.entry(name), notNullValue());
            assertThat(service.isBusy(), is(true));
        }
        assertThat(service.size(), is(names.length));
        final Iterator<MockArchiveDriverEntry> it = service.iterator();
        for (final String name : names)
            assertThat(it.next().getName(), equalTo(name));
    }
}
