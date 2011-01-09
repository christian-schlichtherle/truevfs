/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs.archive.driver;

import de.schlichtherle.truezip.io.fs.concurrent.FSConcurrentModel;
import de.schlichtherle.truezip.io.fs.archive.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class CharsetArchiveDriverTest extends TestCase {

    private CharsetArchiveDriver<ArchiveEntry> driver;

    public CharsetArchiveDriverTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        driver = new DummyArchiveDriver(Charset.forName("US-ASCII"));
    }

    @Override
    protected void tearDown() throws Exception {
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructors() {
        try {
            new DummyArchiveDriver(null);
            fail("Expected NullPointerException!");
        } catch (NullPointerException expected) {
        }

        new DummyArchiveDriver(Charset.forName("IBM437")); // may fall back to TrueZIP implementation
    }

    public void testOpenIcon() {
        assertSame(DummyArchiveDriver.ICON, driver.getOpenIcon(null));
    }

    public void testClosedIcon() {
        assertSame(DummyArchiveDriver.ICON, driver.getClosedIcon(null));
    }

    public void testCharset() {
        assertEquals(Charset.forName("US-ASCII"), driver.getCharset());
    }

    public void testAssertEncodable() throws CharConversionException {
        driver.assertEncodable("foo/bar");
        try {
            driver.assertEncodable("\u1234");
            fail("Unencodable string!");
        } catch (CharConversionException expected) {
        }
    }

    public void testSerialization() throws IOException, ClassNotFoundException {
        // Serialize.
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(driver);
        out.close();

        // Deserialize.
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream in = new ObjectInputStream(bis);
        @SuppressWarnings("unchecked")
        final CharsetArchiveDriver<ArchiveEntry> driver2
                = (CharsetArchiveDriver<ArchiveEntry>) in.readObject();
        in.close();
        
        assertNotSame(driver, driver2);
        assertSame(driver.getCharset(), driver2.getCharset());
        assertSame(driver.getOpenIcon(null), driver2.getOpenIcon(null)); // static property!
        assertSame(driver.getClosedIcon(null), driver2.getClosedIcon(null)); // static property!
        driver2.assertEncodable("foo/bar");
    }

    public void testMultithreading() throws Throwable {
        final Object ready = new Object();
        final Object go = new Object();

        class TestThread extends Thread {
            Throwable throwable; // = null;

            TestThread() {
                setDaemon(true);
            }

            @Override
            public void run() {
                try {
                    synchronized (go) {
                        synchronized (ready) {
                            ready.notify(); // there can be only one waiting thread!
                        }
                        go.wait(2000);
                    }
                    for (int i = 0; i < 100000; i++)
                        driver.assertEncodable("foo/bar");
                } catch (Throwable t) {
                    throwable = t;
                }
            }
        }

        final TestThread[] threads = new TestThread[20];
        synchronized (ready) {
            for (int i = 0; i < threads.length; i++) {
                final TestThread thread = new TestThread();
                thread.start();
                threads[i] = thread;
                ready.wait(100);
            }
        }

        synchronized (go) {
            go.notifyAll(); // Peng!
        }

        for (int i = 0; i < threads.length; i++) {
            final TestThread thread = threads[i];
            thread.join();
            final Throwable throwable = thread.throwable;
            if (throwable != null)
                throw throwable;
        }
    }

    private static class DummyArchiveDriver extends CharsetArchiveDriver<ArchiveEntry> {
        static final long serialVersionUID = 2382398676900721212L;

        static final Icon ICON = new ImageIcon(
                DummyArchiveDriver.class.getResource("empty.gif"));

        DummyArchiveDriver(final Charset charset) {
            super(charset);
        }

        @Override
        public InputShop<ArchiveEntry> newInputShop(FSConcurrentModel archive, InputSocket<?> input)
        throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ArchiveEntry newEntry(String name, Type type, Entry template)
        throws CharConversionException {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputShop<ArchiveEntry> newOutputShop(FSConcurrentModel archive, OutputSocket<?> output, InputShop<ArchiveEntry> source)
        throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Icon getOpenIcon(FSConcurrentModel archive) {
            return ICON;
        }

        @Override
        public Icon getClosedIcon(FSConcurrentModel archive) {
            return ICON;
        }
    }
}
