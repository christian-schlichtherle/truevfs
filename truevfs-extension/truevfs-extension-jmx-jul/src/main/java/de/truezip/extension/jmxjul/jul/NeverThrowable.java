/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

/**
 * @author  Christian Schlichtherle
 */
public final class NeverThrowable extends Throwable {
    private static final long serialVersionUID = 8475247249857263463L;

    public NeverThrowable() {
        super("Relax, this throwable has just been created for the following stack trace and is never thrown:");
    }
}