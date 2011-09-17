/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Indicates an exceptional condition when synchronizing the changes in a
 * federated file system to its parent file system.
 * An exception of this class implies that no or only insignificant parts
 * of the data of the federated file system has been lost.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class FsSyncWarningException extends FsSyncException {
    private static final long serialVersionUID = 2302357394858347366L;

    public FsSyncWarningException(FsModel model, IOException cause) {
        super(model, cause, -1);
    }
}
