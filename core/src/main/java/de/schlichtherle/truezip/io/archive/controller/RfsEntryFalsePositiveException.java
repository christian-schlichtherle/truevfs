package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.Archive;
import java.io.IOException;

/**
 * Indicates that the target file of an archive controller is a false
 * positive archive file which actually exists as a plain file or directory
 * in the real file system.
 * <p>
 * Instances of this class are always associated with an
 * {@code IOException} as their cause.
 */
public final class RfsEntryFalsePositiveException
extends FalsePositiveException {

    private static final long serialVersionUID = 5234672956837622323L;

    RfsEntryFalsePositiveException(Archive archive, IOException cause) {
        super(archive, cause);
    }
}
