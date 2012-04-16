/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import static de.truezip.kernel.FsAccessOptions.NONE;
import static de.truezip.kernel.cio.Entry.Access.*;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import static de.truezip.kernel.cio.Entry.Size.STORAGE;
import static de.truezip.kernel.cio.Entry.Type.FILE;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.io.PowerBuffer;
import de.truezip.kernel.mock.MockController;
import de.truezip.kernel.util.BitField;
import static de.truezip.kernel.util.Throwables.contains;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.*;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @param  <E> The type of the archive entries.
 * @param  <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class FsArchiveDriverTestSuite<
        E extends FsArchiveEntry,
        D extends FsArchiveDriver<E>>
extends FsArchiveDriverTestBase<D> {

    private static final Logger
            logger = Logger.getLogger(FsArchiveDriverTestSuite.class.getName());

    private static final FsEntryName
            entry = FsEntryName.create(URI.create("archive"));

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String US_ASCII_CHARACTERS;
    static {
        final StringBuilder builder = new StringBuilder(128);
        for (char c = 0; c <= 127; c++)
            builder.append(c);
        US_ASCII_CHARACTERS = builder.toString();
    }

    private FsModel model;
    private FsController<?> parent;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        // Order is important here!
        final TestConfig config = getTestConfig();
        config.setDataSize(getMaxArchiveLength());
        config.setIOPoolProvider(null); // reset
        model = newArchiveModel();
        parent = newParentController(model.getParent());
        assert !UTF8.equals(getArchiveDriver().getCharset())
                || null == getUnencodableName() : "Bad test setup!";
    }

    /**
     * Returns an unencodable name or {@code null} if all characters are
     * encodable in entry names for this archive type.
     * 
     * @return An unencodable name or {@code null} if all characters are
     *         encodable in entry names for this archive type.
     */
    protected abstract @CheckForNull String getUnencodableName();

    @Test
    public void testCharsetMustNotBeNull() {
        assert null != getArchiveDriver().getCharset();
    }

    @Test
    public void testUnencodableCharacters() {
        final String name = getUnencodableName();
        if (null != name)
            assertFalse(getArchiveDriver().getCharset().newEncoder().canEncode(name));
    }

    @Test
    public void testAllUsAsciiCharactersMustBeEncodable()
    throws CharConversionException {
        getArchiveDriver().getCharset().newEncoder().canEncode(US_ASCII_CHARACTERS);
    }

    @Test
    public void testArchiveDriverMustBeFederated() {
        assertTrue(getArchiveDriver().isFederated());
    }

    @Test
    public void testIOPoolMustNotBeNull() {
        assertNotNull(getArchiveDriver().getIOPool());
    }

    @Test
    public void testIOPoolShouldBeConstant() {
        final IOPool<?> p1 = getArchiveDriver().getIOPool();
        final IOPool<?> p2 = getArchiveDriver().getIOPool();
        if (p1 != p2)
            logger.log(Level.WARNING, "{0} returns different I/O buffer pools upon multiple invocations of getPool()!", getArchiveDriver().getClass());
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputServiceMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().input(null, parent, entry, NONE);
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputServiceMustNotTolerateNullParentController() throws IOException {
        getArchiveDriver().input(model, null, entry, NONE);
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputServiceMustNotTolerateNullEntryName() throws IOException {
        getArchiveDriver().input(model, parent, null, NONE);
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputServiceMustNotTolerateNullOptions() throws IOException {
        getArchiveDriver().input(model, parent, entry, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputServiceMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().output(null, parent, entry, NONE, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputServiceMustNotTolerateNullParentController() throws IOException {
        getArchiveDriver().output(model, null, entry, NONE, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputServiceMustNotTolerateNullEntryName() throws IOException {
        getArchiveDriver().output(model, parent, null, NONE, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputServiceMustNotTolerateNullOptions() throws IOException {
        getArchiveDriver().output(model, parent, entry, null, null);
    }

    @Test
    public void testRoundTripPersistence() throws IOException {
        output();
        input();
    }

    private void output() throws IOException {
        final OutputService<E> service = getArchiveDriver()
                .output(model, parent, entry, NONE, null);
        try {
            final Closeable[] streams = new Closeable[getNumEntries()];
            try {
                for (int i = 0; i < streams.length; i++)
                    streams[i] = output(service, i);
            } finally {
                close(streams);
            }
            check(service);
        } finally {
            final IOException expected = new IOException();
            trigger(TestCloseable.class, expected);
            try {
                // This call may succeed if the archive driver is not using the
                // parent controller (i.e. the MockArchiveDriver).
                service.close();
                //fail();
            } catch (final IOException got) {
                if (!contains(got, expected))
                    throw got;
            } finally {
                clear(TestCloseable.class);
            }
            service.close();
        }
    }

    @CreatesObligation
    private OutputStream output(final OutputService<E> service, final int i)
    throws IOException {
        final String name = name(i);
        final E entry = newEntry(name);
        final OutputSocket<? extends E> output = service.output(entry);
        assertSame(entry, output.localTarget());

        assertNull(service.entry(name));
        assertEquals(i, service.size());

        boolean failure = true;
        final OutputStream out = output.stream();
        try {
            assertSame(entry, service.entry(name));
            assertEquals(i + 1, service.size());
            out.write(getData());
            failure = false;
        } finally {
            if (failure)
                out.close();
        }
        return out;
    }

    private void input() throws IOException {
        final InputService<E> service = getArchiveDriver()
                .input(model, parent, entry, NONE);
        try {
            check(service);
            final Closeable[] streams = new Closeable[getNumEntries()];
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
                if (!contains(got, expected))
                    throw got;
            } finally {
                clear(TestCloseable.class);
            }
            service.close();
        }
    }

    private InputStream input(final InputService<E> service, final int i)
    throws IOException {
        final InputSocket<? extends E> input = service.input(name(i));

        {
            final PowerBuffer buf = PowerBuffer.allocate(getDataLength());
            SeekableByteChannel channel;
            try {
                channel = input.channel();
            } catch (final UnsupportedOperationException ex) {
                channel = null;
                logger.log(Level.FINE,
                        input.getClass()
                            + " does not support newChannel().",
                        ex);
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
            final DataInputStream
                    in = new DataInputStream(input.stream());
            try {
                in.readFully(buf);
                assertTrue(Arrays.equals(getData(), buf));
                assertEquals(-1, in.read());
                failure = false;
            } finally {
                if (failure)
                    in.close();
            }
            return in;
        }
    }

    private static void close(final Closeable[] resources) throws IOException {
        IOException ex = null;
        for (final Closeable resource : resources) {
            if (null == resource)
                continue;
            try {
                try {
                    resource.close();
                } finally {
                    resource.close(); // must be idempotent on side effects
                }
            } catch (final IOException ex2) {
                if (null != ex)
                    ex.addSuppressed(ex2);
                else
                    ex = ex2;
            }
        }
        if (null != ex)
            throw ex;
    }

    private <E extends FsArchiveEntry> void check(
            final Container<E> container) {
        final int numEntries = getNumEntries();
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
        final E e = getArchiveDriver().entry(name, Entry.Type.FILE, null);
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
        final FsController<?> pc = null == pm ? null : newParentController(pm);
        return new ParentController(model, pc);
    }

    private FsModel newArchiveModel() {
        final FsModel parent = newNonArchiveModel();
        return newModel(
                FsMountPoint.create(URI.create(
                    "scheme:" + parent.getMountPoint() + entry + "!/")),
                parent);
    }

    private FsModel newNonArchiveModel() {
        return newModel(
                FsMountPoint.create(URI.create("file:/")),
                null);
    }

    protected FsModel newModel( FsMountPoint mountPoint,
                                @CheckForNull FsModel parent) {
        return new FsModel(mountPoint, parent);
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
        final ThrowManager ctl = getThrowControl();
        ctl.check(thiz, IOException.class);
        ctl.check(thiz, RuntimeException.class);
        ctl.check(thiz, Error.class);
    }

    private ThrowManager getThrowControl() {
        return getTestConfig().getThrowControl();
    }

    private int getNumEntries() {
        return getTestConfig().getNumEntries();
    }

    private final class ParentController extends MockController {
        ParentController( FsModel model,
                        @CheckForNull FsController<?> parent) {
            super(model, parent, getTestConfig());
        }

        @Override
        public InputSocket<?> input(
                final FsEntryName name,
                final BitField<FsAccessOption> options) {
            if (null == name)
                throw new NullPointerException();
            if (null == options)
                throw new NullPointerException();

            final class Input extends DecoratingInputSocket<Entry> {
                Input() {
                    super(ParentController.super.input(name, options));
                }

                @Override
                public InputStream stream()
                throws IOException {
                    return new TestInputStream(
                            getBoundSocket().stream());
                }

                @Override
                public SeekableByteChannel channel()
                throws IOException {
                    return new TestSeekableChannel(
                            getBoundSocket().channel());
                }
            } // Input

            return new Input();
        }

        @Override
        public OutputSocket<?> output(
                final FsEntryName name,
                final BitField<FsAccessOption> options,
                final Entry template) {
            if (null == name)
                throw new NullPointerException();
            if (null == options)
                throw new NullPointerException();

            final class Output extends DecoratingOutputSocket<Entry> {
                Output() {
                    super(ParentController.super.output(name, options, template));
                }

                @Override
                public SeekableByteChannel channel()
                throws IOException {
                    return new TestSeekableChannel(
                            getBoundSocket().channel());
                }

                @Override
                public OutputStream stream()
                throws IOException {
                    return new TestOutputStream(
                            getBoundSocket().stream());
                }
            } // Output

            return new Output();
        }
    } // TestController

    @SuppressWarnings("MarkerInterface")
    private interface TestCloseable extends Closeable {
    }

    private final class TestInputStream
    extends DecoratingInputStream
    implements TestCloseable {
        TestInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            checkAllExceptions(this);
            in.close();
        }
    } // TestInputStream

    private final class TestOutputStream
    extends DecoratingOutputStream
    implements TestCloseable {
        TestOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            checkAllExceptions(this);
            out.close();
        }
    } // TestOutputStream

    private final class TestSeekableChannel
    extends DecoratingSeekableChannel
    implements TestCloseable {
        TestSeekableChannel(SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public void close() throws IOException {
            checkAllExceptions(this);
            channel.close();
        }
    } // TestSeekableChannel
}
