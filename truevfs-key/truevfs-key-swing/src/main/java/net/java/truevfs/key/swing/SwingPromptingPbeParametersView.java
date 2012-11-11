/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.swing;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import java.util.zip.Deflater;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import javax.swing.JOptionPane;
import net.java.truevfs.key.spec.KeyStrength;
import net.java.truevfs.key.spec.PbeParameters;
import net.java.truevfs.key.spec.UnknownKeyException;
import net.java.truevfs.key.spec.prompting.KeyPromptingDisabledException;
import net.java.truevfs.key.spec.prompting.KeyPromptingInterruptedException;
import net.java.truevfs.key.spec.prompting.PromptingKey;
import net.java.truevfs.key.spec.prompting.PromptingKey.Controller;
import net.java.truevfs.key.spec.prompting.PromptingPbeParameters;
import net.java.truevfs.key.swing.feedback.Feedback;
import net.java.truevfs.key.swing.sl.InvalidKeyFeedbackLocator;
import net.java.truevfs.key.swing.sl.UnknownKeyFeedbackLocator;
import net.java.truevfs.key.swing.util.Windows;

/**
 * A Swing based user interface for prompting for passwords or key files.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class SwingPromptingPbeParametersView<
        P extends PromptingPbeParameters<P, S>,
        S extends KeyStrength>
implements PromptingKey.View<P> {

    private static final ResourceBundle resources = ResourceBundle
            .getBundle(SwingPromptingPbeParametersView.class.getName());

    static final URI INITIAL_RESOURCE = URI.create(""); // NOI18N

    /**
     * This is the number of bytes to load from the beginning of a key file.
     * A valid key file for encryption must contain at least this number of
     * bytes!
     */
    // Must be a multiple of 2 and must be long enough so that
    // GZIPOutputStream most likely produces more than 2 * 256 / 8 bytes
    // output.
    public static final int KEY_FILE_LEN = 512;

    private static final Map<URI, ReadKeyPanel> readKeyPanels = new WeakHashMap<>();

    /**
     * The last resource ID used when prompting.
     * Initialized to the empty string.
     */
    static volatile URI lastResource = INITIAL_RESOURCE;

    private volatile Provider<Feedback>
            unknownKeyFeedbackProvider = UnknownKeyFeedbackLocator.SINGLETON;
    private volatile Provider<Feedback>
            invalidKeyFeedbackProvider = InvalidKeyFeedbackLocator.SINGLETON;

    /**
     * Returns new parameters for safe password based encryption.
     *
     * @return New parameters for safe password based encryption.
     */
    protected abstract P newPbeParameters();

    static void setPassword(
            final PbeParameters<?, ?> param,
            final File keyFile,
            final boolean check)
    throws IOException, WeakKeyException {
        if (check && keyFile.canWrite())
            throw new IOException(resources.getString("keyFile.canWrite"));
        final byte[] key;
        try {
            key = readKeyFile(keyFile);
        } catch (final FileNotFoundException ex) {
            throw new IOException(resources.getString("keyFile.fileNotFoundException"), ex);
        } catch (final EOFException ex) {
            throw new IOException(resources.getString("keyFile.eofException"), ex);
        } catch (final IOException ex) {
            throw new IOException(resources.getString("keyFile.ioException"), ex);
        }
        try {
            if (check) checkKeyEntropy(key);
            setPassword(param, key);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    /**
     * Checks the entropy of the given key bytes.
     *
     * @param key the key to check.
     * @throws WeakKeyException if the entropy of the given key is too weak.
     */
    private static void checkKeyEntropy(byte[] key) throws WeakKeyException {
        Deflater def = new Deflater();
        def.setInput(key);
        def.finish();
        assert def.getTotalOut() == 0;
        final int n = def.deflate(new byte[key.length * 2]);
        assert def.getTotalOut() == n;
        def.end();
        if (n < 2 * 256 / 8) // see RandomAccessEncryptionSpecification
            throw new WeakKeyException(resources.getString("keyFile.badEntropy"));
    }

    /**
     * Reads the encryption key as a char array from the given file.
     *
     * @throws FileNotFoundException if the file cannot get opened for reading.
     * @throws EOFException If the file is not at least {@code KEY_FILE_LEN}
     *         bytes long.
     * @throws IOException on any other I/O related issue.
     */
    private static byte[] readKeyFile(File file)
    throws FileNotFoundException, EOFException, IOException {
        final byte[] buf = new byte[KEY_FILE_LEN];
        try (final FileInputStream _ = new FileInputStream(file)) {
            new DataInputStream(_).readFully(buf);
        }
        return buf;
    }

    private static void setPassword(final PbeParameters<?, ?> param, final byte[] key) {
        final char[] password = decode(key);
        try {
            param.setPassword(password);
        } finally {
            Arrays.fill(password, (char) 0);
        }
    }

    /**
     * Decode the UTF-16BE encoded bytes.
     * This preserves the byte order when the password char array is later
     * encoded to a byte array again in accordance with PKCS #12, section B.1.
     *
     * @param  bytes the UTF16-BE encoded bytes.
     * @return a new array with the decoded character.
     */
    private static char[] decode(final byte[] bytes) {
        final CharBuffer cb = ByteBuffer.wrap(bytes).asCharBuffer();
        final char[] chars = new char[cb.remaining()];
        cb.get(chars);
        return chars;
    }

    Provider<Feedback> getUnknownKeyFeedbackProvider() {
        return unknownKeyFeedbackProvider;
    }

    void setUnkownKeyFeedbackProvider(final Provider<Feedback> provider) {
        unknownKeyFeedbackProvider = provider;
    }

    Provider<Feedback> getInvalidKeyFeedbackProvider() {
        return invalidKeyFeedbackProvider;
    }

    void setInvalidKeyFeedback(final Provider<Feedback> provider) {
        invalidKeyFeedbackProvider = provider;
    }

    @Override
    public void promptKeyForWriting(
            final Controller<P> controller)
    throws UnknownKeyException {
        class PromptKeyForWriting implements Runnable {
            @Override
            public void run() { promptKeyForWritingOnEDT(controller); }
        } // PromptKeyForWriting

        multiplexToEDT(new PromptKeyForWriting());
    }

    /**
     * This method is only called by the AWT Event Dispatch Thread,
     * so it doesn't need to be thread safe.
     */
    void promptKeyForWritingOnEDT(
            final Controller<P> controller) {
        assert EventQueue.isDispatchThread();

        final URI resource = controller.getResource();
        assert null != resource;

        P param = controller.getKeyClone();
        if (null == param) param = newPbeParameters();

        final KeyStrengthPanel<S> keyStrengthPanel = new KeyStrengthPanel<>(
                param.getAllKeyStrengths());
        keyStrengthPanel.setKeyStrength(param.getKeyStrength());
        final WriteKeyPanel keyPanel = new WriteKeyPanel();
        keyPanel.setExtraDataUI(keyStrengthPanel);

        final Window parent = Windows.getParentWindow();
        while (!Thread.interrupted()) { // test and clear status!
            // Setting this inside the loop has the side effect of
            // de-highlighting the resource ID in the panel if the
            // loop iteration has to be repeated due to an invalid
            // user input.
            keyPanel.setResource(resource);
            keyPanel.setFeedback(null != keyPanel.getError()
                    ? getInvalidKeyFeedbackProvider().get()
                    : getUnknownKeyFeedbackProvider().get());

            final int result = JOptionPane.showConfirmDialog(
                    parent,
                    keyPanel,
                    resources.getString("writeKeyDialog.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            /*if (Thread.interrupted()) // test and clear status!
                break;*/

            if (result != JOptionPane.OK_OPTION) break; // reuse old key

            if (keyPanel.updateParam(param)) { // valid input?
                param.setKeyStrength(keyStrengthPanel.getKeyStrength());
                break;
            }

            // Continue looping until valid input.
            assert keyPanel.getError() != null;
        }

        controller.setKeyClone(param);
    }

    @Override
    public void promptKeyForReading(
            final Controller<P> controller,
            final boolean invalid)
    throws UnknownKeyException {
        class PromptKeyForReading implements Runnable {
            @Override
            public void run() {
                promptKeyForReadingOnEDT(controller, invalid);
            }
        } // PromptKeyForReading

        multiplexToEDT(new PromptKeyForReading());
    }

    /**
     * This method is only called by the AWT Event Dispatch Thread,
     * so it doesn't need to be thread safe.
     */
    void promptKeyForReadingOnEDT(
            final Controller<P> controller,
            final boolean invalid) {
        assert EventQueue.isDispatchThread();

        final URI resource = controller.getResource();
        assert null != resource;

        final ReadKeyPanel keyPanel;
        if (invalid) {
            final ReadKeyPanel panel = readKeyPanels.get(resource);
            if (panel != null) {
                keyPanel = panel;
            } else {
                keyPanel = new ReadKeyPanel();
            }
            keyPanel.setError(resources.getString("invalidKey"));
        } else {
            keyPanel = new ReadKeyPanel();
        }
        readKeyPanels.put(resource, keyPanel);

        final Window parent = Windows.getParentWindow();
        while (!Thread.interrupted()) { // test and clear status!
            // Setting this inside the loop has the side effect of
            // de-highlighting the resource ID in the panel if the
            // loop iteration has to be repeated due to an invalid
            // user input.
            keyPanel.setResource(resource);
            keyPanel.setFeedback(null != keyPanel.getError()
                    ? getInvalidKeyFeedbackProvider().get()
                    : getUnknownKeyFeedbackProvider().get());

            final int result = JOptionPane.showConfirmDialog(
                    parent,
                    keyPanel,
                    resources.getString("readKeyDialog.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            /*if (Thread.interrupted()) // test and clear status!
                break;*/

            if (result != JOptionPane.OK_OPTION) {
                controller.setKeyClone(null);
                return;
            }

            final P param = newPbeParameters();
            if (keyPanel.updateParam(param)) { // valid input?
                param.setChangeRequested(keyPanel.isChangeKeySelected());
                controller.setKeyClone(param);
                return;
            }

            // Continue looping until valid input.
            assert keyPanel.getError() != null;
        }
    }

    /**
     * Invokes the given {@code task} on the AWT Event Dispatching Thread
     * (EDT) and waits until it's finished.
     * <p>
     * In multithreaded environments, although technically possible,
     * do not allow multiple threads to prompt for a key concurrently,
     * because this would only confuse users.
     * By explicitly locking the class object rather than the instance,
     * we enforce this even if multiple implementations and instances
     * are used.
     * <p>
     * If the current thread is interrupted, an
     * {@link UndeclaredThrowableException} is thrown with a
     * {@link KeyPromptingInterruptedException} as its cause.
     * <p>
     * If a {@link Throwable} is thrown by the EDT, then it's wrapped in an
     * {@link UndeclaredThrowableException} and re-thrown by this thread.
     */
    private static void multiplexToEDT(final Runnable task)
    throws UnknownKeyException {
        if (GraphicsEnvironment.isHeadless())
            throw new KeyPromptingDisabledException();

        if (EventQueue.isDispatchThread()) {
            task.run();
        } else {
            synchronized (SwingPromptingPbeParametersView.class) {
                try {
                    EventQueue.invokeAndWait(task);
                } catch (InterruptedException interrupt) {
                    throw new KeyPromptingInterruptedException(interrupt);
                } catch (InvocationTargetException failure) {
                    throw new UnknownKeyException(failure);
                }
            }
        }
    }
}
