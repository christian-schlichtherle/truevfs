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

import de.schlichtherle.truezip.key.KeyPromptingDisabledException;
import de.schlichtherle.truezip.key.PromptingAesKeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;

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
 */
public class PromptingKeyManager
        extends de.schlichtherle.truezip.key.PromptingKeyManager {

    /**
     * Constructs a new {@code PromptingKeyManager}.
     * This instance maps the following key provider UI types using
     * {@link de.schlichtherle.truezip.key.PromptingKeyManager#mapPromptingKeyProviderUIType}:
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public PromptingKeyManager() {
        mapPromptingKeyProviderUIType(
        		(Class) PromptingKeyProvider.class,
        		(Class) PromptingKeyProviderUI.class);
        mapPromptingKeyProviderUIType(
        		(Class) PromptingAesKeyProvider.class,
        		(Class) PromptingAesKeyProviderUI.class);
    }

    @Override
    protected boolean isPromptingImpl() {
        return super.isPromptingImpl() && !GraphicsEnvironment.isHeadless();
    }

    /**
     * If this JVM is running in headless mode, then this method throws
     * a {@link KeyPromptingDisabledException} with a {@link HeadlessException}
     * as its cause.
     */
    @Override
    protected void assertPromptingImpl()
    throws KeyPromptingDisabledException {
        if (GraphicsEnvironment.isHeadless())
            throw new KeyPromptingDisabledException(new HeadlessException());
        super.assertPromptingImpl();
    }
}
