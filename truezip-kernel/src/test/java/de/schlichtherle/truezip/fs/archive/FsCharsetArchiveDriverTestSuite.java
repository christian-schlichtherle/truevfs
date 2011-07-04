/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.socket.ByteArrayIOPool;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import java.io.CharConversionException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsCharsetArchiveDriverTestSuite {

    private static class ByteArrayIOPoolProvider implements IOPoolProvider {
        @Override
        public IOPool<?> get() {
            return new ByteArrayIOPool(2048);
        }
    }
    private static final IOPoolProvider
            POOL_PROVIDER = new ByteArrayIOPoolProvider();

    private static final String TEXT = "fubar";

    private FsCharsetArchiveDriver<?> driver;

    @Before
    public void setUp() {
        driver = newArchiveDriver(POOL_PROVIDER);
    }

    protected abstract FsCharsetArchiveDriver<?> newArchiveDriver(IOPoolProvider provider);

    @Test
    public final void testAssertEncodable() throws CharConversionException {
        driver.assertEncodable(TEXT);
    }

    @Test
    public final void testMultithreading() throws Throwable {
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
                        driver.assertEncodable(TEXT);
                } catch (Throwable t) {
                    throwable = t;
                }
            }
        } // class TestThread

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
}
