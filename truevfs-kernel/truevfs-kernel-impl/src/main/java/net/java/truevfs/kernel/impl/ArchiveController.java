/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.cio.Entry.Type;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Provides read/write access to an archive file system.
 * This is a mirror of {@link net.java.truevfs.kernel.spec.FsController} which has been customized to ease the
 * implementation.
 *
 * @author Christian Schlichtherle
 */
interface ArchiveController<E extends FsArchiveEntry> extends ArchiveModelAspect<E>, ReentrantReadWriteLockAspect {

    Optional<? extends FsNode> node(BitField<FsAccessOption> options, FsNodeName name) throws IOException;

    void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types) throws IOException;

    void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException;

    boolean setTime(BitField<FsAccessOption> options, FsNodeName name, Map<Access, Long> times) throws IOException;

    boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types, long time) throws IOException;

    InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name);

    OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<Entry> template);

    void make(BitField<FsAccessOption> options, FsNodeName name, Type type, Optional<Entry> template) throws IOException;

    void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException;

    void sync(BitField<FsSyncOption> options) throws FsSyncException;
}
