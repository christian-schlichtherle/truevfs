/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a resource has been closed.
 *
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ClosedException extends IOException {
    private static final long serialVersionUID = 7502497562473974639L;
}
