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
package de.schlichtherle.truezip.samples;

import de.schlichtherle.truezip.key.passwd.swing.HurlingWindowFeedback;
import de.schlichtherle.truezip.file.File;
import de.schlichtherle.truezip.io.fs.FsManagers;
import de.schlichtherle.truezip.io.fs.FsStatistics;
import de.schlichtherle.truezip.io.fs.FsStatisticsManager;
import de.schlichtherle.truezip.key.passwd.swing.InvalidKeyFeedback;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;

/**
 * Abstract base class for command line utilities.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class CommandLineUtility {

    /** The print stream for standard output. */
    protected final PrintStream out;

    /** The print stream for error output. */
    protected final PrintStream err;

    /** The command line progress monitor. */
    protected final ProgressMonitor monitor;

    /**
     * Equivalent to
     * {@link #CommandLineUtility(OutputStream, OutputStream, boolean)
     * CommandLineUtility(System.out, System.err, true)}.
     */
    protected CommandLineUtility() {
        this(System.out, System.err, true);
    }

    /**
     * Constructs a new command line utility instance.
     * <p>
     * <b>Warning</b>: This constructor has side effects:
     * If Swing based prompting is used, the Hurling Window Feedback is set for
     * feedback on wrong key entry unless the respective system properties
     * have been explicitly set.
     *
     * @param out The standard output stream.
     * @param err The error output stream.
     * @param autoFlush If the output streams are not {@link PrintStream}s,
     *        then they are wrapped in a new {@code PrintStream} with
     *        this as the additional constructor parameter.
     * @see de.schlichtherle.truezip.key.passwd.swing.PromptingKeyManager
     */
    protected CommandLineUtility(
            final OutputStream out,
            final OutputStream err,
            final boolean autoFlush) {
        if (out == null || err == null)
            throw new NullPointerException();
        this.out = out instanceof PrintStream
                ? (PrintStream) out
                : new PrintStream(out, autoFlush);
        this.err = err instanceof PrintStream
                ? (PrintStream) err
                : new PrintStream(err, autoFlush);
        this.monitor = new ProgressMonitor(this.err);
        configureFeedback();
    }

    /**
     * Configure the type of the feedback when prompting the user for keys for
     * RAES encrypted ZIP files using the Swing based prompting key manager.
     * If this JVM is running in headless mode, then the configuration is
     * ignored.
     */
    private static void configureFeedback() {
        String spec = InvalidKeyFeedback.class.getName();
        String impl = HurlingWindowFeedback.class.getName();
        System.setProperty(spec, System.getProperty(spec, impl));
    }

    /**
     * Runs this command line utility.
     * Prints a user readable error message to the error output stream
     * which was provided to the constructor if an error occurs.
     *
     * @param args A non-empty array of Unix-like commands and optional
     *        parameters.
     * @return {@code 1} iff the command fails,
     *         {@code 0} otherwise.
     */
    public final int run(final String[] args) {
        try {
            try {
                return runChecked(args);
            } finally {
                try {
                    File.umount();
                } finally {
                    monitor.shutdown();
                }
            }
        } catch (IllegalUsageException ex) {
            err.println(ex.getLocalizedMessage());
            return 1;
        } catch (IOException ex) {
            err.println(ex.getLocalizedMessage());
            return 1;
        }
    }

    /**
     * Runs this command line utility.
     * Throws an exception if an error occurs.
     *
     * @param  args a non-{@code null} array of command line parameters.
     * @return the return code for {@link System#exit}.
     * @throws IllegalUsageException If {@code args} does not contain
     *         correct commands or parameters.
     * @throws IOException On any I/O related exception.
     */
    public abstract int runChecked(String[] args)
    throws IllegalUsageException, IOException;

    protected static class IllegalUsageException extends Exception {
        private static final long serialVersionUID = 1985623981423542464L;

        public IllegalUsageException(String msg) {
            super(msg);
        }
    } // class IllegalUsageException

    protected static final class ProgressMonitor extends Thread {
        private final PrintStream err;
        private final Long[] args = new Long[2];
        private final FsStatistics stats;

        ProgressMonitor(final PrintStream err) {
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY);
            this.err = err;
            final FsStatisticsManager manager
                    = new FsStatisticsManager(
                        FsManagers.getInstance());
            this.stats = manager.getStatistics();
            FsManagers.setInstance(manager);
        }

        @Override
        public void start() {
            if (err == System.err || err == System.out)
                super.start();
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            boolean run = false;
            for (long sleep = 2000; ; sleep = 200, run = true) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException shutdown) {
                    break;
                }
                showProgress();
            }
            if (run) {
                showProgress();
                err.println();
            }
        }

        /**
         * Prints statistics about the amount of data read and written by
         * {@link File#update()} or {@link File#umount()} on standard output.
         */
        private void showProgress() {
            // Round up to kilobytes.
            args[0] = (stats.getTopLevelRead() + 1023) / 1024;
            args[1] = (stats.getTopLevelWritten() + 1023) / 1024;
            err.print(MessageFormat.format(
                    "Top level archive I/O: {0} / {1} KB        \r", (Object[]) args));
            err.flush();
        }

        @SuppressWarnings("CallToThreadDumpStack")
        private void shutdown() {
            interrupt();
            try {
                join();
            } catch (InterruptedException interrupted) {
                interrupted.printStackTrace();
            }
        }
    } // class ProgressMonitor
}
