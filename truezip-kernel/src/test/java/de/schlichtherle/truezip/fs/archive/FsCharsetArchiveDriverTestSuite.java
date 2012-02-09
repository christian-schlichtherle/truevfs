/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import static de.schlichtherle.truezip.util.ConcurrencyUtils.NUM_IO_THREADS;
import de.schlichtherle.truezip.util.ConcurrencyUtils.TaskFactory;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.runConcurrent;
import java.io.CharConversionException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import javax.annotation.CheckForNull;
import org.junit.Test;

/**
 * @param   <D> The type of the charset archive driver.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsCharsetArchiveDriverTestSuite<
        D extends FsCharsetArchiveDriver<?>>
extends FsArchiveDriverTestSuite<D> {

    private static final String ENCODABLE_NAME;
    static {
        // US-ASCII must always be encodable.
        final StringBuilder builder = new StringBuilder(128);
        for (char c = 0; c <= 127; c++)
            builder.append(c);
        ENCODABLE_NAME = builder.toString();
    }

    @Test
    public void testCharset() {
        assert null != getArchiveDriver().getCharset();
    }

    @Test(expected = CharConversionException.class)
    public void testUnencodableName() throws CharConversionException {
        final String name = getUnencodableName();
        if (null == name)
            throw new CharConversionException("Ignore me!");
        getArchiveDriver().assertEncodable(name);
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
    public void testEncodableName() throws CharConversionException {
        getArchiveDriver().assertEncodable(ENCODABLE_NAME);
    }

    @Test
    public void testMultithreadedEncodableName() throws Throwable {
        final CountDownLatch start = new CountDownLatch(NUM_IO_THREADS);

        class TestTask implements Callable<Void> {
            @Override
            public Void call()
            throws CharConversionException, InterruptedException {
                start.countDown();
                start.await();
                for (int i = 0; i < 100000; i++)
                    getArchiveDriver().assertEncodable(ENCODABLE_NAME);
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
