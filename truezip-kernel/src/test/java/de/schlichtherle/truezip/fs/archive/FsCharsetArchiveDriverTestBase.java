/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.spi.ByteArrayIOPoolService;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.NUM_IO_THREADS;
import de.schlichtherle.truezip.util.ConcurrencyUtils.TaskFactory;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.runConcurrent;
import java.io.CharConversionException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsCharsetArchiveDriverTestBase {

    private static final IOPoolProvider
            IO_POOL_PROVIDER = new ByteArrayIOPoolService(2048);

    private static final String ENCODABLE_TEXT = "fubar";

    private FsCharsetArchiveDriver<?> driver;

    @Before
    public void setUp() {
        driver = newArchiveDriver(IO_POOL_PROVIDER);
    }

    protected abstract FsCharsetArchiveDriver<?> newArchiveDriver(IOPoolProvider provider);

    @Test
    public final void testAssertEncodable() throws CharConversionException {
        driver.assertEncodable(ENCODABLE_TEXT);
    }

    @Test
    public final void testMultithreadedAssertEncodable() throws Throwable {
        final CountDownLatch start = new CountDownLatch(NUM_IO_THREADS);

        class TestTask implements Callable<Void> {
            @Override
            public Void call()
            throws CharConversionException, InterruptedException {
                start.countDown();
                start.await();
                for (int i = 0; i < 100000; i++)
                    driver.assertEncodable(ENCODABLE_TEXT);
                return null;
            }
        } // TestTask

        class TestTaskFactory implements TaskFactory {
            @Override
            public Callable<Void> newTask(int threadNum) {
                return new TestTask();
            }
        } // TestTaskFactory

        runConcurrent(NUM_IO_THREADS, new TestTaskFactory()).join();
    }
}
