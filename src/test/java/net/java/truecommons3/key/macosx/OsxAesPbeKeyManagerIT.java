/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.macosx;

import net.java.truecommons3.key.spec.KeyManager;
import net.java.truecommons3.key.spec.KeyManagerITSuite;
import net.java.truecommons3.key.spec.KeyProvider;
import net.java.truecommons3.key.spec.UnknownKeyException;
import net.java.truecommons3.key.spec.common.AesPbeParameters;
import net.java.truecommons3.key.spec.prompting.KeyPromptingDisabledException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
public class OsxAesPbeKeyManagerIT extends KeyManagerITSuite<AesPbeParameters> {

    @Override
    protected KeyManager<AesPbeParameters> keyManager() {
        final AesPbeParameters parameters = new AesPbeParameters();
        parameters.setPassword("test1234".toCharArray());
        final KeyProvider<AesPbeParameters> provider = mock(KeyProvider.class);
        try {
            when(provider.getKeyForWriting()).thenReturn(parameters);
            when(provider.getKeyForReading(false)).thenReturn(parameters);
            when(provider.getKeyForReading(true)).thenThrow(KeyPromptingDisabledException.class);
        } catch (UnknownKeyException e) {
            throw new AssertionError();
        }
        final KeyManager<AesPbeParameters> manager = mock(KeyManager.class);
        when(manager.provider(uri())).thenReturn(provider);
        return new OsxKeyManager<>(manager, AesPbeParameters.class);
    }
}
