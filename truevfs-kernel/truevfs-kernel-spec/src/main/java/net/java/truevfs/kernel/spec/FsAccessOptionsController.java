/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import java.util.Map;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.cio.Entry.Type;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;

/**
 * Maps the access options before delegating the called operation.
 * The intended use case of this class is for convenient setting or clearing
 * of some file system specific access options with a custom file system driver.
 * <p>
 * For example, let's assume you would like to use the JAR file format with all
 * entries encrypted according to the WinZip AES specification as your custom
 * application file format.
 * Then you could subclass this class as follows:
 * <pre>{@code
class EncryptionController extends FsAccessOptionsController {

    EncryptionController(FsController controller) { super(controller); }

    \@Override
    protected BitField<FsAccessOption> map(BitField<FsAccessOption> options) {
        return options.set(FsAccessOption.ENCRYPT);
    }
}
 * }</pre>
 * <p>
 * Next, you would have to subclass the {@code JarDriver} in order to inject
 * an instance of the {@code EncryptionController} as follows:
 * <pre>{@code
class EncryptionJarDriver extends JarDriver {
    \@Override
    public FsController decorate(FsController controller) {
        return new EncryptionController(super.decorate(controller));
    }
}
 * }</pre>
 * <p>
 * Note the call to {@code super.decorate()} - this is required to let the
 * super class install the built-in key manager.
 * <p>
 * Now, whenever you use this driver it would encrypt all entries written to
 * your custom application file format according to the WinZip AES
 * specification.
 * <p>
 * Mind that this is just sample code - for encryption, you should generally
 * prefer the TrueZIP Driver ZIP.RAES for its increased security level!
 *
 * @since  TrueVFS 0.9.2
 * @author Christian Schlichtherle
 */
public abstract class FsAccessOptionsController extends FsDecoratingController {

    protected FsAccessOptionsController(FsController controller) {
        super(controller);
    }

    /**
     * Maps the given access options.
     *
     * @param  options the access options to map.
     * @return The mapped access options.
     */
    protected abstract BitField<FsAccessOption>
    map(BitField<FsAccessOption> options);

    @Override
    public final FsNode node(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        return controller.node(map(options), name);
    }

    @Override
    public final void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types) throws IOException {
        controller.checkAccess(map(options), name, types);
    }

    @Override
    public final void setReadOnly(BitField<FsAccessOption> options, FsNodeName name)
    throws IOException {
        controller.setReadOnly(map(options), name);
    }

    @Override
    public final boolean setTime(BitField<FsAccessOption> options, FsNodeName name, Map<Access, Long> times) throws IOException {
        return controller.setTime(map(options), name, times);
    }

    @Override
    public final boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types, long value) throws IOException {
        return controller.setTime(map(options), name, types, value);
    }

    @Override
    public final InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return controller.input(map(options), name);
    }

    @Override
    public final OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Entry template) {
        return controller.output(map(options), name, template);
    }

    @Override
    public final void make(BitField<FsAccessOption> options, FsNodeName name, Type type, Entry template) throws IOException {
        controller.make(map(options), name, type, template);
    }

    @Override
    public final void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        controller.unlink(map(options), name);
    }
}
