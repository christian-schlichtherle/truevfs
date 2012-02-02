/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.console;

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A console based user interface to prompt for passwords.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class ConsoleAesPbeParametersView
extends ConsoleSafePbeParametersView<AesKeyStrength, AesPbeParameters> {
    @Override
    public AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }
}
