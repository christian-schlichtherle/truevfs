/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import de.schlichtherle.truezip.key.spi.KeyManagerService;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * A container for a prompting key manager implementation for
 * {@link AesCipherParameters}.
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
     * is an instance of
     * {@link de.schlichtherle.truezip.crypto.raes.param.console.AesCipherParametersView}.
     * Otherwise, it's an instance of
     * {@link de.schlichtherle.truezip.crypto.raes.param.swing.AesCipherParametersView}.
     */
    public <K> PromptingKeyManagerService() {
        this.managers = newMap(new Object[][] {
            {
                AesCipherParameters.class,
                new PromptingKeyManager<AesCipherParameters>(
                    GraphicsEnvironment.isHeadless()
                        ? new de.schlichtherle.truezip.crypto.raes.param.console.AesCipherParametersView()
                        : new de.schlichtherle.truezip.crypto.raes.param.swing.AesCipherParametersView())
            }
        });
    }

    /**
     * Constructs a new prompting key manager service using the given view.
     * 
     * @param view the view for the prompting key providers of the prompting
     *        key manager.
     */
    public PromptingKeyManagerService(View<AesCipherParameters> view) {
        this.managers = newMap(new Object[][] {
            {
                AesCipherParameters.class,
                new PromptingKeyManager<AesCipherParameters>(view)
            }
        });
    }

    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return managers;
    }
}
