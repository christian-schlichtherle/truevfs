/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.driver.mock.MockArchive;
import global.namespace.truevfs.driver.mock.MockArchiveDriverEntry;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import static global.namespace.truevfs.comp.cio.Entry.Type.FILE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Schlichtherle
 */
public class MultiplexingOutputContainerTest {

    private final MockArchive archive = MockArchive.create(null);
    private final MultiplexingOutputContainer<MockArchiveDriverEntry>
            service = new MultiplexingOutputContainer<>(archive.getPool(),
                archive.newOutputService());

    @Test
    public void testMultiplexing() throws IOException {
        assertThat(service.isBusy(), is(false));
        final String[] names = { "foo", "bar", "baz" };
        for (final String name : names) {
            final OutputSocket<MockArchiveDriverEntry> socket = service
                    .output(new MockArchiveDriverEntry(name, FILE));
            assertFalse(service.entry(name).isPresent());
            socket.stream(Optional.empty());
            assertTrue(service.entry(name).isPresent());
            assertThat(service.isBusy(), is(true));
        }
        assertThat(service.entries().size(), is(names.length));
        final Iterator<MockArchiveDriverEntry> it = service.entries().iterator();
        for (final String name : names)
            assertThat(it.next().getName(), equalTo(name));
    }
}
