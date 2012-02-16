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
import de.schlichtherle.truezip.util.BitField;
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
import javax.annotation.Nullable;
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

    private static final int MAX_ENTRIES = 10;

    private static final FsEntryName
            name = FsEntryName.create(URI.create("archive"));
    private static final FsModel model = newArchiveModel();

    private @Nullable FsController<?> parent;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        parent = newController(model.getParent());
    }

    @Test
    public void archiveDriverMustBeFederated() {
        assertTrue(getArchiveDriver().isFederated());
    }

    @Test
    public void ioPoolMustNotBeNull() {
        assertNotNull(getArchiveDriver().getPool());
    }

    @Test
    public void ioPoolShouldBeConstant() {
        final IOPool<?> p1 = getArchiveDriver().getPool();
        final IOPool<?> p2 = getArchiveDriver().getPool();
        if (p1 != p2)
            logger.log(Level.WARNING, "{0} returns different I/O buffer pools upon multiple invocations of getPool()!", getArchiveDriver().getClass());
    }

    @Test(expected = NullPointerException.class)
    public void newControllerMustNotTolerateNullModel() {
        getArchiveDriver().newController(null, parent);
    }

    @Test(expected = NullPointerException.class)
    public void newControllerMustNotTolerateNullParent() {
        getArchiveDriver().newController(model, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newControllerMustCheckParentMemberMatch1() {
        getArchiveDriver().newController(model.getParent(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newControllerMustCheckParentMemberMatch2() {
        getArchiveDriver().newController(model.getParent(), parent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newControllerMustCheckParentMemberMatch3() {
        getArchiveDriver().newController(model, newController(model));
    }

    @Test
    public void newControllerMustNotReturnNull() {
        assertNotNull(getArchiveDriver().newController(model, parent));
    }

    @Test
    public void newControllerMustMeetPostConditions() {
        final FsController<?> c = getArchiveDriver()
                .newController(model, parent);
        assertNotNull(c);
        assertEquals(model.getMountPoint(), c.getModel().getMountPoint());
        assertSame(parent, c.getParent());
    }

    @Test
    public void getInputSocketMustForwardTheCallToTheGivenController() {
        final FsController<FsModel> controller = new MockController<FsModel>(
                model.getParent(), null, getMaxArchiveLength()) {
            @Override
            public InputSocket<?> getInputSocket(
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                assertEquals(FsEntryName.ROOT, name);
                assertNotNull(options);
                throw new UnsupportedOperationException();
            }
        };
        try {
            getArchiveDriver().getInputSocket(
                    controller,
                    FsEntryName.ROOT,
                    FsInputOptions.NO_INPUT_OPTIONS);
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void getOutputSocketMustForwardTheCallToTheGivenController() {
        final FsController<FsModel> controller = new MockController<FsModel>(
                model.getParent(), null, getMaxArchiveLength()) {
            @Override
            public OutputSocket<?> getOutputSocket(
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    Entry template) {
                assertEquals(FsEntryName.ROOT, name);
                assertNotNull(options);
                throw new UnsupportedOperationException();
            }
        };
        try {
            getArchiveDriver().getOutputSocket(
                    controller,
                    FsEntryName.ROOT,
                    FsOutputOptions.NO_OUTPUT_OPTIONS,
                    null);
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test(expected = NullPointerException.class)
    public void newInputShopMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().newInputShop(null, getArchiveInputSocket());
    }

    @Test(expected = NullPointerException.class)
    public void newInputShopMustNotTolerateNullInputSocket() throws IOException {
        getArchiveDriver().newInputShop(model, null);
    }

    @Test(expected = NullPointerException.class)
    public void newOutputShopMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().newOutputShop(null, getArchiveOutputSocket(), null);
    }

    @Test(expected = NullPointerException.class)
    public void newOutputShopMustNotTolerateNullInputSocket() throws IOException {
        getArchiveDriver().newOutputShop(model, null, null);
    }

    @Test
    public void persistenceRoundTrip() throws IOException {
        final OutputShop<E> os = getArchiveDriver()
                .newOutputShop(model, getArchiveOutputSocket(), null);
        try {
            for (int i = 0; i < MAX_ENTRIES; i++)
                output(os, i).close();
            check(os);
        } finally {
            os.close();
        }
        os.close();

        final InputShop<E> is = getArchiveDriver()
                .newInputShop(model, getArchiveInputSocket());
        try {
            check(is);
            for (int i = 0; i < MAX_ENTRIES; i++)
                input(is, i).close();
        } finally {
            is.close();
        }
        is.close();
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
        assertEquals(MAX_ENTRIES, container.getSize());
        final Iterator<E> it = container.iterator();
        for (int i = 0; i < MAX_ENTRIES; i++) {
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
        assertEquals(MAX_ENTRIES, container.getSize());
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
        final OutputStream out = output.newOutputStream();
        try {
            assertSame(entry, shop.getEntry(name));
            assertEquals(i + 1, shop.getSize());
            out.write(getData());
        } catch (IOException ex) {
            out.close();
            throw ex;
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
            final InputStream in = input.newInputStream();
            try {
                readFully(in, buf);
                assertEquals(-1, in.read());
            } catch (IOException ex) {
                in.close();
                throw ex;
            }
            if (!Arrays.equals(getData(), buf)) {
                in.close();
                fail();
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
        final FsController<FsModel> pc = null == pm
                ? null
                : newController(pm);
        return new MockController<M>(model, pc, getMaxArchiveLength());
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
        return MAX_ENTRIES * getDataLength() * 4 / 3; // account for archive type specific overhead
    }
}
