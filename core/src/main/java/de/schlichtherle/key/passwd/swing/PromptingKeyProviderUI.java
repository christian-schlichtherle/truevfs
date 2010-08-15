/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.key.passwd.swing;

import de.schlichtherle.awt.EventQueue;
import de.schlichtherle.awt.EventDispatchTimeoutException;
import de.schlichtherle.key.*;

import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import javax.swing.*;

/**
 * A Swing based user interface to prompt for passwords or key files.
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.0
 */
public class PromptingKeyProviderUI
        implements de.schlichtherle.key.PromptingKeyProviderUI {

    private static final String PACKAGE_NAME
            = "de/schlichtherle/key/passwd/swing".replace('/', '.'); // support code obfuscation!
    private static final String CLASS_NAME
            = PACKAGE_NAME + "/PromptingKeyProviderUI".replace('/', '.'); // support code obfuscation!
    private static final ResourceBundle resources = ResourceBundle.getBundle(CLASS_NAME);
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

    /**
     * The timeout for the EDT to <em>start</em> prompting for a key in
     * milliseconds.
     */
    private static final long START_PROMPTING_TIMEOUT = 1000;

    /**
     * This is the number of bytes to load from the beginning of a key file.
     * A valid key file for encryption must contain at least this number of
     * bytes!
     */
    // Must be a multiple of 2 and must be long enough so that
    // GZIPOutputStream most likely produces more than 2 * 256 / 8 bytes
    // output.
    public static final int KEY_FILE_LEN = 512;

    private static final Map openKeyPanels = new WeakHashMap();

    /**
     * The last resource ID used when prompting.
     * Initialized to the empty string.
     */
    static String lastResourceID = "";

    /**
     * @deprecated This field is not used anymore and will be removed for the
     *             next major release number.
     */
    private CreateKeyPanel createKeyPanel;

    /**
     * @deprecated This field is not used anymore and will be removed for the
     *             next major release number.
     */
    private OpenKeyPanel openKeyPanel;
    
    private Feedback unknownCreateKeyFeedback;
    private Feedback invalidCreateKeyFeedback;
    private Feedback unknownOpenKeyFeedback;
    private Feedback invalidOpenKeyFeedback;

    /**
     * Reads the encryption key as a byte sequence from the given pathname
     * into a buffer of exactly <code>KEY_FILE_LEN</code> bytes and returns it.
     *
     * @throws EOFException If the file is not at least <code>KEY_FILE_LEN</code>
     *         bytes long.
     * @throws IOException If an IOException occurs when opening, reading or
     *         closing the file.
     */
    static byte[] readKeyFile(String pathname)
    throws IOException {
        final byte[] buf = new byte[KEY_FILE_LEN];

        final RandomAccessFile raf = new RandomAccessFile(pathname, "r");
        try {
            raf.readFully(buf);
        } finally {
            raf.close();
        }

        return buf;
    }

    /**
     * @deprecated This method is not used anymore and will be removed for the
     *             next major release number.
     *             It's use may dead lock the GUI.
     *             Use {@link #createCreateKeyPanel} instead.
     */
    protected CreateKeyPanel getCreateKeyPanel() {
        if (createKeyPanel == null)
            createKeyPanel = createCreateKeyPanel();
        return createKeyPanel;
    }

    /**
     * A factory method to create the Create Protected Resource Key Panel.
     */
    protected CreateKeyPanel createCreateKeyPanel() {
        return new CreateKeyPanel();
    }

    /**
     * @deprecated This method is not used anymore and will be removed for the
     *             next major release number.
     *             It's use may dead lock the GUI.
     *             Use {@link #createOpenKeyPanel} instead.
     */
    protected OpenKeyPanel getOpenKeyPanel() {
        if (openKeyPanel == null)
            openKeyPanel = createOpenKeyPanel();
        return openKeyPanel;
    }

    /**
     * A factory method to create the Open Protected Resource Key Panel.
     */
    protected OpenKeyPanel createOpenKeyPanel() {
        return new OpenKeyPanel();
    }

    protected Feedback getUnknownCreateKeyFeedback() {
        if (unknownCreateKeyFeedback == null)
            unknownCreateKeyFeedback = createFeedback("UnknownCreateKeyFeedback");
        return unknownCreateKeyFeedback;
    }

    protected void setUnkownCreateKeyFeedback(final Feedback uckf) {
        this.unknownCreateKeyFeedback = uckf;
    }

    protected Feedback getInvalidCreateKeyFeedback() {
        if (invalidCreateKeyFeedback == null)
            invalidCreateKeyFeedback = createFeedback("InvalidCreateKeyFeedback");
        return invalidCreateKeyFeedback;
    }

    protected void setInvalidCreateKeyFeedback(final Feedback ickf) {
        this.invalidCreateKeyFeedback = ickf;
    }

    protected Feedback getUnknownOpenKeyFeedback() {
        if (unknownOpenKeyFeedback == null)
            unknownOpenKeyFeedback = createFeedback("UnknownOpenKeyFeedback");
        return unknownOpenKeyFeedback;
    }

    protected void setUnknownOpenKeyFeedback(final Feedback uokf) {
        this.unknownOpenKeyFeedback = uokf;
    }

    protected Feedback getInvalidOpenKeyFeedback() {
        if (invalidOpenKeyFeedback == null)
            invalidOpenKeyFeedback = createFeedback("InvalidOpenKeyFeedback");
        return invalidOpenKeyFeedback;
    }

    protected void setInvalidOpenKeyFeedback(final Feedback iokf) {
        this.invalidOpenKeyFeedback = iokf;
    }

    private static Feedback createFeedback(final String type) {
        try {
            String n = System.getProperty(
                    PACKAGE_NAME + "." + type,
                    PACKAGE_NAME + ".Basic" + type);
            ClassLoader l = Thread.currentThread().getContextClassLoader();
            if (l == null)
                l = ClassLoader.getSystemClassLoader();
            Class c = l.loadClass(n);
            Feedback f = (Feedback) c.newInstance();
            return f;
        } catch (ClassNotFoundException ex) {
            logger.log(Level.WARNING, "", ex);
        } catch (IllegalAccessException ex) {
            logger.log(Level.WARNING, "", ex);
        } catch (InstantiationException ex) {
            logger.log(Level.WARNING, "", ex);
        }
        return null;
    }

    public /*synchronized*/ final void promptCreateKey(
            final PromptingKeyProvider provider) {
        final Runnable task = new Runnable() {
            public void run() {
                promptCreateKey(provider, null);
            }
        };
        multiplexOnEDT(task); // synchronized on class instance!
    }

    public /*synchronized*/ final boolean promptUnknownOpenKey(
            final PromptingKeyProvider provider) {
        final BooleanRunnable task = new BooleanRunnable() {
            public void run() {
                result = promptOpenKey(provider, false, null);
            }
        };
        multiplexOnEDT(task); // synchronized on class instance!
        return task.result;
    }

    public /*synchronized*/ final boolean promptInvalidOpenKey(
            final PromptingKeyProvider provider) {
        final BooleanRunnable task = new BooleanRunnable() {
            public void run() {
                result = promptOpenKey(provider, true, null);
            }
        };
        multiplexOnEDT(task); // synchronized on class instance!
        return task.result;
    }

    private abstract static class BooleanRunnable implements Runnable {
        public boolean result;
    }

    /**
     * This method is only called by the AWT Event Dispatch Thread,
     * so it doesn't need to be thread safe.
     */
    protected void promptCreateKey(
            final PromptingKeyProvider provider,
            final JComponent extraDataUI) {
        assert EventQueue.isDispatchThread();

        final CreateKeyPanel createKeyPanel = createCreateKeyPanel();
        createKeyPanel.setExtraDataUI(extraDataUI);

        final Window parent = PromptingKeyManager.getParentWindow();
        try {
            while (!Thread.interrupted()) { // test and clear status!
                // Setting this inside the loop has the side effect of
                // de-highlighting the resource ID in the panel if the
                // loop iteration has to be repeated due to an invalid
                // user input.
                createKeyPanel.setResourceID(provider.getResourceID());
                createKeyPanel.setFeedback(createKeyPanel.getError() != null
                        ? getInvalidCreateKeyFeedback()
                        : getUnknownCreateKeyFeedback());

                final int result;
                try {
                    result = JOptionPane.showConfirmDialog(
                            parent,
                            createKeyPanel,
                            resources.getString("newPasswdDialog.title"),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                } catch (StackOverflowError failure) {
                    // Workaround for bug ID #6471418 - should be fixed in
                    // JSE 1.5.0_11
                    boolean interrupted = Thread.interrupted(); // test and clear status!
                    assert interrupted;
                    break;
                }
                if (Thread.interrupted()) // test and clear status!
                    break;

                if (result != JOptionPane.OK_OPTION)
                    break; // reuse old key

                final Object createKey = createKeyPanel.getCreateKey();
                if (createKey != null) { // valid input?
                    provider.setKey(createKey);
                    break;
                }

                // Continue looping until valid input.
                assert createKeyPanel.getError() != null;
            }
        } finally {
            // Do NOT do this within the loop - would close the next
            // JOptionPane on repeat.
            eventuallyDispose(parent);
        }
    }

    /**
     * This method is only called by the AWT Event Dispatch Thread,
     * so it doesn't need to be thread safe.
     */
    protected boolean promptOpenKey(
            final PromptingKeyProvider provider,
            final boolean invalid,
            final JComponent extraDataUI) {
        assert EventQueue.isDispatchThread();

        final OpenKeyPanel openKeyPanel;
        if (invalid) {
            OpenKeyPanel panel = (OpenKeyPanel) openKeyPanels.get(provider);
            openKeyPanel = panel != null ? panel : createOpenKeyPanel();
            openKeyPanel.setError(resources.getString("invalidKey"));
        } else {
            openKeyPanel = createOpenKeyPanel();
            openKeyPanel.setExtraDataUI(extraDataUI);
        }
        openKeyPanels.put(provider, openKeyPanel);

        final Window parent = PromptingKeyManager.getParentWindow();
        try {
            while (!Thread.interrupted()) { // test and clear status!
                // Setting this inside the loop has the side effect of
                // de-highlighting the resource ID in the panel if the
                // loop iteration has to be repeated due to an invalid
                // user input.
                openKeyPanel.setResourceID(provider.getResourceID());
                openKeyPanel.setFeedback(openKeyPanel.getError() != null
                        ? getInvalidOpenKeyFeedback()
                        : getUnknownOpenKeyFeedback());

                final int result;
                try {
                    result = JOptionPane.showConfirmDialog(
                            parent,
                            openKeyPanel,
                            resources.getString("passwdDialog.title"),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                } catch (StackOverflowError failure) {
                    // Workaround for bug ID #6471418 - should be fixed in
                    // JSE 1.5.0_11
                    boolean interrupted = Thread.interrupted(); // test and clear status!
                    assert interrupted;
                    break;
                }
                if (Thread.interrupted()) // test and clear status!
                    break;

                if (result != JOptionPane.OK_OPTION) {
                    provider.setKey(null);
                    break;
                }

                final Object openKey = openKeyPanel.getOpenKey();
                if (openKey != null) { // valid input?
                    provider.setKey(openKey);
                    break;
                }

                // Continue looping until valid input.
                assert openKeyPanel.getError() != null;
            }
        } finally {
            // Do NOT do this within the loop - would close the next
            // JOptionPane on repeat.
            eventuallyDispose(parent);
        }

        return openKeyPanel.isKeyChangeRequested();
    }

    /**
     * The following is a double work around for Sun's J2SE 1.4.2
     * which has been tested with 1.4.2-b28 and 1.4.2_12-b03 on the
     * Windows platform.
     * The issue for which this work around is required is known
     * to be present in the Java code of the AWT package, so this
     * should pertain to all platforms.
     * This issue has been fixed with Sun's JSE 1.5.0-b64 or higher.
     * <p>
     * The issue is that an application will not terminate until all
     * Window's have been dispose()d or System.exit() has been called -
     * it is not sufficient just to hide() all Window's.
     * <p>
     * The JOptionPane properly dispose()s its Dialog which displays our
     * password panels.
     * However, by default <code>JOptionPane</code> uses an internal
     * <code>Frame</code> as its parent window if the application does not
     * specify a parent window explicitly.
     * This class also uses this frame unless the client application has
     * called {@link PromptingKeyManager#setParentWindow(Window)}.
     * <code>JOptionPane</code> never dispose()s the parent window, so the
     * client application may eventually not terminate.
     * <p>
     * The workaround is to dispose the parent window if it's not showing.
     */
    private static void eventuallyDispose(final Window window) {
        if (!window.isShowing())
            window.dispose();
    }

    /**
     * Invokes the given <code>task</code> on the AWT Event Dispatching Thread
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
    private static void multiplexOnEDT(final Runnable task) {
        if (Thread.interrupted())
            throw new UndeclaredThrowableException(
                    new KeyPromptingInterruptedException());

        if (EventQueue.isDispatchThread()) {
            task.run();
        } else {
            synchronized (PromptingKeyProviderUI.class) {
                try {
                    EventQueue.invokeAndWaitUninterruptibly(
                            task, START_PROMPTING_TIMEOUT);
                } catch (EventDispatchTimeoutException failure) {
                    // Timeout while waiting for the EDT to start the task.
                    // Now wrap this in two exceptions: The outermost exception will
                    // be catched by the PromptingKeyProvider class and its cause
                    // will be unwrapped and passed on to the client application by
                    // the PromptingKeyProvider class.
                    throw new UndeclaredThrowableException(
                            new KeyPromptingTimeoutException(failure));
                /*} catch (InterruptedException failure) {
                    // We've been interrupted while waiting for the EDT.
                    // Now wrap this in two exceptions: The outermost exception will
                    // be catched by the PromptingKeyProvider class and its cause
                    // will be unwrapped and passed on to the client application by
                    // the PromptingKeyProvider class.
                    throw new UndeclaredThrowableException(
                            new KeyPromptingInterruptedException(failure));
                */} catch (InvocationTargetException failure) {
                    throw new UndeclaredThrowableException(failure);
                } finally {
                    Thread.interrupted();
                }
            }
        }
    }
}
