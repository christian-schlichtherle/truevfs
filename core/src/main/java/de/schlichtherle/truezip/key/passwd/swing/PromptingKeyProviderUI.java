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

package de.schlichtherle.truezip.key.passwd.swing;

import de.schlichtherle.truezip.awt.EventDispatchTimeoutException;
import de.schlichtherle.truezip.awt.EventQueue;
import de.schlichtherle.truezip.awt.Windows;
import de.schlichtherle.truezip.key.KeyPromptingInterruptedException;
import de.schlichtherle.truezip.key.KeyPromptingTimeoutException;
import de.schlichtherle.truezip.key.PromptingKeyProvider;
import de.schlichtherle.truezip.key.UnknownKeyException;
import java.awt.Window;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import static de.schlichtherle.truezip.util.ClassLoaders.loadClass;

/**
 * A Swing based user interface to prompt for passwords or key files.
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class PromptingKeyProviderUI<P extends PromptingKeyProvider<Cloneable>>
implements de.schlichtherle.truezip.key.PromptingKeyProviderUI<Cloneable, P> {

    private static final String CLASS_NAME
            = PromptingKeyProviderUI.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);
    private static final String PACKAGE_NAME
            = PromptingKeyProviderUI.class.getPackage().getName();
    static final URI INITIAL_RESOURCE = URI.create("null:/");

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

    private static final Map<PromptingKeyProvider<?>, OpenKeyPanel> openKeyPanels
            = new WeakHashMap<PromptingKeyProvider<?>, OpenKeyPanel>();

    /**
     * The last resource ID used when prompting.
     * Initialized to the empty string.
     */
    static URI lastResource = INITIAL_RESOURCE; // NOI18N

    private Feedback unknownCreateKeyFeedback;
    private Feedback invalidCreateKeyFeedback;
    private Feedback unknownOpenKeyFeedback;
    private Feedback invalidOpenKeyFeedback;

    /**
     * Reads the encryption key as a byte sequence from the given pathname
     * into a buffer of exactly {@code KEY_FILE_LEN} bytes and returns it.
     *
     * @throws EOFException If the file is not at least {@code KEY_FILE_LEN}
     *         bytes long.
     * @throws IOException on any other I/O related issue.
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
     * A factory method to create the Create Protected Resource Key Panel.
     */
    protected CreateKeyPanel newCreateKeyPanel() {
        return new CreateKeyPanel();
    }

    /**
     * A factory method to create the Open Protected Resource Key Panel.
     */
    protected OpenKeyPanel newOpenKeyPanel() {
        return new OpenKeyPanel();
    }

    protected Feedback getUnknownCreateKeyFeedback() {
        if (unknownCreateKeyFeedback == null)
            unknownCreateKeyFeedback = newFeedback("UnknownCreateKeyFeedback");
        return unknownCreateKeyFeedback;
    }

    protected void setUnkownCreateKeyFeedback(final Feedback uckf) {
        this.unknownCreateKeyFeedback = uckf;
    }

    protected Feedback getInvalidCreateKeyFeedback() {
        if (invalidCreateKeyFeedback == null)
            invalidCreateKeyFeedback = newFeedback("InvalidCreateKeyFeedback");
        return invalidCreateKeyFeedback;
    }

    protected void setInvalidCreateKeyFeedback(final Feedback ickf) {
        this.invalidCreateKeyFeedback = ickf;
    }

    protected Feedback getUnknownOpenKeyFeedback() {
        if (unknownOpenKeyFeedback == null)
            unknownOpenKeyFeedback = newFeedback("UnknownOpenKeyFeedback");
        return unknownOpenKeyFeedback;
    }

    protected void setUnknownOpenKeyFeedback(final Feedback uokf) {
        this.unknownOpenKeyFeedback = uokf;
    }

    protected Feedback getInvalidOpenKeyFeedback() {
        if (invalidOpenKeyFeedback == null)
            invalidOpenKeyFeedback = newFeedback("InvalidOpenKeyFeedback");
        return invalidOpenKeyFeedback;
    }

    protected void setInvalidOpenKeyFeedback(final Feedback iokf) {
        this.invalidOpenKeyFeedback = iokf;
    }

    private static Feedback newFeedback(final String type) {
        try {
            String n = System.getProperty(
                    PACKAGE_NAME + "." + type,
                    PACKAGE_NAME + ".Basic" + type);
            Class<?> c = loadClass(n, PromptingKeyProviderUI.class);
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

    @Override
	public void promptCreateKey(final P provider)
    throws UnknownKeyException {
        final Runnable task = new Runnable() {
            @Override
			public void run() {
                promptCreateKey(provider, null);
            }
        };
        multiplexOnEDT(task); // synchronized on class instance!
    }

    @Override
	public boolean promptUnknownOpenKey(final P provider)
    throws UnknownKeyException {
        final BooleanRunnable task = new BooleanRunnable() {
            @Override
			public void run() {
                result = promptOpenKey(provider, false, null);
            }
        };
        multiplexOnEDT(task); // synchronized on class instance!
        return task.result;
    }

    @Override
	public boolean promptInvalidOpenKey(final P provider)
    throws UnknownKeyException {
        final BooleanRunnable task = new BooleanRunnable() {
            @Override
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
            final P provider,
            final JComponent extraDataUI) {
        assert EventQueue.isDispatchThread();

        final CreateKeyPanel createKeyPanel = newCreateKeyPanel();
        createKeyPanel.setExtraDataUI(extraDataUI);

        final Window parent = Windows.getParentWindow();
        try {
            while (!Thread.interrupted()) { // test and clear status!
                // Setting this inside the loop has the side effect of
                // de-highlighting the resource ID in the panel if the
                // loop iteration has to be repeated due to an invalid
                // user input.
                final URI resource = provider.getResource();
                assert resource != null : "violation of contract for PromptingKeyProviderUI";
                createKeyPanel.setResource(resource);
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

                final Cloneable createKey = createKeyPanel.getCreateKey();
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
            final P provider,
            final boolean invalid,
            final JComponent extraDataUI) {
        assert EventQueue.isDispatchThread();

        final OpenKeyPanel openKeyPanel;
        if (invalid) {
            final OpenKeyPanel panel = openKeyPanels.get(provider);
            if (panel != null) {
                openKeyPanel = panel;
            } else {
                openKeyPanel = newOpenKeyPanel();
                openKeyPanel.setExtraDataUI(extraDataUI);
            }
            openKeyPanel.setError(resources.getString("invalidKey"));
        } else {
            openKeyPanel = newOpenKeyPanel();
            openKeyPanel.setExtraDataUI(extraDataUI);
        }
        openKeyPanels.put(provider, openKeyPanel);

        final Window parent = Windows.getParentWindow();
        try {
            while (!Thread.interrupted()) { // test and clear status!
                // Setting this inside the loop has the side effect of
                // de-highlighting the resource ID in the panel if the
                // loop iteration has to be repeated due to an invalid
                // user input.
                final URI resource = provider.getResource();
                assert resource != null : "violation of contract for PromptingKeyProviderUI";
                openKeyPanel.setResource(resource);
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

                final Cloneable openKey = openKeyPanel.getOpenKey();
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
     * The {@code JOptionPane} properly dispose()s its Dialog which displays
     * our password panels.
     * However, by default {@code JOptionPane} uses an internal {@code Frame}
     * as its parent window if the application does not specify a parent
     * window explicitly.
     * {@code JOptionPane} never dispose()s the parent window, so the
     * client application may eventually not terminate.
     * <p>
     * The workaround is to dispose the parent window if it's not showing.
     */
    private static void eventuallyDispose(final Window window) {
        if (!window.isShowing())
            window.dispose();
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
        if (Thread.interrupted())
            throw new KeyPromptingInterruptedException();

        if (EventQueue.isDispatchThread()) {
            task.run();
        } else {
            synchronized (PromptingKeyProviderUI.class) {
                try {
                    EventQueue.invokeAndWaitUninterruptibly(
                            task, START_PROMPTING_TIMEOUT);
                } catch (EventDispatchTimeoutException ex) {
                    // Timeout while waiting for the EDT to start the task.
                    // Now wrap this in two exceptions: The outermost exception will
                    // be catched by the PromptingKeyProvider class and its cause
                    // will be unwrapped and passed on to the client application by
                    // the PromptingKeyProvider class.
                    throw new KeyPromptingTimeoutException(ex);
                /*} catch (InterruptedException failure) {
                    // We've been interrupted while waiting for the EDT.
                    // Now wrap this in two exceptions: The outermost exception will
                    // be catched by the PromptingKeyProvider class and its cause
                    // will be unwrapped and passed on to the client application by
                    // the PromptingKeyProvider class.
                    throw new KeyPromptingInterruptedException(failure));
                */} catch (InvocationTargetException ex) {
                    throw new UnknownKeyException(ex);
                } finally {
                    Thread.interrupted();
                }
            }
        }
    }
}
