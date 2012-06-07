/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import javax.annotation.concurrent._
import net.truevfs.kernel.cio._

/** An output socket which obtains its delegate socket lazily and `reset`s
  * it upon any [[java.lang.Throwable]].
  *
  * @see    ClutchInputSocket
  * @tparam E the type of the `localTarget`.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private abstract class ClutchOutputSocket[E <: Entry]
extends DelegatingOutputSocket[E] with ClutchSocketLike[E, OutputSocket[_ <: E]] {
  override def localTarget() = apply(_.localTarget())
  override def stream() = apply(_.stream())
  override def channel() = apply(_.channel())
}
