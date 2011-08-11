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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.key.pbe.console.ConsoleAesPbeParametersView;
import de.schlichtherle.truezip.key.pbe.swing.SwingAesPbeParametersView;
import de.schlichtherle.truezip.key.spi.KeyManagerService;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * A container for a prompting key manager implementation for
 * {@link AesPbeParameters}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class PromptingKeyManagerService extends KeyManagerService {

    private final Map<Class<?>, KeyManager<?>> managers;

    /**
     * Constructs a new prompting key manager service using the default view.
     * If this JVM is running {@link GraphicsEnvironment#isHeadless() headless},
     * then the view of the prompting key provider of the prompting key manager
     * is an instance of {@link ConsoleAesPbeParametersView}.
     * Otherwise, it's an instance of {@link SwingAesPbeParametersView}.
     */
    public <K> PromptingKeyManagerService() {
        this.managers = newMap(new Object[][] {
            {
                AesPbeParameters.class,
                new PromptingKeyManager<AesPbeParameters>(
                    GraphicsEnvironment.isHeadless()
                        ? new ConsoleAesPbeParametersView()
                        : new SwingAesPbeParametersView())
            }
        });
    }

    /**
     * Constructs a new prompting key manager service using the given view.
     * 
     * @param view the view for the prompting key providers of the prompting
     *        key manager.
     */
    public PromptingKeyManagerService(View<AesPbeParameters> view) {
        this.managers = newMap(new Object[][] {
            {
                AesPbeParameters.class,
                new PromptingKeyManager<AesPbeParameters>(view)
            }
        });
    }

    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return managers;
    }
}
