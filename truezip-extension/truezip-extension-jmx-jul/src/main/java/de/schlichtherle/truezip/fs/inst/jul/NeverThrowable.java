/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jul;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class NeverThrowable extends Throwable {
    private static final long serialVersionUID = 8475247249857263463L;

    public NeverThrowable() {
        super("Relax, this throwable has just been created for the following stack trace and is never thrown:");
    }
}
