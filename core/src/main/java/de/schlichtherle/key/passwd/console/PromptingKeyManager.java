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

package de.schlichtherle.key.passwd.console;

import de.schlichtherle.key.*;

/**
 * A simple key manager which enables users to enter passwords as keys using
 * console I/O.
 * This key manager is used by default if the JVM is running in headless mode
 * and the API complies to JSE6 (i.e. the class <code>java.io.Console</code>
 * is available)!
 * To request it explicitly, set the system property
 * <code>de.schlichtherle.key.KeyManager</code> to
 * <code>de.schlichtherle.key.passwd.console.PromptingKeyManager</code>.
 * <p>
 * This key manager does not support key files and disables prompting if no
 * console is attached to the JVM.
 * <p>
 * Note that class loading and instantiation may happen in a JVM shutdown hook,
 * so class initializers and constructors must behave accordingly.
 * In particular, it's not permitted to construct or use a Swing GUI there.
 * <p>
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @since TrueZIP 6.4
 * @version $Id$
 */
public class PromptingKeyManager extends de.schlichtherle.key.PromptingKeyManager {

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

    //
    // Instance stuff:
    //

    protected boolean isPromptingImpl() {
        return super.isPromptingImpl() && System.console() != null;
    }

    /**
     * If no console is attached to this JVM, then this method throws a
     * {@link KeyPromptingDisabledException}.
     */
    protected void ensurePromptingImpl()
    throws KeyPromptingDisabledException {
        if (System.console() == null)
            throw new KeyPromptingDisabledException();
        super.ensurePromptingImpl();
    }
}
