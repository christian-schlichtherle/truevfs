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

import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.UnknownKeyException;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simulates the typical life cycle of a protected resource and its
 * associated key using the API in the package {@link de.schlichtherle.truezip.key}.
 * <p>
 * Note that this Runnable simulates the regular operation of a client
 * application of the key manager package.
 * It does <em>not</em> throw any {@code AssertionError}s.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class KeyMgmtLifeCycle implements Runnable {

    private static final Logger LOGGER
            = Logger.getLogger(KeyMgmtLifeCycle.class.getName());

    /** The identifier of the protected resource. */
    public final URI id;

    /** The reference key for the resource. */
    private Object refKey;

    /**
     * Contains non-{@code null} if and only if {@code run()} has
     * terminated because the user cancelled the key prompting.
     */
    public Throwable throwable;

    /**
     * @param id The identifier of the protected resource.
     */
    public KeyMgmtLifeCycle(final URI id) {
        this.id = id;
    }

    @Override
	public void run() {
        try {
            runIt();
        }  catch (Throwable t) {
            throwable = t;
        }
    }

    private void runIt() throws UnknownKeyException {
        // Prompt for a key to create the resource.
        createResource();

        // Forget the key stored in the key manager.
        KeyManager.resetKeyProvider(id);

        // Prompt for the key again to open the resource.
        openResource();

        // Overwrite the resource, eventually prompting for a new
        // key if the user has requested so during prompting for the
        // key in openResource().
        createResource();

        // Use last entered key to open the resource yet again.
        openResource();
    }

    private void createResource() throws UnknownKeyException {
        final KeyManager manager = KeyManager.getInstance();
        final KeyProvider<?> provider = getKeyProvider(manager, id);

        // Store the key, so we can later check the key stored in the
        // key manager when opening the resource.
        refKey = provider.getCreateKey();
        String msg = id + ": getCreateKey() returned " + toString(refKey) + ".";
        LOGGER.fine(msg);

        createResourceHook(provider);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected KeyProvider<?> getKeyProvider(KeyManager manager, URI id) {
        return manager.getKeyProvider(id, (Class) KeyProvider.class);
    }

    private void openResource() throws UnknownKeyException {
        final KeyManager manager = KeyManager.getInstance();
        final KeyProvider<?> provider = getKeyProvider(manager, id);

        boolean correct = false;
        while (!correct) {
            correct = authenticateKey(provider);
        }

        openResourceHook(provider);
    }

    private boolean authenticateKey(final KeyProvider<?> provider) throws UnknownKeyException {
        final Object providedKey = provider.getOpenKey();
        final boolean correct = equals(refKey, providedKey);
        String msg = id + ": getOpenKey() returned " + toString(providedKey) + ". ";
        // Nullifying the clone returned by getOpenKey() must not
        // affect the key manager.
        nullify(providedKey);
        if (correct) {
            msg += "That's correct.";
            LOGGER.fine(msg);
        }  else {
            provider.invalidOpenKey();
            msg += "That's wrong!";
            LOGGER.fine(msg);
            try {
                Thread.sleep(3000);
            }  catch (InterruptedException ex) {
                Logger.getLogger(KeyMgmtLifeCycle.class.getName()).log(Level.WARNING, "Current thread was interrupted while waiting!", ex);
            }
        }

        return correct;
    }

    private static boolean equals(final Object o1, final Object o2) {
        if (o1.getClass() != o2.getClass())
            return false;

        if (o1 instanceof char[])
            return Arrays.equals((char[]) o1, (char[]) o2);
        else            if (o1 instanceof byte[])
                return Arrays.equals((byte[]) o1, (byte[]) o2);
            else
                return o1.equals(o2);
    }

    private static String toString(final Object o) {
        if (o instanceof char[])
            return "\"" + new String((char[]) o) + "\".toCharArray()";
        else            if (o instanceof byte[])
                return "\"" + new String((byte[]) o) + "\".getBytes(\"UTF16BE\")";
            else
                return "\"" + o.toString() + "\".toString()";
    }

    private static void nullify(final Object o) {
        if (o instanceof char[])
            Arrays.fill((char[]) o, (char) 0);
        else            if (o instanceof byte[])
                Arrays.fill((byte[]) o, (byte) 0);
    }

    protected void createResourceHook(KeyProvider<?> provider) {
    }

    protected void openResourceHook(KeyProvider<?> provider) {
    }
}