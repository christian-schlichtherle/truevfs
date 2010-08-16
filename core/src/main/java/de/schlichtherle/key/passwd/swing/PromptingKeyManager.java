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

import de.schlichtherle.key.*;

import java.awt.*;
import java.lang.ref.*;

import javax.swing.*;

/**
 * A key manager which enables users to enter passwords or select key files
 * as keys using a Swing GUI.
 * This key manager is used by default unless the JVM is running in headless
 * mode!
 * <p>
 * If a password is entered, then the run time type of the key is a char array,
 * holding each password character.
 * If a key file is selected, the file size must be 512 bytes or more, of
 * which only the first 512 bytes are used as a byte array.
 * <p>
 * If this JVM is run in headless mode, all prompting is disabled.
 * <p>
 * Note that class loading and instantiation may happen in a JVM shutdown hook,
 * so class initializers and constructors must behave accordingly.
 * In particular, it's not permitted to construct or use a Swing GUI there.
 * <p>
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class PromptingKeyManager
        extends de.schlichtherle.key.PromptingKeyManager {

    /**
     * Constructs a new <code>PromptingKeyManager</code>.
     * This instance maps the following key provider UI types using
     * {@link de.schlichtherle.key.PromptingKeyManager#mapPromptingKeyProviderUIType}:
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>uiClassID</th>
     *   <th>uiClass</th>
     * </tr>
     * <tr>
     *   <td>"PromptingKeyProvider"</td>
     *   <td>PromptingKeyProviderUI.class</td>
     * </tr>
     * <tr>
     *   <td>"PromptingAesKeyProvider"</td>
     *   <td>PromptingAesKeyProviderUI.class</td>
     * </tr>
     * </table>
     */
    public PromptingKeyManager() {
        mapPromptingKeyProviderUIType(
                "PromptingKeyProvider",
                PromptingKeyProviderUI.class);
        mapPromptingKeyProviderUIType(
                "PromptingAesKeyProvider",
                PromptingAesKeyProviderUI.class);
    }

    /**
     * Returns a window which is suitable to be the parent of the dialog
     * used to prompt the user for a key.
     * If no parent window has been set explicitly, then the last focused
     * window is used.
     * If no window received the focus yet or is already made eligible for
     * finalization, then any showing window is used.
     * <p>
     * In all cases, the first showing parent window which is found by
     * searching the containment hierarchy upwards is preferrably returned.
     * <p>
     * As a last resort, if no window is showing, then {@link JOptionPane}'s
     * root frame is used.
     *
     * @see #setParentWindow(Window)
     */
    public static Window getParentWindow() {
        return WindowUtils.getParentWindow();
    }

    /**
     * Sets the parent window of the dialog used for prompting the user for
     * a key.
     * The window is stored in a weak reference in order to allow it to get
     * garbage collected if no thread is holding a strong reference to it
     * from a root object.
     *
     * @param w The parent window to use for key prompting or
     *        <code>null</code> if a default window shall be used.
     * @see #getParentWindow()
     * @deprecated You shouldn't use this method any more, but rely on the
     *             implementation in <code>getParentWindow()</code>.
     */
    public static void setParentWindow(final Window w) {
        WindowUtils.setParentWindow(w);
    }

    //
    // Instance stuff:
    //

    protected boolean isPromptingImpl() {
        return super.isPromptingImpl() && !GraphicsEnvironment.isHeadless();
    }

    /**
     * If this JVM is running in headless mode, then this method throws
     * a {@link KeyPromptingDisabledException} with a {@link HeadlessException}
     * as its cause.
     */
    protected void ensurePromptingImpl()
    throws KeyPromptingDisabledException {
        if (GraphicsEnvironment.isHeadless())
            throw new KeyPromptingDisabledException(new HeadlessException());
        super.ensurePromptingImpl();
    }
}
