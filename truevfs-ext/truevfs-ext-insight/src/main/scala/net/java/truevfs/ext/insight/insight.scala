/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext

/** Instruments the TrueVFS Kernel for statistics monitoring via JMX.
  *
  * @author Christian Schlichtherle
  */
package object insight {

  private[insight] val applicationIoMediator = new I5tIoMediator("Application I/O")
  private[insight] val bufferIoMediator = new I5tIoMediator("Buffer I/O")
  private[insight] val kernelIoMediator = new I5tIoMediator("Kernel I/O")
  private[insight] val syncOperationsMediator = new I5tSyncMediator("Sync Operations")

  private[insight] def mediators = Array(
    syncOperationsMediator, applicationIoMediator, kernelIoMediator, bufferIoMediator
  )
}
