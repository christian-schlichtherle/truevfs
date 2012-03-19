/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that an input resource (input stream etc.) has been closed.
 *
 * @see     OutputClosedException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
// TODO: Remove this class and just use its super class.
@ThreadSafe
public class InputClosedException extends ClosedException {
    private static final long serialVersionUID = 4563928734723923649L;
}
