/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * @version $Id$
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
