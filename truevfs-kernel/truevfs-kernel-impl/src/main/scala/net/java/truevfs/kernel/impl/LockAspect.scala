/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks._
import javax.annotation.concurrent._

/** A mixin which provides some features of its associated reentrant `lock`.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait LockAspect extends GenLockAspect[Lock]
