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
package de.schlichtherle.truezip.sample.file.app;

import de.schlichtherle.truezip.crypto.raes.param.swing.HurlingWindowFeedback;
import de.schlichtherle.truezip.crypto.raes.param.swing.InvalidKeyFeedback;
import de.schlichtherle.truezip.file.TApplication;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Abstract base class for command line utilities.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
abstract class Application extends TApplication<RuntimeException> {

    /** The print stream for standard output. */
    protected final PrintStream out;

    /** The print stream for error output. */
    protected final PrintStream err;

    /**
     * Equivalent to
     * {@link #Application(OutputStream, OutputStream, boolean)
     * Application(System.out, System.err, true)}.
     */
    protected Application() {
        this(System.out, System.err, true);
    }

    /**
     * Constructs a new command line utility instance.
     * <p>
     * Note that this constructor has side effects:
     * <ul>
     * <li>If Swing based prompting is used, the Hurling Window Feedback is
     *     set for feedback on wrong key entry unless the respective system
     *     properties have been explicitly set.
     * <li>Similarly, the {@link SampleManagerService} class is set for
     *     obtaining statistics when synchronizing any uncommitted changes to
     *     the contents of archive files.
     * </ul>
     *
     * @param out the standard output stream.
     * @param err the error output stream.
     * @param autoFlush if the output streams are not {@link PrintStream}s,
     *        then they are wrapped in a new {@code PrintStream} with
     *        this as the additional constructor parameter.
     */
    protected Application(
            final OutputStream out,
            final OutputStream err,
            final boolean autoFlush) {
        this.out = out instanceof PrintStream
                ? (PrintStream) out
                : new PrintStream(out, autoFlush);
        this.err = err instanceof PrintStream
                ? (PrintStream) err
                : new PrintStream(err, autoFlush);
    }

    /**
     * Runs the setup phase.
     * <p>
     * This method is {@link #run run} only once at the start of the life
     * cycle.
     * Its task is to configure the default behavior of the TrueZIP File* API
     * in order to answer the following questions:
     * <ul>
     * <li>What are the file suffixes which shall be recognized as archive
     *     files and hence as virtual directories?
     * <li>Shall missing archive files and directory entries get automatically
     *     created whenever required?
     * </ul>
     * <p>
     * The implementation in the class {@link Application} configures
     * the type of the feedback when prompting the user for keys for RAES
     * encrypted ZIP alias ZIP.RAES alias TZP files by the Swing based
     * prompting key manager.
     * If this JVM is running in headless mode, then this configuration is
     * ignored and the user is prompted by the console I/O based prompting
     * key manager.
     */
    @Override
    protected void setup() {
        String spec = InvalidKeyFeedback.class.getName();
        String impl = HurlingWindowFeedback.class.getName();
        System.setProperty(spec, System.getProperty(spec, impl));
    }

    /**
     * Runs the work phase by calling {@link #runChecked}.
     * Prints a user readable error message to the error output stream
     * which was provided to the constructor if an {@link Exception occurs}.
     * <p>
     * This method is {@link #run run} at least once and repeatedly called
     * until {@link #runChecked} returns a non-negative integer for use as the
     * {@link System#exit(int) exist status} of the VM.
     * After this method, the {@link #sync} method is called in a
     * finally-block.
     * 
     * @param  args an array of arguments for this command line utility.
     * @return A negative integer in order to continue calling this method
     *         in a loop.
     *         Otherwise, the return value is used as the
     *         {@link System#exit(int) exit status} of the VM.
     * @throws RuntimeException at the discretion of {@link #runChecked}.
     */
    @Override
    protected final int work(final String[] args) {
        try {
            return runChecked(args);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            err.println(ex.getLocalizedMessage());
            return 1;
        }
    }

    /**
     * Runs this command line utility.
     * Throws an exception if an error occurs.
     * <p>
     * Avoid repeating this method and updating the same archive file upon
     * each call!
     * This would degrade the overall performance from O(n) to O(m*n),
     * where m is the number of new or modified entries and n is the number
     * of all entries in the archive file!
     *
     * @param  args an array of arguments for this command line utility.
     * @return A negative integer in order to continue calling this method
     *         in a loop.
     *         Otherwise, the return value is used as the
     *         {@link System#exit(int) exit status} of the VM.
     * @throws Exception on any exception.
     */
    protected abstract int runChecked(String[] args) throws Exception;

    /** Indicates illegal application parameters. */
    protected static class IllegalUsageException extends Exception {
        private static final long serialVersionUID = 1985623981423542464L;

        protected IllegalUsageException(String msg) {
            super(msg);
        }
    } // class IllegalUsageException
}
