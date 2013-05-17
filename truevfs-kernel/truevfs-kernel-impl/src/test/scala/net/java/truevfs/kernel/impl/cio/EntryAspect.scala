/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import language.implicitConversions
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._

/**
  * @author Christian Schlichtherle
  */
trait EntryAspect[E <: Entry]
extends GenEntryAspect[E] with EntryLike {
  type IndexedProperty[-A, B] = A => B

  final def size = entry.getSize(_)
  final def time = entry.getTime(_)
  final def permission(tµpe: Access) = { entity =>
    val p = entry.isPermitted(tµpe, entity)
    if (null ne p) Option(p) else None
  }
}

/**
  * @author Christian Schlichtherle
  */
object EntryAspect {
  implicit def apply[E <: Entry](e: E) = new EntryAspect[E] { def entry = e }
}
