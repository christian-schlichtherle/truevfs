/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import net.truevfs.kernel.util.PathSplitter

class Splitter(separatorChar: Char)
extends PathSplitter(separatorChar, false) {
  override def getParentPath = {
    val path = super.getParentPath
    if (null ne path) path else ""
  }

  def apply(path: String) = {
    split(path)
    (getParentPath, getMemberName)
  }
} // Splitter
