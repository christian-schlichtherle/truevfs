/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.samples.file;

import de.schlichtherle.truezip.key.pbe.swing.feedback.HurlingWindowFeedback;
import de.schlichtherle.truezip.key.pbe.swing.feedback.InvalidKeyFeedback;
import de.truezip.file.TApplication;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Abstract base class for command line utilities.
 *
 * @author Christian Schlichtherle
 */
public abstract class Application extends TApplication<RuntimeException> {

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
     *
     * @param out the standard output stream.
     * @param err the error output stream.
     * @param autoFlush if the output streams are not {@link PrintStream}s,
     *        then they are wrapped in a new {@code PrintStream} with
     *        this value as the additional constructor parameter.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_DEFAULT_ENCODING")
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
     * Configures the type of the feedback when prompting the user for keys
     * for RAES encrypted ZIP alias ZIP.RAES alias TZP files by the Swing
     * based prompting key manager.
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
        } catch (IllegalUsageException ex) {
            err.println(ex.getLocalizedMessage());
            return 1;
        } catch (Exception ex) {
            ex.printStackTrace(err);
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
}