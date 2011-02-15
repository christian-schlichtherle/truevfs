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
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.key.spi.KeyManagerProvider;
import java.awt.GraphicsEnvironment;
import java.util.ServiceConfigurationError;
import net.jcip.annotations.Immutable;

/**
 * A container for a prompting key manager implementation for
 * {@link AesCipherParameters}.
 * If this JVM is running {@link GraphicsEnvironment#isHeadless() headless},
 * then its UI is an instance of
 * {@link de.schlichtherle.truezip.crypto.raes.param.console.AesCipherParametersView}.
 * Otherwise, it's an instance of
 * {@link de.schlichtherle.truezip.crypto.raes.param.swing.AesCipherParametersView}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class PromptingKeyManagerProvider extends KeyManagerProvider {

    private static final PromptingKeyManager<AesCipherParameters>
            manager = new PromptingKeyManager<AesCipherParameters>(
                GraphicsEnvironment.isHeadless()
                    ? new de.schlichtherle.truezip.crypto.raes.param.console.AesCipherParametersView()
                    : new de.schlichtherle.truezip.crypto.raes.param.swing.AesCipherParametersView());

    @Override
    @SuppressWarnings("unchecked")
    public <K> KeyManager<? extends K, ?> get(Class<K> type) {
        if (type.isAssignableFrom(AesCipherParameters.class))
            return (KeyManager<? extends K, ?>) manager;
        throw new ServiceConfigurationError("No service available for " + type);
    }
}
