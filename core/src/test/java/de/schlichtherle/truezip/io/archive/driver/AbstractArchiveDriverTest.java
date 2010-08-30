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

package de.schlichtherle.truezip.io.archive.driver;

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharConversionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class AbstractArchiveDriverTest extends TestCase {

    private AbstractArchiveDriver driver;

    public AbstractArchiveDriverTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        driver = new DummyArchiveDriver();
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

    public void testCharset() {
        assertSame("US-ASCII", driver.getCharset()); // string literals are interned
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
        assertNotSame(driver.getCharset(), driver2.getCharset());
        assertEquals(driver.getCharset(), driver2.getCharset());
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
        private static final long serialVersionUID = 2382398676900721212L;

        private static final Icon ICON = new ImageIcon(
                DummyArchiveDriver.class.getResource("empty.gif"));

        private DummyArchiveDriver() {
            this("US-ASCII");
        }

        private DummyArchiveDriver(final String encoding) {
            super(encoding, ICON, ICON);
        }

        public InputArchive newInputArchive(Archive archive, ReadOnlyFile rof)
        throws IOException {
            throw new FileNotFoundException(
                    archive.getCanonicalPath() + " (inaccessible archive file)");
        }

        public ArchiveEntry newArchiveEntry(Archive archive, String entryName, ArchiveEntry template)
        throws CharConversionException {
            return new RfsEntry(new File("foo/bar"));
        }

        public OutputArchive newOutputArchive(Archive archive, OutputStream out, InputArchive source)
        throws IOException {
            throw new FileNotFoundException(
                    archive.getCanonicalPath() + " (inaccessible archive file)");
        }
    }
}
