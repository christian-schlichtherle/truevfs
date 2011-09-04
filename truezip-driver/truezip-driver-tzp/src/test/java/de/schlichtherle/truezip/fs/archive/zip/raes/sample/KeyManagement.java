/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes.sample;

import de.schlichtherle.truezip.crypto.raes.RaesKeyException;
import de.schlichtherle.truezip.crypto.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.archive.zip.raes.PromptingKeyManagerService;
import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider.Controller;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import de.schlichtherle.truezip.key.UnknownKeyException;
import de.schlichtherle.truezip.key.sl.KeyManagerLocator;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Provides static utility methods to set passwords for RAES encrypted ZIP
 * files programmatically.
 * Use whatever approach fits your needs best when you want to set the password
 * programmatically instead of prompting the user for a key by means of the
 * default Swing or Console based user interfaces.
 *
 * @deprecated This sample code should <em>not</em> be considered part of the
 *             public API.
 *             If you want to use it, then copy it, don't call it!
 * @author     Christian Schlichtherle
 * @version    $Id$
 */
@Deprecated
@DefaultAnnotation(NonNull.class)
public final class KeyManagement {

    /** You cannot instantiate this class. */
    private KeyManagement() {
    }

// START SNIPPET: setPassword
    /**
     * Sets the password for an individual RAES encrypted ZIP file a priori.
     * <p>
     * This method uses the default {@link KeyManager} for
     * {@link AesCipherParameters} and the default {@link KeyProvider} for the
     * given {@code file} in order to set the given {@code password}.
     * <p>
     * <p>
     * As another side effect, the AES key strength is set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param file the TZP archive file to set the password for.
     * @param password the password char array to be copied for internal use.
     */
    public static void setPassword(TFile file, char[] password) {
        if (!file.isArchive())
            throw new IllegalArgumentException(file + " (not an archive file)");
        AesCipherParameters param = new AesCipherParameters();
        param.setPassword(password);
        param.setKeyStrength(KeyStrength.BITS_128);
        KeyManagerLocator
                .SINGLETON
                .get(AesCipherParameters.class)
                .getKeyProvider(file.toFsPath().toHierarchicalUri()) // http://java.net/jira/browse/TRUEZIP-72
                .setKey(param);
    }
// END SNIPPET: setPassword

// START SNIPPET: setAllPasswords1
    /**
     * Sets a common password for all RAES encrypted ZIP files ad hoc.
     * <p>
     * This method installs a custom {@link TArchiveDetector} for the canonical
     * archive file suffixes {@code "tzp|zip.rae|zip.raes"} as the
     * {@link TFile#setDefaultArchiveDetector default archive detector}.
     * This side effect will apply only to subsequently created {@link TFile}
     * objects.
     * The custom archive driver uses a custom {@link View} implementation as
     * its {@link PromptingKeyManagerService}.
     * <p>
     * As another side effect, the AES key strength is set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param password the password char array to be copied for internal use.
     */
    public static void setAllPasswords1(char[] password) {
        TFile.setDefaultArchiveDetector(
                new TArchiveDetector(
                    TFile.getDefaultArchiveDetector(),
                    "tzp|zip.rae|zip.raes",
                    new CustomZipRaesDriver()));
    }
    
    private static final class CustomZipRaesDriver extends SafeZipRaesDriver {
        static final RaesParameters param = new CustomRaesParameters();
        
        public CustomZipRaesDriver() {
            super(IOPoolLocator.SINGLETON, KeyManagerLocator.SINGLETON);
        }
        
        @Override
        protected RaesParameters raesParameters(FsModel model) {
            // If you need the URI of the particular archive file, then call
            // model.getMountPoint().toUri().
            // If you need a more user friendly form of this URI, then call
            // model.getMountPoint().toHierarchicalUri().

            // Let's not use the key manager but instead our custom parameters.
            return param;
        }
        
        @Override
        public FsController<?> newController(FsModel model, FsController<?> parent) {
            // This is a minor improvement: The default implementation decorates
            // the default file system controller chain with a package private
            // file system controller which keeps track of the encryption keys.
            // Because we are not using the key manager, we don't need this
            // special purpose file system controller and can use the default
            // file system controller chain instead.
            return superNewController(model, parent);
        }
    } // CustomZipRaesDriver
    
    private static final class CustomRaesParameters
    implements Type0RaesParameters {
        
        public char[] getWritePassword() throws RaesKeyException {
            return "secret".toCharArray();
        }
        
        public char[] getReadPassword(boolean invalid) throws RaesKeyException {
            if (invalid)
                throw new RaesKeyException("Invalid password!");
            return "secret".toCharArray();
        }
        
        public KeyStrength getKeyStrength() throws RaesKeyException {
            return KeyStrength.BITS_128;
        }
        
        public void setKeyStrength(KeyStrength keyStrength) throws RaesKeyException {
            // We have been using only 128 bits to create archive entries.
            assert KeyStrength.BITS_128 == keyStrength;
        }
    } // CustomRaesParameters
// END SNIPPET: setAllPasswords1

// START SNIPPET: setAllPasswords2
    /**
     * Sets a common password for all RAES encrypted ZIP files ad hoc.
     * <p>
     * This method installs a custom {@link TArchiveDetector} for the canonical
     * archive file suffixes {@code "tzp|zip.rae|zip.raes"} as the
     * {@link TFile#setDefaultArchiveDetector default archive detector}.
     * This side effect will apply only to subsequently created {@link TFile}
     * objects.
     * The custom archive driver uses a custom {@link View} implementation as
     * its {@link PromptingKeyManagerService}.
     * <p>
     * As another side effect, the key strength of all
     * {@link AesCipherParameters} is set to the maximum 256 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param password the password char array to be copied for internal use.
     */
    public static void setAllPasswords2(char[] password) {
        TFile.setDefaultArchiveDetector(
                new TArchiveDetector(
                    TFile.getDefaultArchiveDetector(),
                    "tzp|zip.rae|zip.raes",
                    new SafeZipRaesDriver(
                        IOPoolLocator.SINGLETON,
                        new PromptingKeyManagerService(
                            new SimpleView(password)))));
    }
    
    private static final class SimpleView
    implements PromptingKeyProvider.View<AesCipherParameters> {
        private AesCipherParameters params = new AesCipherParameters();
        
        SimpleView(char[] password) {
            if (null == password)
                throw new NullPointerException();
            params.setPassword(password);
        }
        
        @Override
        public void promptWriteKey(Controller<AesCipherParameters> controller)
        throws UnknownKeyException {
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            // Note that this would typically return the hierarchical URI of
            // the archive file unless ZipDriver.mountPointUri(FsModel) would
            // have been overridden.
            controller.setKey(params);
        }
        
        @Override
        public void promptReadKey(  Controller<AesCipherParameters> controller,
                                    boolean invalid)
        throws UnknownKeyException {
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            // Note that this would typically return the hierarchical URI of
            // the archive file unless ZipDriver.mountPointUri(FsModel) would
            // have been overridden.
            controller.setKey(params);
        }
    } // SimpleView
// END SNIPPET: setAllPasswords2
}
