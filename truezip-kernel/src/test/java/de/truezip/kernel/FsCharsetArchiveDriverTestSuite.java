/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.FsArchiveEntry;
import de.truezip.kernel.FsCharsetArchiveDriver;
import static de.truezip.kernel.util.ConcurrencyUtils.NUM_IO_THREADS;
import de.truezip.kernel.util.ConcurrencyUtils.TaskFactory;
import static de.truezip.kernel.util.ConcurrencyUtils.runConcurrent;
import java.io.CharConversionException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import javax.annotation.CheckForNull;
import org.junit.Test;

/**
 * @param  <E> The type of the archive entries.
 * @param  <D> The type of the charset archive driver.
 * @author Christian Schlichtherle
 */
public abstract class FsCharsetArchiveDriverTestSuite<
        E extends FsArchiveEntry,
        D extends FsCharsetArchiveDriver<E>>
extends FsArchiveDriverTestSuite<E, D> {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String US_ASCII_CHARACTERS;
    static {
        final StringBuilder builder = new StringBuilder(128);
        for (char c = 0; c <= 127; c++)
            builder.append(c);
        US_ASCII_CHARACTERS = builder.toString();
    }

    @Override
    public void setUp() throws IOException {
        super.setUp();
        assert !UTF8.equals(getArchiveDriver().getCharset())
                || null == getUnencodableName() : "Bad test setup!";
    }

    @Test
    public void testCharsetMustNotBeNull() {
        assert null != getArchiveDriver().getCharset();
    }

    @Test(expected = CharConversionException.class)
    public void testUnencodableNameMustThrowCharConversionException()
    throws CharConversionException {
        final String name = getUnencodableName();
        if (null == name)
            throw new CharConversionException("Ignore me!");
        getArchiveDriver().checkEncodable(name);
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
    public void testAllUsAsciiCharactersMustBeEncodable()
    throws CharConversionException {
        getArchiveDriver().checkEncodable(US_ASCII_CHARACTERS);
    }

    @Test
    public void testAllUsAsciiCharactersMustBeEncodableWhenRunningMultithreaded()
    throws Throwable {
        final CountDownLatch start = new CountDownLatch(NUM_IO_THREADS);

        final class CheckFactory implements TaskFactory {
            @Override
            public Callable<?> newTask(int threadNum) {
                return new Check();
            }

            final class Check implements Callable<Void> {
                @Override
                public Void call()
                throws CharConversionException, InterruptedException {
                    start.countDown();
                    start.await();
                    for (int i = 0; i < 100000; i++)
                        getArchiveDriver().checkEncodable(US_ASCII_CHARACTERS);
                    return null;
                }
            } // Check
        } // CheckFactory

        runConcurrent(NUM_IO_THREADS, new CheckFactory()).join();
    }
}
