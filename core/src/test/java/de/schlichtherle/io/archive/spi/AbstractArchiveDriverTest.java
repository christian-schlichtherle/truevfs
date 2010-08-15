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

package de.schlichtherle.io.archive.spi;

import de.schlichtherle.io.archive.*;
import de.schlichtherle.io.rof.*;

import java.awt.*;
import java.io.*;

import javax.swing.*;

import junit.framework.*;

/**
 * @author Christian Schlichtherle
 */
public class AbstractArchiveDriverTest extends TestCase {

    private AbstractArchiveDriver driver;

    public AbstractArchiveDriverTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        driver = new DummyArchiveDriver();
    }

    protected void tearDown() throws Exception {
    }

    public void testConstructors() {
        try {
            new DummyArchiveDriver(null);
            fail("Expected NullPointerException!");
        } catch (NullPointerException expected) {
        }
        
        try {
            new DummyArchiveDriver("FooBar");
            fail("Expected RuntimeException!");
        } catch (RuntimeException expected) {
        }

        new DummyArchiveDriver("IBM437"); // may fall back to TrueZIP implementation
    }

    public void testOpenIcon() {
        assertSame(DummyArchiveDriver.ICON, driver.getOpenIcon(null));
    }

    public void testClosedIcon() {
        assertSame(DummyArchiveDriver.ICON, driver.getClosedIcon(null));
    }

    public void testEncoding() {
        assertSame("US-ASCII", driver.getEncoding()); // string literals are interned
    }

    public void testEnsureEncodable() throws CharConversionException {
        driver.ensureEncodable("foo/bar");
        try {
            driver.ensureEncodable("\u1234");
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
        final AbstractArchiveDriver driver2 = (AbstractArchiveDriver) in.readObject();
        in.close();
        
        assertNotSame(driver, driver2);
        assertNotSame(driver.getEncoding(), driver2.getEncoding());
        assertEquals(driver.getEncoding(), driver2.getEncoding());
        assertNotSame(driver.getOpenIcon(null), driver2.getOpenIcon(null));
        //assertEquals(driver.getOpenIcon(null), driver2.getOpenIcon(null));
        assertNotSame(driver.getClosedIcon(null), driver2.getClosedIcon(null));
        //assertEquals(driver.getClosedIcon(null), driver2.getClosedIcon(null));
        driver2.ensureEncodable("foo/bar");
    }

    public void testMultithreading() throws Throwable {
        final Object ready = new Object();
        final Object go = new Object();

        class TestThread extends Thread {
            Throwable throwable; // = null;

            public void run() {
                try {
                    synchronized (go) {
                        synchronized (ready) {
                            ready.notify(); // there can be only one waiting thread!
                        }
                        go.wait(2000);
                    }
                    for (int i = 0; i < 100000; i++)
                        driver.ensureEncodable("foo/bar");
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

    private static class DummyArchiveDriver extends AbstractArchiveDriver {
        private static final Icon ICON = new ImageIcon(
                DummyArchiveDriver.class.getResource("empty.gif"));

        private DummyArchiveDriver() {
            this("US-ASCII");
        }

        private DummyArchiveDriver(final String encoding) {
            super(encoding, ICON, ICON);
        }

        public InputArchive createInputArchive(Archive archive, ReadOnlyFile rof)
        throws IOException {
            throw new FileNotFoundException(
                    archive.getPath() + " (inaccessible archive file)");
        }

        public ArchiveEntry createArchiveEntry(Archive archive, String entryName, ArchiveEntry template)
        throws CharConversionException {
            return new RfsEntry(new File("foo/bar"));
        }

        public OutputArchive createOutputArchive(Archive archive, OutputStream out, InputArchive source)
        throws IOException {
            throw new FileNotFoundException(
                    archive.getPath() + " (inaccessible archive file)");
        }
    }
}
