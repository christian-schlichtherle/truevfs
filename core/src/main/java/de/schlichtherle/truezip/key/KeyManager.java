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

import java.net.URI;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.schlichtherle.truezip.util.ClassLoaders.loadClass;

/**
 * An abstract class which maintains a map of {@link KeyProvider} instances for
 * any protected resource which clients need to create or open.
 * This key manager class is designed to be of general purpose:
 * Resources are simply represented by a string as their identifier, called
 * the <i>resource identifier</i> or <i>resource ID</i> for short.
 * For each resource ID, a key provider may be associated to it which handles
 * the actual retrieval of the key.
 * <p>
 * Clients need to call {@link #get} to get the default instance.
 * Because the map of key providers and some associated methods are static
 * members of this class, the default instance of this class may be changed
 * dynamically (using {@link #setInstance}) without affecting already mapped
 * key providers.
 * This allows to change other aspects of the implementation dynamically
 * (the user interface for example) without affecting the key providers and hence the
 * keys.
 * <p>
 * Implementations need to subclass this class and provide a public
 * no-arguments constructor.
 * Finally, an instance of the implementation must be installed either by
 * calling {@link #setInstance(KeyManager)} or by setting the system property
 * {@code de.schlichtherle.truezip.key.KeyManager} to the fully qualified class
 * name of the implementation before this class is ever used.
 * In the latter case, the class will be loaded using the context class loader
 * of the current thread.
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
public abstract class KeyManager {

    private static volatile KeyManager instance;

    /** Maps resource IDs to providers. */
    private static final Map<URI, KeyProvider<?>> providers
            = new HashMap<URI, KeyProvider<?>>();

    /**
     * Maps key provider types (should be interfaces) to key provider types
     * (their implementing classes).
     */
    private final Map<Class<? extends KeyProvider<?>>, Class<? extends KeyProvider<?>>>
            types = new HashMap<Class<? extends KeyProvider<?>>, Class<? extends KeyProvider<?>>>();
    //private final Map types = new HashMap(); // look at the beauty of this instead!

    //
    // Static Methods.
    //

    /**
     * Returns the default instance of the key manager.
     * <p>
     * If the default instance has been explicitly set using
     * {@link #setInstance}, then this instance is returned.
     * <p>
     * Otherwise, the value of the system property
     * {@code de.schlichtherle.truezip.key.KeyManager} is considered:
     * <p>
     * If this system property is set, it must denote the fully qualified
     * class name of a subclass of this class. The class is loaded and
     * instantiated using its public, no-arguments constructor.
     * <p>
     * Otherwise, if the JVM is running in headless mode and the API conforms
     * to JSE 6 (where the class {@code java.io.Console} is available),
     * then the console I/O based implementation in the class
     * {@link de.schlichtherle.truezip.key.passwd.console.PromptingKeyManager}
     * is loaded and instantiated using its public, no-arguments constructor.
     * <p>
     * Otherwise, the Swing based implementation in the class
     * {@link de.schlichtherle.truezip.key.passwd.swing.PromptingKeyManager}
     * is loaded and instantiated using its public, no-arguments constructor.
     * <p>
     * In order to support this plug-in architecture, you should <em>not</em>
     * cache the instance returned by this method!
     *
     * @throws ClassCastException If the class name in the system property
     *         does not denote a subclass of this class.
     * @throws UndeclaredThrowableException If any other precondition on the
     *         value of the system property does not hold.
     */
    public static synchronized KeyManager get() {
        if (instance != null)
            return instance;

        final String n = System.getProperty(KeyManager.class.getName(),
                getDefaultKeyManagerClassName());
        try {
            Class<?> c = loadClass(n, KeyManager.class);
            instance = (KeyManager) c.newInstance();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UndeclaredThrowableException(ex);
        }

        return instance;
    }

    private static String getDefaultKeyManagerClassName() {
        if (GraphicsEnvironment.isHeadless()) {
            try {
                Class.forName("java.io.Console");
                return de.schlichtherle.truezip.key.passwd.console.PromptingKeyManager.class.getName();
            } catch (ClassNotFoundException noJSE6ConsoleAvailable) {
                // Ignore and fall through - prompting will be disabled.
            }
        }
        return de.schlichtherle.truezip.key.passwd.swing.PromptingKeyManager.class.getName();
    }

    /**
     * Sets the default instance of the key manager explicitly.
     *
     * @param keyManager The key manager to use as the default instance.
     *        If this is set to {@code null}, on the next call to
     *        {@link #get} a new instance will be created.
     */
    public static void setInstance(final KeyManager keyManager) {
        KeyManager.instance = keyManager;
    }

    /**
     * Maps the given key provider for the given resource identifier.
     *
     * @param  resource the non-{@code null} protected resource.
     * @param  provider the nullable key provider.
     *         Use {@code null} to unmap a key provider for the given protected
     *         resource.
     * @return The key provider previously mapped for the given resource
     *         or {@code null} if no key provider was mapped.
     * @throws NullPointerException if {@code resource} is {@code null}.
     */
    static synchronized KeyProvider<?> mapKeyProvider(
            final URI resource,
            final KeyProvider<?> provider) {
        if (resource == null)
            throw new NullPointerException();
        return provider != null
                ? providers.put(resource, provider)
                : providers.remove(resource);
    }

    /**
     * Resets the key provider for the given resource identifier, causing it
     * to forget its common key.
     * This works only if the key provider associated with the given resource
     * identifier is an instance of {@link AbstractKeyProvider}.
     * Otherwise, nothing happens.
     *
     * @param resource The resource identifier.
     * @return Whether or not an instance of {@link AbstractKeyProvider}
     *         is mapped for the resource identifier and has been reset.
     */
    public static synchronized boolean resetKeyProvider(final URI resource) {
        final KeyProvider<?> provider = providers.get(resource);
        if (provider instanceof AbstractKeyProvider<?>) {
            final AbstractKeyProvider<?> akp = (AbstractKeyProvider<?>) provider;
            akp.reset();
            return true;
        }
        return false;
    }

    /**
     * Resets the key provider for the given resource identifier, causing it
     * to forget its common key, and throws the key provider away.
     * If the key provider associated with the given resource identifier is
     * not an instance of {@link AbstractKeyProvider}, it is just removed from
     * the map.
     *
     * @param resource The resource identifier.
     * @return Whether or not a key provider was mapped for the resource
     *         identifier and has been removed.
     */
    public static synchronized boolean resetAndRemoveKeyProvider(
            final URI resource) {
        final KeyProvider<?> provider = providers.get(resource);
        if (provider instanceof AbstractKeyProvider<?>) {
            final AbstractKeyProvider<?> akp = (AbstractKeyProvider<?>) provider;
            akp.reset();
            final KeyProvider<?> result = akp.removeFromKeyManager(resource);
            assert provider == result;
            return true;
        } else if (provider != null) {
            final KeyProvider<?> previous = mapKeyProvider(resource, null);
            assert provider == previous;
            return true;
        }
        return false;
    }

    /**
     * Resets all key providers, causing them to forget their respective
     * common key.
     * If a mapped key provider is not an instance of {@link AbstractKeyProvider},
     * nothing happens.
     */
    public static void resetKeyProviders() {
        forEachKeyProvider(new KeyProviderCommand() {
            @Override
			public void run(URI resource, KeyProvider<?> provider) {
                if (provider instanceof AbstractKeyProvider) {
                    ((AbstractKeyProvider<?>) provider).reset();
                }
            }
        });
    }

    /**
     * Resets all key providers, causing them to forget their key, and removes
     * them from the map.
     * If the key provider associated with the given resource identifier is
     * not an instance of {@link AbstractKeyProvider}, it is just removed from
     * the map.
     *
     * @throws IllegalStateException If resetting or unmapping one or more
     *         key providers is prohibited by a constraint in a subclass of
     *         {@link AbstractKeyProvider}, in which case the respective key
     *         provider(s) are reset but remain mapped.
     *         The operation is continued normally for all other key providers.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    public static synchronized void resetAndRemoveKeyProviders() {
        class ResetAndRemoveKeyProvider implements KeyProviderCommand {
            IllegalStateException ise = null;

            @Override
			public void run(URI resource, KeyProvider<?> provider) {
                if (provider instanceof AbstractKeyProvider<?>) {
                    final AbstractKeyProvider<?> akp
                            = (AbstractKeyProvider<?>) provider;
                    akp.reset();
                    try {
                        akp.removeFromKeyManager(resource); // support proper clean up!
                    } catch (IllegalStateException exc) {
                        ise = exc; // mark and forget any previous exception
                    }
                } else {
                    final KeyProvider<?> previous = mapKeyProvider(resource, null);
                    assert provider == previous;
                }
            }
        }

        final ResetAndRemoveKeyProvider cmd = new ResetAndRemoveKeyProvider();
        forEachKeyProvider(cmd);
        if (cmd.ise != null)
            throw cmd.ise;
    }

    /**
     * Executes a {@link KeyProviderCommand} for each mapped key provider.
     * It is safe to call any method of this class within the command,
     * even if it modifies the map of key providers.
     */
    protected static synchronized void forEachKeyProvider(
            final KeyProviderCommand command) {
        // We can't use an iterator because the command may modify the map.
        // Otherwise, resetAndClearKeyProviders() would fail with a
        // ConcurrentModificationException.
        final Set<Map.Entry<URI, KeyProvider<?>>> entrySet = providers.entrySet();
        final int n = entrySet.size();
        @SuppressWarnings("unchecked")
		final Map.Entry<URI, KeyProvider<?>>[] entries
                = entrySet.toArray(new Map.Entry[n]);
        for (int i = 0; i < n; i++) {
            final Map.Entry<URI, KeyProvider<?>> entry = entries[i];
            final URI resource = entry.getKey();
            final KeyProvider<?> provider = entry.getValue();
            command.run(resource, provider);
        }
    }

    /**
     * Implemented by sub classes to define commands which shall be executed
     * on key providers with the {@link #forEachKeyProvider} method.
     */
    protected interface KeyProviderCommand {
        void run(URI resource, KeyProvider<?> provider);
    }

    /**
     * Moves a key provider from one resource identifier to another.
     * This may be useful if a protected resource changes its identifier.
     * For example, if the protected resource is a file, the most obvious
     * identifier would be its canonical path name.
     * Calling this method then allows you to rename a file without the need
     * to retrieve its keys again, thereby possibly prompting (and confusing)
     * the user.
     *
     * @return {@code true} if and only if the operation succeeded,
     *         which means that there was an instance of
     *         {@link KeyProvider} associated with
     *         {@code oldResourceID}.
     * @throws NullPointerException If {@code oldResourceID} or
     *         {@code newResourceID} is {@code null}.
     * @throws IllegalStateException If unmapping or mapping the key provider
     *         is prohibited by a constraint in a subclass of
     *         {@link AbstractKeyProvider}, in which case the transaction is
     *         rolled back before this exception is (re)thrown.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    public static synchronized boolean moveKeyProvider(
            final URI oldResource,
            final URI newResource)
    throws NullPointerException, IllegalStateException {
        if (oldResource == null || newResource == null)
            throw new NullPointerException();
        final KeyProvider<?> provider = providers.get(oldResource);
        if (provider == null)
            return false;
        if (provider instanceof AbstractKeyProvider<?>) {
            final AbstractKeyProvider<?> akp = (AbstractKeyProvider<?>) provider;
            // Implement transactional behaviour.
            akp.removeFromKeyManager(oldResource);
            try {
                akp.addToKeyManager(newResource);
            } catch (RuntimeException failure) {
                akp.addToKeyManager(oldResource);
                throw failure;
            }
        } else {
            mapKeyProvider(oldResource, null);
            mapKeyProvider(newResource, provider);
        }
        return true;
    }

    //
    // Instance methods:
    //

    /**
     * Creates a new {@code KeyManager}.
     * This class does <em>not</em> map any key provider types.
     * This must be done in the subclass using {@link #mapKeyProviderType}.
     */
    protected KeyManager() {
    }

    /**
     * Subclasses must use this method to register default implementation
     * classes for the interfaces {@link KeyProvider} and {@link AesKeyProvider}
     * and optionally other subinterfaces or subclasses of
     * {@code KeyProvider}.
     * This is best done in the constructor of the subclass
     * (this method is final).
     *
     * @param forType The type which shall be substituted with
     *        {@code useType} when determining a suitable
     *        run time type in {@link #getKeyProvider(URI, Class)}.
     * @param useType The type which shall be substituted for
     *        {@code forType} when determining a suitable
     *        run time type in {@link #getKeyProvider(URI, Class)}.
     * @throws NullPointerException If any of the parameters is
     *         [@code null}.
     * @throws IllegalArgumentException If {@code useType} is the same as
     *         {@code forType}, or if {@code useType} does not
     *         provide a public constructor with no parameters.
     */
    protected final synchronized <K extends Cloneable, P extends KeyProvider<K>>
    void mapKeyProviderType(
            final Class<P> forType,
            final Class<? extends P> useType) {
        if (useType == forType)
            throw new IllegalArgumentException(
                    useType.getName()
                    + " must be a subclass or implementation of "
                    + forType.getName() + "!");
        try {
            useType.getConstructor((Class<?>[]) null);
        } catch (NoSuchMethodException noPublicNullaryConstructor) {
            throw new IllegalArgumentException(useType.getName()
            + " (no public nullary constructor)",
                    noPublicNullaryConstructor);
        }
        types.put(forType, useType);
    }

    /**
     * Returns the {@link KeyProvider} for the given protected resource.
     * If no key provider is mapped, this key manager will determine an
     * appropriate class which is assignment compatible to
     * {@code type} (but is not necessarily the same),
     * instantiate it, map the instance for the protected resource and return
     * it.
     * <p>
     * Client applications should specify an interface rather than an
     * implementation as the {@code type} parameter in order to allow
     * the key manager to instantiate a useful default implementation of this
     * interface unless another key provider was already mapped for the
     * protected resource.
     * <p>
     * <b>Example:</b>
     * The following example asks the default key manager to provide a
     * suitable implementation of the {@link AesKeyProvider} interface
     * for a protected resource.
     * <pre>
     * URI resource = file.getCanonicalFile().toURI();
     * KeyManager km = KeyManager.get();
     * KeyProvider kp = km.getKeyProvider(resource, AesKeyProvider.class);
     * Object key = kp.getCreateKey(); // may prompt the user
     * int ks;
     * if (kp instanceof AesKeyProvider) {
     *      // The run time type of the implementing class is determined
     *      // by the key manager.
     *      // Anyway, the AES key provider can be safely asked for a cipher
     *      // key strength.
     *      ks = ((AesKeyProvider) kp).getKeyStrength();
     * } else {
     *      // Unfortunately, another key provider was already mapped for the
     *      // pathname before - use default key strength.
     *      ks = AesKeyProvider.KEY_STRENGTH_256;
     * }
     * </pre>.
     *
     * @param  resource the URI for the protected resource.
     * @param  type unless another key provider is already mapped
     *         for the protected resource, this denotes the root of the class
     *         hierarchy to which the run time type of the returned instance
     *         may belong.
     *         In case the key manager does not know a more suitable class in
     *         this hierarchy, this is supposed to denote an implementation of
     *         the {@link KeyProvider} interface with a public no-argument
     *         constructor.
     * @return The {@link KeyProvider} mapped for the protected resource.
     *         If no key provider has been previously mapped for the protected
     *         resource, the run time type of this instance is guaranteed to be
     *         assignment compatible to the given {@code type}.
     * @throws NullPointerException if {@code resourceID} or
     *         {@code type} is {@code null}.
     * @throws ClassCastException if no other key provider is mapped for the
     *         protected resource and the given class is not an implementation
     *         of the {@code KeyProvider} interface.
     * @throws IllegalArgumentException if any other precondition on the
     *         parameter {@code type} does not hold.
     * @see    #get
     */
    public synchronized KeyProvider<?> getKeyProvider(
            final URI resource,
            Class<? extends KeyProvider<?>> type)
    throws NullPointerException, ClassCastException, IllegalArgumentException {
        if (resource == null)
            throw new NullPointerException();
        synchronized (KeyManager.class) {
            KeyProvider<?> kp = providers.get(resource);
            if (kp == null) {
                final Class<? extends KeyProvider<?>> subst = types.get(type);
                if (subst != null)
                    type = subst;
                try {
                    kp = type.newInstance();
                } catch (InstantiationException ex) {
                    throw new IllegalArgumentException(type.getName(), ex);
                } catch (IllegalAccessException ex) {
                    throw new IllegalArgumentException(type.getName(), ex);
                }
                setKeyProvider(resource, kp);
            }
            return kp;
        }
    }

    /**
     * Sets the key provider programmatically.
     * <p>
     * <b>Warning</b>: This method replaces any key provider previously
     * associated with the given resource and installs it as the return
     * value for {@link #getKeyProvider}.
     * While this allows a reasonable level of flexibility, it may easily
     * confuse users if they have already been prompted for a key by the
     * previous provider before and may negatively affect the security if the
     * provider is not properly guarded by the application.
     * Use with caution only!
     *
     * @param resource The resource identifier to associate the key
     *        provider with.
     *        For an RAES encrypted ZIP file, this must be the canonical
     *        path name of the archive file.
     * @param provider The key provider for {@code resourceID}.
     *        For an RAES encrypted ZIP file, this must be an instance of
     *        the {@link AesKeyProvider} interface.
     * @throws NullPointerException If {@code resourceID} or
     *         {@code provider} is {@code null}.
     * @throws IllegalStateException If mapping this instance is prohibited
     *         by a constraint in a subclass of {@link AbstractKeyProvider}.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    public void setKeyProvider(
            final URI resource,
            final KeyProvider<?> provider)
    throws NullPointerException, IllegalStateException {
        if (provider == null)
            throw new NullPointerException();

        if (provider instanceof AbstractKeyProvider<?>) {
            ((AbstractKeyProvider<?>) provider).addToKeyManager(resource);
        } else {
            mapKeyProvider(resource, provider);
        }
    }
}
