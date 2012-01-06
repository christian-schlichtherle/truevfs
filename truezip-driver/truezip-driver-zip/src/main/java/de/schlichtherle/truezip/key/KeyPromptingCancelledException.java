/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key;

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has been cancelled.
 * This is normally caused by user input, for example if the user has closed
 * the prompting dialog.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class KeyPromptingCancelledException extends CacheableUnknownKeyException {
    private static final long serialVersionUID = 7645927619378423566L;
    
    public KeyPromptingCancelledException() {
        super("Key prompting has been cancelled!");
    }
}
