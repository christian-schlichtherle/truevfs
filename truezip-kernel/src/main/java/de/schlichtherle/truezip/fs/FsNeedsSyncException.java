/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.BitField;
import net.jcip.annotations.Immutable;

/**
 * Indicates that the file system needs to get
 * {@linkplain FsController#sync(BitField) synced} before the operation can
 * proceed.
 *
 * @since   TrueZIP 7.3
 * @see     FsSyncController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public final class FsNeedsSyncException extends FsControllerException {
    public FsNeedsSyncException() {
        super(null);
    }
}
