/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriverTestSuite;
import de.schlichtherle.truezip.socket.IOPoolProvider;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TarDriverTest
extends FsCharsetArchiveDriverTestSuite<TTarArchiveEntry, TarDriver> {

    @Override
    protected TarDriver newArchiveDriver(IOPoolProvider provider) {
        return new TarDriver(provider);
    }

    @Override
    protected String getUnencodableName() {
        return "\u0080";
    }
}
