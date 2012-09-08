/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truevfs.kernel.spec._

/**
  * @author Christian Schlichtherle
  */
trait GenModel extends FsModel {

  /** Composes the node path from the mountpoint of this model and the given
    * node name.
    * 
    * @param name the node name.
    */
  def path(name: FsNodeName) = new FsNodePath(getMountPoint, name)
}
