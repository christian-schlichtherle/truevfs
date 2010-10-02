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

package de.schlichtherle.truezip.key;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract {@link KeyManager} which prompts the user for a key if required.
 * <p>
 * This class maintains a map of user interface classes for the
 * {@code PromptingKeyProvider} class and each of its subclasses which
 * require an individual user interface.
 * The particular user interface classes are determined by a subclass of this
 * key manager. This enables the subclass to determine which user interface
 * technology should actually be used to prompt the user for a key.
 * For example, the implementation in the class
 * {@link de.schlichtherle.truezip.key.passwd.swing.PromptingKeyManager} uses Swing
 * to prompt the user for either a password or a key file.
 * <p>
 * Subclasses must use the method {@link #mapPromptingKeyProviderUIType} to
 * register a user interface class for a particular user interface class
 * identifier (the value returned by {@link PromptingKeyProvider#getUITypeKey}).
 * This is best done in the constructor of the subclass.
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
public class PromptingKeyManager extends KeyManager {

    private static volatile boolean prompting = true;

    /**
     * The user interface classes or instances.
     * Values may be instances of {@link PromptingKeyProviderUI} or
     * {@link Class}.
     */
    private final Map<Class<? extends PromptingKeyProvider>, Object> uis
            = new HashMap<Class<? extends PromptingKeyProvider>, Object>();

    /**
     * Constructs a new {@code PromptingKeyManager}.
     * This instance maps the following key provider types using
     * {@link KeyManager#mapKeyProviderType}:
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>forType</th>
     *   <th>useType</th>
     * </tr>
     * <tr>
     *   <td>KeyProvider.class</td>
     *   <td>PromptingKeyProvider.class</td>
     * </tr>
     * <tr>
     *   <td>AesKeyProvider.class</td>
     *   <td>PromptingAesKeyProvider.class</td>
     * </tr>
     * </table>
     */
    public PromptingKeyManager() {
        mapKeyProviderType(KeyProvider.class, PromptingKeyProvider.class);
        mapKeyProviderType(AesKeyProvider.class, PromptingAesKeyProvider.class);
    }

    //
    // Static methods:
    //

    /**
     * Returns {@code true} if and only if prompting mode is enabled.
     * This is a class property.
     * <p>
     * Note that subclasses might add additional behaviour to both
     * {@link #isPrompting} and {@link #setPrompting} through the default
     * key manager instance (see {@link #getInstance}).
     * Regardless, an application may safely assume that
     * {@code isPrompting()} reflects the actual behaviour of the API
     * in this package although it may not reflect the parameter value of
     * the last call to {@code setPrompting(boolean)}.
     *
     * @return Whether or not the user will be prompted for a key if required.
     *
     * @see #setPrompting
     */
    public static boolean isPrompting() {
        KeyManager manager = getInstance();
        return manager instanceof PromptingKeyManager
                && ((PromptingKeyManager) manager).isPromptingImpl();
    }

    /**
     * Called by {@link #isPrompting} on the default key manager instance in
     * order to implement its behaviour and allow subclasses to override it.
     * Subclasses should call the implementation in this class when
     * overriding this method.
     *
     * @see #setPromptingImpl
     * @see #getInstance
     */
    protected boolean isPromptingImpl() {
        return prompting;
    }

    /**
     * Enables or disables prompting mode.
     * If prompting mode is enabled, the user will be prompted for a key when
     * a {@link PromptingKeyProvider} is first requested to provide a key
     * for the respective resource.
     * If prompting mode is disabled, all attempts to prompt the user will
     * result in an {@link UnknownKeyException} until prompting mode is
     * enabled again.
     * <p>
     * This is a class property.
     * <p>
     * Note that subclasses might add additional behaviour to both
     * {@link #isPrompting} and {@link #setPrompting} through the default
     * key manager instance (see {@link #getInstance}).
     * Regardless, an application may safely assume that
     * {@code isPrompting()} reflects the actual behaviour of the API
     * in this package although it may not reflect the parameter value of
     * the last call to {@code setPrompting(boolean)}.
     *
     * @param prompting The value of the property {@code prompting}.
     *
     * @see #isPrompting
     */
    public static void setPrompting(boolean prompting) {
        KeyManager manager = getInstance();
        if (manager instanceof PromptingKeyManager)
                ((PromptingKeyManager) manager).setPromptingImpl(prompting);
    }

    /**
     * Called by {@link #isPrompting} on the default key manager instance in
     * order to implement its behaviour and allow subclasses to override it.
     * Subclasses should call the implementation in this class when
     * overriding this method.
     *
     * @see #isPromptingImpl
     * @see #getInstance
     */
    protected void setPromptingImpl(boolean prompting) {
        PromptingKeyManager.prompting = prompting;
    }

    static void ensurePrompting()
    throws KeyPromptingDisabledException {
        KeyManager manager = getInstance();
        if (manager instanceof PromptingKeyManager)
                ((PromptingKeyManager) manager).ensurePromptingImpl();
    }

    /**
     * Called by some methods in the {@link PromptingKeyProvider} class in
     * order to ensure that prompting mode is enabled.
     * This method may be overridden by subclasses in order to throw a more
     * detailed exception.
     * <p>
     * The implementation in this class is equivalent to:
     * <pre>
        if (!isPromptingImpl())
            throw new KeyPromptingDisabledException();
     * </pre>
     */
    protected void ensurePromptingImpl()
    throws KeyPromptingDisabledException {
        if (!isPromptingImpl())
            throw new KeyPromptingDisabledException();
    }

    /**
     * Resets all cancelled key prompts, forcing a new prompt on the next
     * call to {@link PromptingKeyProvider#getOpenKey()} or
     * {@link PromptingKeyProvider#getCreateKey()}.
     * Of course, this call only affects instances of
     * {@link PromptingKeyProvider}.
     */
    public static void resetCancelledPrompts() {
        forEachKeyProvider(new KeyProviderCommand() {
            public void run(URI resource, KeyProvider<?> provider) {
                if (provider instanceof PromptingKeyProvider)
                    ((PromptingKeyProvider<?>) provider).resetCancelledPrompt();
            }
        });
    }

    //
    // Instance stuff:
    //

    /**
     * Subclasses must use this method to register a user interface class
     * for a particular user interface class identifier as returned by
     * {@link PromptingKeyProvider#getUITypeKey}.
     * This is best done in the constructor of the subclass
     * (this method is final).
     *
     * @param  forType The key type of the user interface class.
     * @param  useType The value type of the user interface class.
     *         This class must have a nullary constructor.
     * @see    #getKeyProvider(URI, Class)
     * @throws NullPointerException If any of the parameters is
     *         {@code null}.
     * @throws IllegalArgumentException If {@code uiClass} does not
     *         provide a public constructor with no parameters.
     */
    protected final synchronized
    void mapPromptingKeyProviderUIType(
            final Class<? extends PromptingKeyProvider> forType,
            final Class<? extends PromptingKeyProviderUI> useType) {
        if (forType == null)
            throw new NullPointerException();
        try {
            useType.getConstructor((Class[]) null);
        } catch (NoSuchMethodException noPublicNullaryConstructor) {
            throw new IllegalArgumentException(useType.getName()
            + " (no public nullary constructor)",
                    noPublicNullaryConstructor);
        }
        uis.put(forType, useType);
    }

    /**
     * Behaves like the super class implementation, but adds additional
     * behaviour in case the resulting key provider is an instance of
     * {@link PromptingKeyProvider}.
     * In this case, the appropriate user interface instance is determined
     * and installed in the key provider before it is returned.
     *
     * @see KeyManager#getKeyProvider(URI, Class)
     */
    @Override
    public KeyProvider<?> getKeyProvider(
            final URI resource,
            final Class<? extends KeyProvider> type)
    throws NullPointerException, ClassCastException, IllegalArgumentException {
        final KeyProvider<?> kp = super.getKeyProvider(resource, type);
        if (kp instanceof PromptingKeyProvider) {
            final PromptingKeyProvider<?> pkp = (PromptingKeyProvider<?>) kp;
            pkp.setUI((PromptingKeyProviderUI) getUI(pkp.getUITypeKey())); // FIXME: This is cheating!
        }
        return kp;
    }

    private synchronized
    PromptingKeyProviderUI<?, ? super PromptingKeyProvider<?>> getUI(
            final Class<? extends PromptingKeyProvider> forType) {
        final Object value = uis.get(forType);
        final PromptingKeyProviderUI<?, ? super PromptingKeyProvider<?>> ui;
        if (value instanceof Class) {
            try {
                ui = (PromptingKeyProviderUI) ((Class) value).newInstance();
            } catch (InstantiationException failure) {
                throw new UndeclaredThrowableException(failure);
            } catch (IllegalAccessException failure) {
                throw new UndeclaredThrowableException(failure);
            }
            uis.put(forType, ui);
        } else if (value != null) {
            ui = (PromptingKeyProviderUI) value;
        } else { // value == null
            throw new IllegalArgumentException(forType +
                    " (unknown user interface for PromptingKeyProvider)");
        }
        return ui;
    }
}
