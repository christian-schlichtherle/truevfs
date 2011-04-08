/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.sample.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.file.TDefaultArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.SafeKeyManager;
import de.schlichtherle.truezip.key.SafeKeyProvider;
import de.schlichtherle.truezip.key.sl.KeyManagerLocator;
import de.schlichtherle.truezip.key.spi.KeyManagerService;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Arrays;
import java.util.ServiceConfigurationError;

/**
 * Provides static utility methods to demonstrate various approaches to set
 * the password for one or more RAES encrypted ZIP files programmatically.
 * Use whatever approach fits your needs best when you want to set the password
 * programmatically instead of prompting the user for a key by means of the
 * default Swing or Console based user interfaces.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class KeyManagement {

    /** You cannot instantiate this class. */
    private KeyManagement() {
    }

    /**
     * Sets the password for an individual RAES encrypted ZIP file.
     * This method uses the default {@link KeyManager} for
     * {@link AesCipherParameters} and the default {@link KeyProvider} for the
     * given {@code file} in order to set the given {@code password}.
     * <p>
     * As another side effect, the key strength of the
     * {@link AesCipherParameters} is set to the maximum 256 bits.
     * <p>
     * This method makes a protective copy of the given password char array.
     * It's highly recommended to overwrite this array with any non-password
     * data after calling this method.
     *
     * @param file the TZP archive file to set the password for.
     * @param password the password char array to be copied for subsequent use.
     */
    public static void setPassword(final TFile file, final char[] password) {
        if (!file.isArchive())
            throw new IllegalArgumentException(file + " (not an archive file)");
        if (null == password)
            throw new NullPointerException();
        AesCipherParameters params = new AesCipherParameters();
        params.setPassword(password);
        Arrays.fill(password, (char) 0);
        KeyManagerLocator
                .SINGLETON
                .get(AesCipherParameters.class)
                .getKeyProvider(file.toURI())
                .setKey(params);
    }

    /**
     * Sets the password for all RAES encrypted ZIP files.
     * This method decorates {@link TDefaultArchiveDetector#ALL} with a
     * custom archive driver for the canonical archive file suffixes
     * {@code "tzp|zip.rae|zip.raes"} and installs the result as the
     * default archive detector using {@link TFile#setDefaultArchiveDetector}.
     * <p>
     * As another side effect, the key strength of the
     * {@link AesCipherParameters} is set to the maximum 256 bits.
     * <p>
     * This method makes a protective copy of the given password char array.
     * It's highly recommended to overwrite this array with any non-password
     * data after calling this method.
     *
     * @param password the password char array to be copied for subsequent use.
     */
    public static void setAllPasswords(final char[] password) {
        TFile.setDefaultArchiveDetector(
                new TDefaultArchiveDetector(
                    TDefaultArchiveDetector.ALL,
                    "tzp|zip.rae|zip.raes",
                    new SafeZipRaesDriver(  IOPoolLocator.SINGLETON,
                                            new SimpleKeyManagerService(password))));
    }

    private static final class SimpleKeyManagerService
    extends KeyManagerService {
        private final KeyManager<AesCipherParameters> manager;

        SimpleKeyManagerService(final char[] password) {
            manager = new SafeKeyManager<AesCipherParameters, SimpleKeyProvider>(
                        new SimpleKeyProviderFactory(password));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K> KeyManager<K> get(Class<K> type) {
            if (type.equals(AesCipherParameters.class))
                return (KeyManager<K>) manager;
            throw new ServiceConfigurationError("No key manager available for " + type);
        }
    } // SimpleKeyManagerService

    private static final class SimpleKeyProviderFactory
    implements KeyProvider.Factory<SimpleKeyProvider> {
        private final char[] password;

        SimpleKeyProviderFactory(final char[] password) {
            this.password = password.clone();
        }

        @Override
        public SimpleKeyProvider newKeyProvider() {
            return new SimpleKeyProvider(password);
        }
    } // class SimpleKeyProviderFactory

    private static final class SimpleKeyProvider
    extends SafeKeyProvider<AesCipherParameters> {
        private AesCipherParameters key = new AesCipherParameters();

        SimpleKeyProvider(final char[] password) {
            key.setPassword(password);
        }

        @Override
        protected AesCipherParameters getWriteKeyImpl() {
            return key/*.clone()*/; // cloning is optional here
        }

        @Override
        protected AesCipherParameters getReadKeyImpl(boolean invalid) {
            return key/*.clone()*/; // cloning is optional here
        }

        @Override
        public void setKey(final AesCipherParameters key) {
            this.key = key.clone();
        }
    } // class SimpleKeyProvider
}
