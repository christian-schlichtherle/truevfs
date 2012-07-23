/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes.sample;

import java.util.Map;
import javax.inject.Provider;
import net.truevfs.access.TArchiveDetector;
import net.truevfs.access.TConfig;
import net.truevfs.driver.zip.raes.SafeZipRaesDriver;
import net.truevfs.driver.zip.raes.crypto.RaesKeyException;
import net.truevfs.driver.zip.raes.crypto.RaesParameters;
import net.truevfs.driver.zip.raes.crypto.Type0RaesParameters;
import net.truevfs.kernel.spec.FsController;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsModel;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.keymanager.spec.KeyManagerContainer;
import net.truevfs.keymanager.spec.PromptingKeyManagerContainer;
import net.truevfs.keymanager.spec.PromptingKeyProvider.Controller;
import net.truevfs.keymanager.spec.PromptingKeyProvider.View;
import net.truevfs.keymanager.spec.UnknownKeyException;
import net.truevfs.keymanager.spec.param.AesKeyStrength;
import net.truevfs.keymanager.spec.param.AesPbeParameters;

/**
 * Provides static utility methods to set passwords for RAES encrypted ZIP
 * files programmatically.
 * Use whatever approach fits your needs best when you want to set the password
 * programmatically instead of prompting the user for a key by means of the
 * default Swing or Console based user interfaces.
 *
 * @author Christian Schlichtherle
 */
public final class KeyManagement {

    /* Can't touch this - hammer time! */
    private KeyManagement() { }

    static void install(TArchiveDetector detector) {
// START SNIPPET: install
        TConfig.get().setArchiveDetector(detector);
// END SNIPPET: install
    }

// START SNIPPET: newArchiveDetector1
    /**
     * Returns a new archive detector which uses the given password for all
     * RAES encrypted ZIP files with the given list of extensions.
     * <p>
     * When used for encryption, the AES key strength will be set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param  provider the file system driver provider to decorate.
     * @param  extensions A list of file name extensions which shall identify
     *         prospective archive files.
     *         This must not be {@code null} and must not be empty.
     * @param  password the password char array to be copied for internal use.
     * @return A new archive detector which uses the given password for all
     *         RAES encrypted ZIP files with the given list of extensions.
     */
    public static TArchiveDetector newArchiveDetector1(
            Provider<Map<FsScheme, FsDriver>> provider,
            String extensions,
            char[] password) {
        return new TArchiveDetector(provider,
                extensions, new CustomZipRaesDriver1(password));
    }
    
    private static final class CustomZipRaesDriver1 extends SafeZipRaesDriver {
        final RaesParameters param;
        
        CustomZipRaesDriver1(char[] password) {
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
        public <M extends FsModel> FsController<M> decorate(
                FsController<M> controller) {
            // This is a minor improvement: The default implementation decorates
            // the default file system controller chain with a package private
            // file system controller which uses the key manager to keep track
            // of the encryption parameters.
            // Because we are not using the key manager, we don't need this
            // special purpose file system controller and can simply return the
            // given file system controller chain instead.
            return controller;
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
        public AesKeyStrength getKeyStrength()
        throws RaesKeyException {
            return AesKeyStrength.BITS_128;
        }
        
        @Override
        public void setKeyStrength(AesKeyStrength keyStrength)
        throws RaesKeyException {
            // We have been using only 128 bits to create archive entries.
            assert AesKeyStrength.BITS_128 == keyStrength;
        }
    } // CustomRaesParameters
// END SNIPPET: newArchiveDetector1

// START SNIPPET: newArchiveDetector2
    /**
     * Returns a new archive detector which uses the given password for all
     * RAES encrypted ZIP files with the given list of extensions.
     * <p>
     * When used for encryption, the AES key strength will be set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param  provider the file system driver provider to decorate.
     * @param  extensions A list of file name extensions which shall identify
     *         prospective archive files.
     *         This must not be {@code null} and must not be empty.
     * @param  password the password char array to be copied for internal use.
     * @return A new archive detector which uses the given password for all
     *         RAES encrypted ZIP files with the given list of extensions.
     */
    public static TArchiveDetector newArchiveDetector2(
            Provider<Map<FsScheme, FsDriver>> provider,
            String extensions,
            char[] password) {
        return new TArchiveDetector(provider,
                    extensions, new CustomZipRaesDriver2(password));
    }
    
    private static final class CustomZipRaesDriver2 extends SafeZipRaesDriver {
        final KeyManagerContainer provider;
        
        CustomZipRaesDriver2(char[] password) {
            this.provider = new PromptingKeyManagerContainer(
                    AesPbeParameters.class,
                    new CustomView(password));
        }
        
        @Override
        public KeyManagerContainer getKeyManagerProvider() {
            return provider;
        }
    } // CustomZipRaesDriver2
    
    private static final class CustomView
    implements View<AesPbeParameters> {
        final char[] password;
        
        CustomView(char[] password) {
            this.password = password.clone();
        }
        
        /**
         * You need to create a new key because the key manager may eventually
         * reset it when the archive file gets moved or deleted.
         */
        private AesPbeParameters newKey() {
            AesPbeParameters param = new AesPbeParameters();
            param.setPassword(password);
            param.setKeyStrength(AesKeyStrength.BITS_128);
            return param;
        }
        
        @Override
        public void promptWriteKey(Controller<AesPbeParameters> controller)
        throws UnknownKeyException {
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            // Note that this would typically return the hierarchical URI of
            // the archive file unless ZipDriver.mountPointUri(FsModel) would
            // have been overridden.
            controller.setKey(newKey());
        }
        
        @Override
        public void promptReadKey(  Controller<AesPbeParameters> controller,
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