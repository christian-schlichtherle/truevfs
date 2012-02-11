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
import java.io.*;
import java.net.URI;
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
                output(os, i);
            check(os);
        } finally {
            os.close();
        }

        final InputShop<E> is = getArchiveDriver()
                .newInputShop(model, getArchiveInputSocket());
        try {
            check(is);
            for (int i = 0; i < MAX_ENTRIES; i++)
                input(is, i);
        } finally {
            is.close();
        }
    }

    private InputSocket<?> getArchiveInputSocket() {
        return getArchiveDriver().getInputSocket(parent, name,
                FsInputOptions.NO_INPUT_OPTIONS);
    }

    private OutputSocket<?> getArchiveOutputSocket() {
        return getArchiveDriver().getOutputSocket(parent, name,
                FsOutputOptions.NO_OUTPUT_OPTIONS, null);
    }

    private <E extends FsArchiveEntry> void check(final EntryContainer<E> c) {
        assertEquals(MAX_ENTRIES, c.getSize());
        final Iterator<E> it = c.iterator();
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
            assertSame(e, c.getEntry(e.getName()));
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
        assertEquals(MAX_ENTRIES, c.getSize());
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

    private void output(final OutputShop<E> shop, final int i)
    throws IOException {
        final String name = name(i);
        final E entry = newEntry(name);
        final OutputSocket<? extends E> output = shop.getOutputSocket(entry);

        assertNull(shop.getEntry(name));
        assertEquals(i, shop.getSize());
        final OutputStream out = output.newOutputStream();
        try {
            assertSame(entry, shop.getEntry(name));
            assertEquals(i + 1, shop.getSize());
            out.write(getData());
        } finally {
            out.close();
        }
    }

    private void input(final InputShop<E> shop, final int i)
    throws IOException {
        final InputSocket<? extends E> input = shop.getInputSocket(name(i));

        {
            final byte[] buf = new byte[getDataLength()];
            final InputStream in = input.newInputStream();
            try {
                readFully(in, buf);
                assertEquals(-1, in.read());
            } finally {
                in.close();
            }
            assertTrue(Arrays.equals(getData(), buf));
        }

        try {
            final byte[] buf = new byte[getDataLength()];
            final ReadOnlyFile rof = input.newReadOnlyFile();
            try {
                rof.readFully(buf);
                assertEquals(-1, rof.read());
            } finally {
                rof.close();
            }
            assertTrue(Arrays.equals(getData(), buf));
        } catch (UnsupportedOperationException ignore) {
        }
    }

    private static void readFully(InputStream in, byte[] b)
    throws IOException {
        readFully(in, b, 0, b.length);
    }

    private static void readFully(  final InputStream in,
                                    final byte[] buf,
                                    final int off,
                                    final int len)
    throws IOException {
        int total = 0, read;
        do {
            read = in.read(buf, off + total, len - total);
            if (read < 0)
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
