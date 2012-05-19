/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.key.param;

import net.truevfs.key.PromptingKeyProvider.View;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A user interface to prompt for parameters for safe password based encryption.
 * <p>
 * Sub classes must be thread-safe and should have no side effects!
 * 
 * @param  <P> the type of the safe PBE parameters.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class SafePbeParametersView<P extends SafePbeParameters<P, ?>>
implements View<P> {

    /**
     * Returns new parameters for safe password based encryption.
     * 
     * @return New parameters for safe password based encryption.
     */
    protected abstract P newPbeParameters();
}