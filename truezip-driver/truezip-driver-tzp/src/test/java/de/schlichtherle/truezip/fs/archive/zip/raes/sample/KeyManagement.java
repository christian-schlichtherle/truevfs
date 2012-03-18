/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
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
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.archive.zip.raes.PromptingKeyManagerService;
import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.key.PromptingKeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider.Controller;
import de.schlichtherle.truezip.key.UnknownKeyException;
import de.schlichtherle.truezip.key.sl.KeyManagerLocator;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

/**
 * Provides static utility methods to set passwords for RAES encrypted ZIP
 * files programmatically.
 * Use whatever approach fits your needs best when you want to set the password
 * programmatically instead of prompting the user for a key by means of the
 * default Swing or Console based user interfaces.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class KeyManagement {

    /** You cannot instantiate this class. */
    private KeyManagement() {
    }

    static void install(TArchiveDetector detector) {
// START SNIPPET: install
        TFile.setDefaultArchiveDetector(detector);
// END SNIPPET: install
    }

// START SNIPPET: newArchiveDetector1
    /**
     * Returns a new archive detector which uses the given password for all
     * RAES encrypted ZIP files with the given list of suffixes.
     * <p>
     * When used for encryption, the AES key strength will be set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param  delegate the file system driver provider to decorate.
     * @param  suffixes A list of file name suffixes which shall identify
     *         prospective archive files.
     *         This must not be {@code null} and must not be empty.
     * @param  password the password char array to be copied for internal use.
     * @return A new archive detector which uses the given password for all
     *         RAES encrypted ZIP files with the given list of suffixes.
     */
    public static TArchiveDetector newArchiveDetector1(
            FsDriverProvider delegate,
            String suffixes,
            char[] password) {
        return new TArchiveDetector(delegate,
                suffixes, new CustomZipRaesDriver(password));
    }
    
    private static final class CustomZipRaesDriver extends SafeZipRaesDriver {
        final RaesParameters param;
        
        CustomZipRaesDriver(char[] password) {
            super(IOPoolLocator.SINGLETON, KeyManagerLocator.SINGLETON);
            param = new CustomRaesParameters(password);
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
        public FsController<?> newController(
                FsModel model,
                FsController<?> parent) {
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
        final char[] password;
        
        CustomRaesParameters(final char[] password) {
            this.password = password.clone();
        }
        
        @Override
        public char[] getWritePassword()
        throws RaesKeyException {
            return password.clone();
        }
        
        @Override
        public char[] getReadPassword(boolean invalid)
        throws RaesKeyException {
            if (invalid)
                throw new RaesKeyException("Invalid password!");
            return password.clone();
        }
        
        @Override
        public KeyStrength getKeyStrength()
        throws RaesKeyException {
            return KeyStrength.BITS_128;
        }
        
        @Override
        public void setKeyStrength(KeyStrength keyStrength)
        throws RaesKeyException {
            // We have been using only 128 bits to create archive entries.
            assert KeyStrength.BITS_128 == keyStrength;
        }
    } // CustomRaesParameters
// END SNIPPET: newArchiveDetector1

// START SNIPPET: newArchiveDetector2
    /**
     * Returns a new archive detector which uses the given password for all
     * RAES encrypted ZIP files with the given list of suffixes.
     * <p>
     * When used for encryption, the AES key strength will be set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param  delegate the file system driver provider to decorate.
     * @param  suffixes A list of file name suffixes which shall identify
     *         prospective archive files.
     *         This must not be {@code null} and must not be empty.
     * @param  password the password char array to be copied for internal use.
     * @return A new archive detector which uses the given password for all
     *         RAES encrypted ZIP files with the given list of suffixes.
     */
    public static TArchiveDetector newArchiveDetector2(
            FsDriverProvider delegate,
            String suffixes,
            char[] password) {
        return new TArchiveDetector(delegate,
                    suffixes,
                    new SafeZipRaesDriver(
                        IOPoolLocator.SINGLETON,
                        new PromptingKeyManagerService(
                            new CustomView(password))));
    }
    
    private static final class CustomView
    implements PromptingKeyProvider.View<AesCipherParameters> {
        final char[] password;
        
        CustomView(char[] password) {
            this.password = password.clone();
        }
        
        /**
         * You need to create a new key because the key manager may eventually
         * reset it when the archive file gets moved or deleted.
         */
        private AesCipherParameters newKey() {
            AesCipherParameters param = new AesCipherParameters();
            param.setPassword(password);
            param.setKeyStrength(KeyStrength.BITS_128);
            return param;
        }
        
        @Override
        public void promptWriteKey(Controller<AesCipherParameters> controller)
        throws UnknownKeyException {
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            // Note that this would typically return the hierarchical URI of
            // the archive file unless ZipDriver.mountPointUri(FsModel) would
            // have been overridden.
            controller.setKey(newKey());
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
            controller.setKey(newKey());
        }
    } // CustomView
// END SNIPPET: newArchiveDetector2
}
