/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.pacemaker;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.api.*;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Calls a template method to apply an aspect to all file system operations except {@link #sync(BitField)}.
 *
 * @see #apply
 * @author Christian Schlichtherle
 */
abstract class AspectController extends FsDecoratingController {

    AspectController(FsController controller) {
        super(controller);
    }

    /**
     * Applies the aspect to the given file system operation.
     *
     * @param  op the file system operation to apply an aspect to.
     * @return The return value of the file system operation.
     */
    abstract <T> T apply(Op<T> op) throws IOException;

    interface Op<T> extends Callable<T> {

        T call() throws IOException;
    }

    @CheckForNull
    @Override
    public final Optional<? extends FsNode> node(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        return apply(() -> controller.node(options, name));
    }

    @Override
    public final void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types) throws IOException {
        apply(() -> {
            controller.checkAccess(options, name, types);
            return null;
        });
    }

    @Override
    public final void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        apply(() -> {
            controller.setReadOnly(options, name);
            return null;
        });
    }

    @Override
    public final boolean setTime(BitField<FsAccessOption> options, FsNodeName name, Map<Entry.Access, Long> times) throws IOException {
        return apply(() -> controller.setTime(options, name, times));
    }

    @Override
    public final boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types, long value) throws IOException {
        return apply(() -> controller.setTime(options, name, types, value));
    }

    @Override
    public final InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return new Input(controller.input(options, name));
    }

    private final class Input extends AbstractInputSocket<Entry> {

        private final InputSocket<? extends Entry> socket;

        Input(final InputSocket<? extends Entry> socket) {
            this.socket = socket;
        }

        @Override
        public Entry target() throws IOException {
            return apply(socket::target);
        }

        @Override
        public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
            return apply(() -> socket.stream(peer));
        }

        @Override
        public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
            return apply(() -> socket.channel(peer));
        }
    }

    @Override
    public final OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<? extends Entry> template) {
        return new Output(controller.output(options, name, template));
    }

    private final class Output extends AbstractOutputSocket<Entry> {

        private final OutputSocket<? extends Entry> socket;

        Output(final OutputSocket<? extends Entry> socket) {
            this.socket = socket;
        }

        @Override
        public Entry target() throws IOException {
            return apply(socket::target);
        }

        @Override
        public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
            return apply(() -> socket.stream(peer));
        }

        @Override
        public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
            return apply(() -> socket.channel(peer));
        }
    }

    @Override
    public final void make(BitField<FsAccessOption> options, FsNodeName name, Entry.Type type, Optional<? extends Entry> template) throws IOException {
        apply(() -> {
            controller.make(options, name, type, template);
            return null;
        });
    }

    @Override
    public final void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        apply(() -> {
            controller.unlink(options, name);
            return null;
        });
    }
}
