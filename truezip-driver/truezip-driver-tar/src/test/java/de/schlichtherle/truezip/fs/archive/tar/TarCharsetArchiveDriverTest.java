/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriverTestBase;
import de.schlichtherle.truezip.socket.IOPoolProvider;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TarCharsetArchiveDriverTest
extends FsCharsetArchiveDriverTestBase {

    @Override
    protected TarDriver newArchiveDriver(IOPoolProvider provider) {
        return new TarDriver(provider);
    }
}
