/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.pbe;

import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A user interface to prompt for parameters for safe password based encryption.
 * <p>
 * Sub classes must be thread-safe and should have no side effects!
 * 
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public abstract class SafePbeParametersView<P extends SafePbeParameters<?, P>>
implements View<P> {

    /**
     * Returns new parameters for safe password based encryption.
     * 
     * @return New parameters for safe password based encryption.
     */
    protected abstract P newPbeParameters();
}