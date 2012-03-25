/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has been cancelled.
 * This is normally caused by user input, for example if the user has closed
 * the prompting dialog.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class KeyPromptingCancelledException extends CacheableUnknownKeyException {
    private static final long serialVersionUID = 7645927619378423566L;
    
    public KeyPromptingCancelledException() {
        super("Key prompting has been cancelled!");
    }
}