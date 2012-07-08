/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl.cio

import net.truevfs.kernel.spec.cio.Entry

/**
  * @author Christian Schlichtherle
  */
trait GenEntryAspect[E <: Entry] {
  def entry: E
  final def name = entry.getName
}