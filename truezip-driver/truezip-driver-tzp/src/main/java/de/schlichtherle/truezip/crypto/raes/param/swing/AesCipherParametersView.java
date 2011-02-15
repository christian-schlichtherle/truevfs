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

    private static final Map<URI, OpenKeyPanel>
            openKeyPanels = new WeakHashMap<URI, OpenKeyPanel>();

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
    public void promptCreateKey(
            final Controller<? super AesCipherParameters> controller)
    throws UnknownKeyException {
        class PromptCreateKey implements Runnable {
            @Override
            public void run() {
                promptCreateKeyEDT(controller);
            }
        } // class PromptCreateKey
        multiplexOnEDT(new PromptCreateKey()); // synchronized on class instance!
    }

    @Override
    public void promptOpenKey(
            final Controller<? super AesCipherParameters> controller,
            final boolean invalid)
    throws UnknownKeyException {
        class PromptOpenKey implements Runnable {
            @Override
            public void run() {
                promptOpenKeyEDT(controller, invalid);
            }
        } // class PromptOpenKey
        multiplexOnEDT(new PromptOpenKey()); // synchronized on class instance!
    }

    /**
     * This method is only called by the AWT Event Dispatch Thread,
     * so it doesn't need to be thread safe.
     */
    private void promptCreateKeyEDT(
            final Controller<? super AesCipherParameters> controller) {
        assert EventQueue.isDispatchThread();

        final URI resource = controller.getResource();
        final AesCipherParameters param = new AesCipherParameters();

        final AesKeyStrengthPanel keyStrengthPanel = new AesKeyStrengthPanel();
        keyStrengthPanel.setKeyStrength(param.getKeyStrength());
        final CreateKeyPanel createKeyPanel = new CreateKeyPanel();
        createKeyPanel.setExtraDataUI(keyStrengthPanel);

        final Window parent = Windows.getParentWindow();
        while (!Thread.interrupted()) { // test and clear status!
            // Setting this inside the loop has the side effect of
            // de-highlighting the resource ID in the panel if the
            // loop iteration has to be repeated due to an invalid
            // user input.
            assert null != resource : "violation of contract for PromptingKeyProviderUI";
            createKeyPanel.setResource(resource);
            createKeyPanel.setFeedback(createKeyPanel.getError() != null
                    ? getInvalidKeyFeedback()
                    : getUnknownKeyFeedback());

            final int result = JOptionPane.showConfirmDialog(
                    parent,
                    createKeyPanel,
                    resources.getString("newPasswdDialog.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            /*if (Thread.interrupted()) // test and clear status!
                break;*/

            if (result != JOptionPane.OK_OPTION)
                break; // reuse old key

            if (createKeyPanel.updateCreateKey(param)) { // valid input?
                param.setKeyStrength(keyStrengthPanel.getKeyStrength());
                controller.setKey(param);
                break;
            }

            // Continue looping until valid input.
            assert createKeyPanel.getError() != null;
        }
    }

    /**
     * This method is only called by the AWT Event Dispatch Thread,
     * so it doesn't need to be thread safe.
     */
    private void promptOpenKeyEDT(
            final Controller<? super AesCipherParameters> controller,
            final boolean invalid) {
        assert EventQueue.isDispatchThread();

        final URI resource = controller.getResource();
        final AesCipherParameters param = new AesCipherParameters();

        final OpenKeyPanel openKeyPanel;
        if (invalid) {
            final OpenKeyPanel panel = openKeyPanels.get(resource);
            if (panel != null) {
                openKeyPanel = panel;
            } else {
                openKeyPanel = new OpenKeyPanel();
            }
            openKeyPanel.setError(resources.getString("invalidKey"));
        } else {
            openKeyPanel = new OpenKeyPanel();
        }
        openKeyPanels.put(resource, openKeyPanel);

        final Window parent = Windows.getParentWindow();
        while (!Thread.interrupted()) { // test and clear status!
            // Setting this inside the loop has the side effect of
            // de-highlighting the resource ID in the panel if the
            // loop iteration has to be repeated due to an invalid
            // user input.
            assert resource != null : "violation of contract for PromptingKeyProviderUI";
            openKeyPanel.setResource(resource);
            openKeyPanel.setFeedback(null != openKeyPanel.getError()
                    ? getInvalidKeyFeedback()
                    : getUnknownKeyFeedback());

            final int result = JOptionPane.showConfirmDialog(
                    parent,
                    openKeyPanel,
                    resources.getString("passwdDialog.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            /*if (Thread.interrupted()) // test and clear status!
                break;*/

            if (result != JOptionPane.OK_OPTION) {
                controller.setKey(null);
                break;
            }

            if (openKeyPanel.updateOpenKey(param)) { // valid input?
                controller.setKey(param);
                controller.setChangeRequested(openKeyPanel.isChangeKeySelected());
                break;
            }

            // Continue looping until valid input.
            assert openKeyPanel.getError() != null;
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
