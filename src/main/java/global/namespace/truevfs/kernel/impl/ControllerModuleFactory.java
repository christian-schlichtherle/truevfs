/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import bali.Module;
import global.namespace.truevfs.kernel.api.FsArchiveDriver;
import global.namespace.truevfs.kernel.api.FsArchiveEntry;

@Module
interface ControllerModuleFactory {

    ControllerModuleFactory INSTANCE = ControllerModuleFactory$.new$();

    <E extends FsArchiveEntry> ControllerModule<E> module(FsArchiveDriver<E> driver);
}
