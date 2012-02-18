/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Size.STORAGE;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import de.schlichtherle.truezip.entry.EntryContainer;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.fs.mock.MockController;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.test.TestConfig;
import de.schlichtherle.truezip.test.ThrowControl;
import static de.schlichtherle.truezip.test.ThrowControl.contains;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @param   <E> The type of the archive entries.
 * @param   <D> The type of the archive driver.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsArchiveDriverTestSuite<
        E extends FsArchiveEntry,
        D extends FsArchiveDriver<E>>
extends FsArchiveDriverTestBase<D> {

    private static final Logger
            logger = Logger.getLogger(FsArchiveDriverTestSuite.class.getName());

    private static final FsEntryName
            name = FsEntryName.create(URI.create("archive"));
    private static final FsModel model = newArchiveModel();

    private MockController<FsModel> parent;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        // Order is important here!
        final TestConfig config = getTestConfig();
        config.setDataSize(getMaxArchiveLength());
        config.setIOPoolProvider(null); // reset
        parent = newController(model.getParent());
    }

    @Test
    public void testArchiveDriverMustBeFederated() {
        assertTrue(getArchiveDriver().isFederated());
    }

    @Test
    public void testIOPoolMustNotBeNull() {
        assertNotNull(getArchiveDriver().getPool());
    }

    @Test
    public void testIOPoolShouldBeConstant() {
        final IOPool<?> p1 = getArchiveDriver().getPool();
        final IOPool<?> p2 = getArchiveDriver().getPool();
        if (p1 != p2)
            logger.log(Level.WARNING, "{0} returns different I/O buffer pools upon multiple invocations of getPool()!", getArchiveDriver().getClass());
    }

    @Test(expected = NullPointerException.class)
    public void testNewControllerMustNotTolerateNullModel() {
        getArchiveDriver().newController(null, parent);
    }

    @Test(expected = NullPointerException.class)
    public void testNewControllerMustNotTolerateNullParent() {
        getArchiveDriver().newController(model, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewControllerMustCheckParentMemberMatch1() {
        getArchiveDriver().newController(model.getParent(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewControllerMustCheckParentMemberMatch2() {
        getArchiveDriver().newController(model.getParent(), parent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewControllerMustCheckParentMemberMatch3() {
        getArchiveDriver().newController(model, newController(model));
    }

    @Test
    public void testNewControllerMustNotReturnNull() {
        assertNotNull(getArchiveDriver().newController(model, parent));
    }

    @Test
    public void testNewControllerMustMeetPostConditions() {
        final FsController<?> c = getArchiveDriver()
                .newController(model, parent);
        assertNotNull(c);
        assertEquals(model.getMountPoint(), c.getModel().getMountPoint());
        assertSame(parent, c.getParent());
    }

    @Test
    public void testGetInputSocketMustForwardTheCallToTheGivenController() {
        final Throwable expected = new RuntimeException();
        trigger(MockController.class, expected);
        try {
            getArchiveInputSocket();
            fail();
        } catch (final RuntimeException got) {
            if (!contains(got, expected))
                throw got;
        }
    }

    @Test
    public void testGetOutputSocketMustForwardTheCallToTheGivenController() {
        final Throwable expected = new RuntimeException();
        trigger(MockController.class, expected);
        try {
            getArchiveOutputSocket();
            fail();
        } catch (final RuntimeException got) {
            if (!contains(got, expected))
                throw got;
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputShopMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().newInputShop(null, getArchiveInputSocket());
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputShopMustNotTolerateNullInputSocket() throws IOException {
        getArchiveDriver().newInputShop(model, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputShopMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().newOutputShop(null, getArchiveOutputSocket(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputShopMustNotTolerateNullInputSocket() throws IOException {
        getArchiveDriver().newOutputShop(model, null, null);
    }

    @Test
    public void testRoundTripPersistence() throws IOException {
        final OutputShop<E> os = getArchiveDriver()
                .newOutputShop(model, getArchiveOutputSocket(), null);
        try {
            final Closeable[] streams = new Closeable[getNumEntries()];
            try {
                for (int i = 0; i < streams.length; i++)
                    streams[i] = output(os, i);
            } finally {
                close(streams);
            }
            check(os);
        } finally {
            os.close();
        }
        os.close();

        final InputShop<E> is = getArchiveDriver()
                .newInputShop(model, getArchiveInputSocket());
        try {
            check(is);
            final Closeable[] streams = new Closeable[getNumEntries()];
            try {
                for (int i = 0; i < streams.length; i++) {
                    input(is, i).close(); // first attempt
                    streams[i] = input(is, i); // second attempt
                }
            } finally {
                close(streams);
            }
        } finally {
            is.close();
        }
        is.close();
    }

    private static void close(final Closeable[] resources) throws IOException {
        final IOException expected = new IOException();
        IOException failure = null;
        for (final Closeable resource : resources) {
            if (null == resource)
                break;
            /*final Class<?> from;
            // Mind that resource may be decorating the respective Mock* class!
            if (resource instanceof InputStream)
                trigger(from = InputStream.class, expected);
            else if (resource instanceof OutputStream)
                trigger(from = OutputStream.class, expected);
            else if (resource instanceof ReadOnlyFile)
                trigger(from = ReadOnlyFile.class, expected);
            else if (resource instanceof SeekableByteChannel)
                trigger(from = SeekableByteChannel.class, expected);
            else
                from = null;*/
            try {
                resource.close();
            } catch (final IOException got) {
                if (!contains(got, expected))
                    failure = got;
            } finally {
                /*if (null != from)
                    clear(from);*/
            }
            try {
                resource.close();
            } catch (final IOException ex) {
                failure = ex;
            }
        }
        if (null != failure)
            throw failure;
    }

    private InputSocket<?> getArchiveInputSocket() {
        return getArchiveDriver().getInputSocket(parent, name,
                FsInputOptions.NO_INPUT_OPTIONS);
    }

    private OutputSocket<?> getArchiveOutputSocket() {
        return getArchiveDriver().getOutputSocket(parent, name,
                FsOutputOptions.NO_OUTPUT_OPTIONS, null);
    }

    private <E extends FsArchiveEntry> void check(
            final EntryContainer<E> container) {
        final int numEntries = getNumEntries();
        assertEquals(numEntries, container.getSize());
        final Iterator<E> it = container.iterator();
        for (int i = 0; i < numEntries; i++) {
            final E e = it.next();
            assertNotNull(e);
            assertEquals(name(i), e.getName());
            assertSame(FILE, e.getType());
            assertEquals(getDataLength(), e.getSize(DATA));
            assertTrue(getDataLength() <= e.getSize(STORAGE)); // random data is not compressible!
            assertTrue(UNKNOWN != e.getTime(WRITE));
            try {
                it.remove();
                fail();
            } catch (UnsupportedOperationException expected) {
            }
            assertSame(e, container.getEntry(e.getName()));
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
        assertEquals(numEntries, container.getSize());
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

    @CreatesObligation
    private OutputStream output(final OutputShop<E> shop, final int i)
    throws IOException {
        final String name = name(i);
        final E entry = newEntry(name);
        final OutputSocket<? extends E> output = shop.getOutputSocket(entry);
        assertSame(entry, output.getLocalTarget());

        assertNull(shop.getEntry(name));
        assertEquals(i, shop.getSize());

        boolean failure = true;
        final OutputStream out = output.newOutputStream();
        try {
            assertSame(entry, shop.getEntry(name));
            assertEquals(i + 1, shop.getSize());
            out.write(getData());
            failure = false;
        } finally {
            if (failure)
                out.close();
        }
        return out;
    }

    private InputStream input(final InputShop<E> shop, final int i)
    throws IOException {
        final InputSocket<? extends E> input = shop.getInputSocket(name(i));

        {
            final byte[] buf = new byte[getDataLength()];
            ReadOnlyFile rof;
            try {
                rof = input.newReadOnlyFile();
            } catch (UnsupportedOperationException ex) {
                rof = null;
                logger.log(Level.FINE,
                        input.getClass()
                            + " does not support newReadOnlyFile().",
                        ex);
            }
            if (null != rof) {
                try {
                    rof.readFully(buf);
                    assertEquals(-1, rof.read());
                } finally {
                    rof.close();
                }
                rof.close(); // expect no issues
                assertTrue(Arrays.equals(getData(), buf));
            }
        }

        {
            final byte[] buf = new byte[getDataLength()];
            SeekableByteChannel sbc;
            try {
                sbc = input.newSeekableByteChannel();
            } catch (UnsupportedOperationException ex) {
                sbc = null;
                logger.log(Level.FINE,
                        input.getClass()
                            + " does not support newSeekableByteChannel().",
                        ex);
            }
            if (null != sbc) {
                try {
                    readFully(sbc, ByteBuffer.wrap(buf));
                    assertEquals(-1, sbc.read(ByteBuffer.wrap(buf)));
                } finally {
                    sbc.close();
                }
                sbc.close(); // expect no issues
                assertTrue(Arrays.equals(getData(), buf));
            }
        }

        {
            final byte[] buf = new byte[getDataLength()];
            boolean failure = true;
            final InputStream in = input.newInputStream();
            try {
                readFully(in, buf);
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

    private static void readFully(InputStream in, byte[] b)
    throws IOException {
        new DataInputStream(in).readFully(b);
    }

    private static void readFully(  final ReadableByteChannel rbc,
                                    final ByteBuffer buf)
    throws IOException {
        final int len = buf.remaining();
        int total = 0;
        do {
            final int read = rbc.read(buf);
            if (0 > read)
                throw new EOFException();
            total += read;
        } while (total < len);
    }

    private <M extends FsModel> MockController<M>
    newController(final M model) {
        final FsModel pm = model.getParent();
        final FsController<FsModel> pc = null == pm ? null : newController(pm);
        return new MockController<M>(model, pc, getTestConfig());
    }

    private static FsModel newArchiveModel() {
        final FsModel parent = newNonArchiveModel();
        return new FsDefaultModel(
                FsMountPoint.create(URI.create(
                    "scheme:" + parent.getMountPoint() + name + "!/")),
                parent);
    }

    private static FsModel newNonArchiveModel() {
        return new FsDefaultModel(
                FsMountPoint.create(URI.create("file:/")),
                null);
    }

    private int getMaxArchiveLength() {
        return getNumEntries() * getDataLength() * 4 / 3; // account for archive type specific overhead
    }

    private Throwable trigger(Class<?> from, Throwable toThrow) {
        return getControl().trigger(from, toThrow);
    }

    private ThrowControl getControl() {
        return getTestConfig().getThrowControl();
    }

    private int getNumEntries() {
        return getTestConfig().getNumEntries();
    }
}
