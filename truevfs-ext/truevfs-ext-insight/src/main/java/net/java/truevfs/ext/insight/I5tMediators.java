package net.java.truevfs.ext.insight;

import java.util.List;

/**
 * @author Christian Schlichtherle
 */
interface I5tMediators {

    I5tIoMediator applicationIoMediator = new I5tIoMediator("Application I/O");

    I5tIoMediator bufferIoMediator = new I5tIoMediator("Buffer I/O");

    I5tIoMediator kernelIoMediator = new I5tIoMediator("Kernel I/O");

    I5tSyncMediator syncOperationsMediator = new I5tSyncMediator("Sync Operations");

    List<I5tMediator> mediators = List.of(
            syncOperationsMediator, applicationIoMediator, kernelIoMediator, bufferIoMediator
    );
}
