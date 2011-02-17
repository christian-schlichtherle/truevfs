/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.crypto.raes.param.swing;

import de.schlichtherle.truezip.awt.Windows;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.KeyPromptingInterruptedException;
import de.schlichtherle.truezip.key.PromptingKeyProvider.Controller;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import de.schlichtherle.truezip.key.UnknownKeyException;
import de.schlichtherle.truezip.util.ServiceLocator;
import java.awt.EventQueue;
import java.awt.Window;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import javax.swing.JOptionPane;

/**
 * A Swing based user interface to prompt for passwords or key files.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class AesCipherParametersView
implements View<AesCipherParameters> {

    private static final String CLASS_NAME
            = AesCipherParametersView.class.getName();
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);
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

    private static final Map<URI, ReadKeyPanel>
            readKeyPanels = new WeakHashMap<URI, ReadKeyPanel>();

    private static final ServiceLocator serviceLocator
            = new ServiceLocator(AesCipherParametersView.class.getClassLoader());

    /**
     * The last resource ID used when prompting.
     * Initialized to the empty string.
     */
    static volatile URI lastResource = INITIAL_RESOURCE;

    private volatile UnknownKeyFeedback unknownKeyFeedback;
    private volatile InvalidKeyFeedback invalidKeyFeedback;

    /**
     * Reads the encryption key as a byte sequence from the given pathname
     * into a buffer of exactly {@code KEY_FILE_LEN} bytes and returns it.
     *
     * @throws EOFException If the file is not at least {@code KEY_FILE_LEN}
     *         bytes long.
     * @throws IOException on any other I/O related issue.
     */
    static byte[] readKeyFile(File file) throws IOException {
        final byte[] buf = new byte[KEY_FILE_LEN];
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        try {
            raf.readFully(buf);
        } finally {
            raf.close();
        }
        return buf;
    }

    UnknownKeyFeedback getUnknownKeyFeedback() {
        if (unknownKeyFeedback == null)
            unknownKeyFeedback = serviceLocator.getService(
                    UnknownKeyFeedback.class,
                    BasicUnknownKeyFeedback.class);
        return unknownKeyFeedback;
    }

    void setUnkownKeyFeedback(final UnknownKeyFeedback uckf) {
        this.unknownKeyFeedback = uckf;
    }

    InvalidKeyFeedback getInvalidKeyFeedback() {
        if (invalidKeyFeedback == null)
            invalidKeyFeedback = serviceLocator.getService(
                    InvalidKeyFeedback.class,
                    BasicInvalidKeyFeedback.class);
        return invalidKeyFeedback;
    }

    void setInvalidKeyFeedback(final InvalidKeyFeedback ickf) {
        this.invalidKeyFeedback = ickf;
    }

    @Override
    public void promptWriteKey(
            final Controller<? super AesCipherParameters> controller)
    throws UnknownKeyException {
        class PromptWriteKey implements Runnable {
            @Override
            public void run() {
                promptWriteKeyEDT(controller);
            }
        } // class PromptWriteKey
        multiplexOnEDT(new PromptWriteKey()); // synchronized on class instance!
    }

    /**
     * This method is only called by the AWT Event Dispatch Thread,
     * so it doesn't need to be thread safe.
     */
    private void promptWriteKeyEDT(
            final Controller<? super AesCipherParameters> controller) {
        assert EventQueue.isDispatchThread();

        final URI resource = controller.getResource();
        AesCipherParameters param = new AesCipherParameters();

        final AesKeyStrengthPanel keyStrengthPanel = new AesKeyStrengthPanel();
        keyStrengthPanel.setKeyStrength(param.getKeyStrength());
        final WriteKeyPanel keyPanel = new WriteKeyPanel();
        keyPanel.setExtraDataUI(keyStrengthPanel);

        final Window parent = Windows.getParentWindow();
        while (!Thread.interrupted()) { // test and clear status!
            // Setting this inside the loop has the side effect of
            // de-highlighting the resource ID in the panel if the
            // loop iteration has to be repeated due to an invalid
            // user input.
            assert null != resource : "violation of contract for PromptingKeyProviderUI";
            keyPanel.setResource(resource);
            keyPanel.setFeedback(keyPanel.getError() != null
                    ? getInvalidKeyFeedback()
                    : getUnknownKeyFeedback());

            final int result = JOptionPane.showConfirmDialog(
                    parent,
                    keyPanel,
                    resources.getString("writeKeyDialog.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            /*if (Thread.interrupted()) // test and clear status!
                break;*/

            if (result != JOptionPane.OK_OPTION)
                break; // reuse old key

            if (keyPanel.updateParam(param)) { // valid input?
                param.setKeyStrength(keyStrengthPanel.getKeyStrength());
                controller.setKey(param);
                break;
            }

            // Continue looping until valid input.
            assert keyPanel.getError() != null;
        }
    }

    @Override
    public void promptReadKey(
            final Controller<? super AesCipherParameters> controller,
            final boolean invalid)
    throws UnknownKeyException {
        class PromptReadKey implements Runnable {
            @Override
            public void run() {
                promptReadKeyEDT(controller, invalid);
            }
        } // class PromptReadKey
        multiplexOnEDT(new PromptReadKey()); // synchronized on class instance!
    }

    /**
     * This method is only called by the AWT Event Dispatch Thread,
     * so it doesn't need to be thread safe.
     */
    private void promptReadKeyEDT(
            final Controller<? super AesCipherParameters> controller,
            final boolean invalid) {
        assert EventQueue.isDispatchThread();

        final URI resource = controller.getResource();
        final AesCipherParameters param = new AesCipherParameters();

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
            assert resource != null : "violation of contract for PromptingKeyProviderUI";
            keyPanel.setResource(resource);
            keyPanel.setFeedback(null != keyPanel.getError()
                    ? getInvalidKeyFeedback()
                    : getUnknownKeyFeedback());

            final int result = JOptionPane.showConfirmDialog(
                    parent,
                    keyPanel,
                    resources.getString("readKeyDialog.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            /*if (Thread.interrupted()) // test and clear status!
                break;*/

            if (result != JOptionPane.OK_OPTION) {
                controller.setKey(null);
                break;
            }

            if (keyPanel.updateParam(param)) { // valid input?
                controller.setKey(param);
                controller.setChangeRequested(keyPanel.isChangeKeySelected());
                break;
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
    private static void multiplexOnEDT(final Runnable task)
    throws UnknownKeyException {
        /*if (Thread.interrupted())
            throw new KeyPromptingInterruptedException();*/

        if (EventQueue.isDispatchThread()) {
            task.run();
        } else {
            synchronized (AesCipherParametersView.class) {
                try {
                    EventQueue.invokeAndWait(task);
                } catch (InterruptedException failure) {
                    throw new KeyPromptingInterruptedException(failure);
                } catch (InvocationTargetException ex) {
                    throw new UnknownKeyException(ex);
                /*} finally {
                    Thread.interrupted();*/
                }
            }
        }
    }
}
