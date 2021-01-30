/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import bali.Module;
import net.java.truevfs.kernel.spec.FsArchiveDriver;
import net.java.truevfs.kernel.spec.FsArchiveEntry;

@Module
interface ControllerModuleFactory {

    ControllerModuleFactory INSTANCE = ControllerModuleFactory$.new$();

    <E extends FsArchiveEntry> ControllerModule<E> module(FsArchiveDriver<E> driver);
}
