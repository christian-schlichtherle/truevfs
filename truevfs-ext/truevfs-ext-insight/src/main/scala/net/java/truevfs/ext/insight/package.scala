/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext

/**
  * Instruments the TrueVFS Kernel for statistics monitoring via JMX.
  *
  * @author Christian Schlichtherle
  */
package object insight {

  private[insight] object applicationIoMediator extends I5tIoMediator("Application I/O")
  private[insight] object bufferIoMediator extends I5tIoMediator("Buffer I/O")
  private[insight] object kernelIoMediator extends I5tIoMediator("Kernel I/O")
  private[insight] object syncOperationsMediator extends I5tSyncMediator("Sync Operations")

  private[insight] def mediators = Array(
    syncOperationsMediator, applicationIoMediator, kernelIoMediator,
    bufferIoMediator
  )
}
