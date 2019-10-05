/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import net.java.truecommons.cio.*;
import net.java.truecommons.io.DecoratingInputStream;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.io.DecoratingSeekableChannel;
import net.java.truecommons.io.PowerBuffer;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.mock.MockController;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.*;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.java.truecommons.cio.Entry.Access.*;
import static net.java.truecommons.cio.Entry.Size.DATA;
import static net.java.truecommons.cio.Entry.Size.STORAGE;
import static net.java.truecommons.cio.Entry.Type.FILE;
import static net.java.truecommons.cio.Entry.UNKNOWN;
import static net.java.truecommons.shed.Throwables.contains;
import static net.java.truevfs.kernel.spec.FsAccessOptions.NONE;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

/**
 * @param <E> The type of the archive entries.
 * @param <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class FsArchiveDriverTestSuite<E extends FsArchiveEntry, D extends FsArchiveDriver<E>>
        extends FsArchiveDriverTestBase<D> {

    private static final Logger logger = LoggerFactory.getLogger(FsArchiveDriverTestSuite.class);

    private static final FsNodeName name = FsNodeName.create(URI.create("archive"));

    private static final String US_ASCII_CHARACTERS;

    static {
        final StringBuilder builder = new StringBuilder(128);
        for (char c = 0; c < 128; c++) {
            builder.append(c);
        }
        US_ASCII_CHARACTERS = builder.toString();
    }

    private FsModel model;
    private FsController parent;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        // Order is important here!
        final FsTestConfig config = FsTestConfig.get();
        config.setDataSize(getMaxArchiveLength());
        config.setPool(null); // reset
        model = newArchiveModel();
        parent = newParentController(model.getParent());
        assert !UTF_8.equals(getArchiveDriver().getCharset()) || null == getUnencodableName() : "Bad test setup!";
    }

    /**
     * Returns an unencodable name or {@code null} if all characters are
     * encodable in entry names for this archive type.
     *
     * @return An unencodable name or {@code null} if all characters are
     * encodable in entry names for this archive type.
     */
    protected abstract @CheckForNull
    String getUnencodableName();

    @Test
    public void testCharsetMustNotBeNull() {
        assertThat(getArchiveDriver().getCharset(), notNullValue());
    }

    @Test
    public void testUnencodableCharacters() {
        final String name = getUnencodableName();
        if (null != name) {
            assertFalse(getArchiveDriver().getCharset().newEncoder().canEncode(name));
        }
    }

    @Test
    public void testAllUsAsciiCharactersMustBeEncodable() {
        getArchiveDriver().getCharset().newEncoder().canEncode(US_ASCII_CHARACTERS);
    }

    @Test
    public void testArchiveDriverProperty() {
        assertTrue(getArchiveDriver().isArchiveDriver());
    }

    @Test
    public void testIoPoolMustNotBeNull() {
        assertNotNull(getArchiveDriver().getPool());
    }

    @Test
    public void testIoPoolShouldBeConstant() {
        final IoBufferPool p1 = getArchiveDriver().getPool();
        final IoBufferPool p2 = getArchiveDriver().getPool();
        if (p1 != p2) {
            logger.warn("{} returns different I/O buffer pools upon multiple invocations of getPool()!", getArchiveDriver().getClass());
        }
    }

    /*@Test(expected = NullPointerException.class)
    public void testNewControllerMustNotTolerateNullModel() {
        getArchiveDriver().newController(newManager(), null, parent);
    }

    @Test(expected = NullPointerException.class)
    public void testNewControllerMustNotTolerateNullParent() {
        getArchiveDriver().newController(newManager(), model, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewControllerMustCheckParentMemberMatch1() {
        getArchiveDriver().newController(newManager(), model.getParent(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewControllerMustCheckParentMemberMatch2() {
        getArchiveDriver().newController(newManager(), model.getParent(), parent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewControllerMustCheckParentMemberMatch3() {
        getArchiveDriver().newController(newManager(), model, newController(model));
    }

    @Test
    public void testNewControllerMustNotReturnNull() {
        assertNotNull(getArchiveDriver().newController(newManager(), model, parent));
    }

    @Test
    public void testNewControllerMustMeetPostConditions() {
        final FsController<?> c = getArchiveDriver().newController(newManager(), model, parent);
        assertNotNull(c);
        assertEquals(model.getMountPoint(), c.getModel().getMountPoint());
        assertSame(parent, c.getParent());
    }*/

    @Test(expected = NullPointerException.class)
    public void testNewInputMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().newInput(null, NONE, parent, name);
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputMustNotTolerateNullParentController() throws IOException {
        getArchiveDriver().newInput(model, NONE, null, name);
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputMustNotTolerateNullEntryName() throws IOException {
        getArchiveDriver().newInput(model, NONE, parent, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputMustNotTolerateNullOptions() throws IOException {
        getArchiveDriver().newInput(model, null, parent, name);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().newOutput(null, NONE, parent, name, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputMustNotTolerateNullParentController() throws IOException {
        getArchiveDriver().newOutput(model, NONE, null, name, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputMustNotTolerateNullEntryName() throws IOException {
        getArchiveDriver().newOutput(model, NONE, parent, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputMustNotTolerateNullOptions() throws IOException {
        getArchiveDriver().newOutput(model, null, parent, name, null);
    }

    @Test
    public void testEmptyRoundTripPersistence() throws IOException {
        roundTripPersistence(0);
    }

    @Test
    public void testStandardRoundTripPersistence() throws IOException {
        roundTripPersistence(getNumEntries());
    }

    private void roundTripPersistence(int numEntries) throws IOException {
        output(numEntries);
        input(numEntries);
    }

    private void output(final int numEntries) throws IOException {
        final OutputService<E> service = getArchiveDriver().newOutput(model, NONE, parent, name, null);
        try {
            final Closeable[] streams = new Closeable[numEntries];
            try {
                for (int i = 0; i < streams.length; i++) {
                    streams[i] = output(service, i);
                }
            } finally {
                close(streams);
            }
            check(service, numEntries);
        } finally {
            final IOException expected = new IOException();
            trigger(TestCloseable.class, expected);
            try {
                // This call may succeed if the archive driver is not using the
                // parent controller (i.e. the MockArchiveDriver).
                service.close();
                //fail();
            } catch (final IOException got) {
                if (!contains(got, expected)) {
                    throw got;
                }
            } finally {
                clear(TestCloseable.class);
            }
            service.close();
        }
    }

    @CreatesObligation
    private OutputStream output(final OutputService<E> service, final int i) throws IOException {
        final String name = name(i);
        final E entry = newEntry(name);
        final OutputSocket<E> output = service.output(entry);
        assertSame(entry, output.target());

        assertNull(service.entry(name));
        assertEquals(i, service.size());

        boolean failure = true;
        final OutputStream out = output.stream(null);
        try {
            assertSame(entry, service.entry(name));
            assertEquals(i + 1, service.size());
            out.write(getData());
            failure = false;
        } finally {
            if (failure) out.close();
        }
        return out;
    }

    private void input(final int numEntries) throws IOException {
        final InputService<E> service = getArchiveDriver().newInput(model, NONE, parent, name);
        try {
            check(service, numEntries);
            final Closeable[] streams = new Closeable[numEntries];
            try {
                for (int i = 0; i < streams.length; i++) {
                    input(service, i).close(); // first attempt
                    streams[i] = input(service, i); // second attempt
                }
            } finally {
                close(streams);
            }
        } finally {
            final IOException expected = new IOException();
            trigger(TestCloseable.class, expected);
            try {
                // This call may succeed if the archive driver is not using the
                // parent controller (i.e. the MockArchiveDriver) or has been
                // reading the archive file upfront (e.g. the TAR driver).
                service.close();
                //fail();
            } catch (final IOException got) {
                if (!contains(got, expected)) {
                    throw got;
                }
            } finally {
                clear(TestCloseable.class);
            }
            service.close();
        }
    }

    private InputStream input(final InputService<E> service, final int i)
            throws IOException {
        final InputSocket<E> input = service.input(name(i));

        {
            final PowerBuffer<?> buf = PowerBuffer.allocate(getDataLength());
            SeekableByteChannel channel;
            try {
                channel = input.channel(null);
            } catch (final UnsupportedOperationException ex) {
                channel = null;
                logger.trace(input.getClass().getName(), ex);
            }
            if (null != channel) {
                try {
                    buf.load(channel);
                    assertEquals(channel.position(), channel.size());
                } finally {
                    channel.close();
                }
                channel.close(); // expect no issues
                assertTrue(Arrays.equals(getData(), buf.array()));
            }
        }

        {
            final byte[] buf = new byte[getDataLength()];
            boolean failure = true;
            final DataInputStream in = new DataInputStream(input.stream(null));
            try {
                in.readFully(buf);
                assertTrue(Arrays.equals(getData(), buf));
                assertEquals(-1, in.read());
                failure = false;
            } finally {
                if (failure) {
                    in.close();
                }
            }
            return in;
        }
    }

    private static void close(final Closeable[] resources) throws IOException {
        IOException ex = null;
        for (final Closeable resource : resources) {
            if (null == resource) {
                continue;
            }
            try {
                try {
                    resource.close();
                } finally {
                    resource.close(); // must be idempotent on side effects
                }
            } catch (final IOException ex2) {
                if (null != ex) {
                    ex.addSuppressed(ex2);
                } else {
                    ex = ex2;
                }
            }
        }
        if (null != ex) {
            throw ex;
        }
    }

    private <E extends FsArchiveEntry> void check(
            final Container<E> container,
            final int numEntries) {
        assertEquals(numEntries, container.size());
        final Iterator<E> it = container.iterator();
        for (int i = 0; i < numEntries; i++) {
            final E e = it.next();
            assertNotNull(e);
            assertEquals(name(i), e.getName());
            assertSame(FILE, e.getType());
            assertEquals(getDataLength(), e.getSize(DATA));
            final long storage = e.getSize(STORAGE);
            assertTrue(UNKNOWN == storage || getDataLength() <= storage); // random data is not compressible!
            assertTrue(UNKNOWN != e.getTime(WRITE));
            try {
                it.remove();
                fail();
            } catch (UnsupportedOperationException expected) {
            }
            assertSame(e, container.entry(e.getName()));
        }
        assertFalse(it.hasNext());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException expected) {
        }
        try {
            it.remove();
            fail();
        } catch (UnsupportedOperationException expected) {
        }
        assertEquals(numEntries, container.size());
    }

    private E newEntry(final String name) throws CharConversionException {
        final E e = getArchiveDriver().newEntry(name, Entry.Type.FILE, null);
        assertNotNull(e);
        assertEquals(name, e.getName());
        assertSame(FILE, e.getType());
        assertTrue(UNKNOWN == e.getSize(DATA));
        assertTrue(UNKNOWN == e.getSize(STORAGE));
        assertTrue(UNKNOWN == e.getTime(WRITE));
        assertTrue(UNKNOWN == e.getTime(READ));
        assertTrue(UNKNOWN == e.getTime(CREATE));
        return e;
    }

    private static String name(int i) {
        return Integer.toString(i);
    }

    private MockController newParentController(final FsModel model) {
        final FsModel pm = model.getParent();
        final FsController pc = null == pm ? null : newParentController(pm);
        return new ParentController(model, pc);
    }

    private FsModel newArchiveModel() {
        final FsModel parent = newNonArchiveModel();
        return newModel(
                FsMountPoint.create(URI.create(
                        "scheme:" + parent.getMountPoint() + name + "!/")),
                parent);
    }

    private FsModel newNonArchiveModel() {
        return newModel(
                FsMountPoint.create(URI.create("file:/")),
                null);
    }

    protected FsModel newModel(FsMountPoint mountPoint, @CheckForNull FsModel parent) {
        return new FsTestModel(mountPoint, parent);
    }

    private int getMaxArchiveLength() {
        return getNumEntries() * getDataLength() * 4 / 3; // account for archive type specific overhead
    }

    private Throwable trigger(Class<?> from, Throwable toThrow) {
        return getThrowControl().trigger(from, toThrow);
    }

    private Throwable clear(Class<?> from) {
        return getThrowControl().clear(from);
    }

    private void checkAllExceptions(final Object thiz) throws IOException {
        final FsThrowManager ctl = getThrowControl();
        ctl.check(thiz, IOException.class);
        ctl.check(thiz, RuntimeException.class);
        ctl.check(thiz, Error.class);
    }

    private FsThrowManager getThrowControl() {
        return FsTestConfig.get().getThrowControl();
    }

    private int getNumEntries() {
        return FsTestConfig.get().getNumEntries();
    }

    private final class ParentController extends MockController {

        ParentController(FsModel model, @CheckForNull FsController parent) {
            super(model, parent, FsTestConfig.get());
        }

        @Override
        public InputSocket<?> input(
                final BitField<FsAccessOption> options,
                final FsNodeName name) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(options);

            final class Input extends DecoratingInputSocket<Entry> {

                Input() {
                    super(ParentController.super.input(options, name));
                }

                @Override
                public InputStream stream(OutputSocket<? extends Entry> peer) throws IOException {
                    return new TestInputStream(socket().stream(peer));
                }

                @Override
                public SeekableByteChannel channel(OutputSocket<? extends Entry> peer) throws IOException {
                    return new TestSeekableChannel(socket().channel(peer));
                }
            }

            return new Input();
        }

        @Override
        public OutputSocket<?> output(
                final BitField<FsAccessOption> options,
                final FsNodeName name,
                final @CheckForNull Entry template) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(options);

            final class Output extends DecoratingOutputSocket<Entry> {

                Output() {
                    super(ParentController.super.output(options, name, template));
                }

                @Override
                public SeekableByteChannel channel(InputSocket<? extends Entry> peer) throws IOException {
                    return new TestSeekableChannel(socket().channel(peer));
                }

                @Override
                public OutputStream stream(InputSocket<? extends Entry> peer) throws IOException {
                    return new TestOutputStream(socket().stream(peer));
                }
            }

            return new Output();
        }
    }

    @SuppressWarnings("MarkerInterface")
    private interface TestCloseable extends Closeable {
    }

    private final class TestInputStream extends DecoratingInputStream implements TestCloseable {

        TestInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            checkAllExceptions(this);
            in.close();
        }
    }

    private final class TestOutputStream extends DecoratingOutputStream implements TestCloseable {

        TestOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            checkAllExceptions(this);
            out.close();
        }
    }

    private final class TestSeekableChannel extends DecoratingSeekableChannel implements TestCloseable {

        TestSeekableChannel(SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public void close() throws IOException {
            checkAllExceptions(this);
            channel.close();
        }
    }
}
