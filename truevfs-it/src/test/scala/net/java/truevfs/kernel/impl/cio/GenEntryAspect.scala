/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import net.java.truecommons.cio.Entry

/**
  * @author Christian Schlichtherle
  */
trait GenEntryAspect[E <: Entry] {
  def entry: E
  final def name = entry.getName
}
