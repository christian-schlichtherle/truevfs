/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.mock;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.cio.Entry.Access;
import global.namespace.truevfs.comp.cio.Entry.Type;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.api.*;
import global.namespace.truevfs.kernel.api.io.ThrowingInputStream;
import global.namespace.truevfs.kernel.api.io.ThrowingOutputStream;
import global.namespace.truevfs.kernel.api.io.ThrowingSeekableChannel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MockController extends FsAbstractController {

    private final Optional<? extends FsController> parent;

    private final ConcurrentMap<FsNodeName, IoEntry<?>> map = new ConcurrentHashMap<>();

    private final FsTestConfig config;

    private volatile Optional<FsThrowManager> control = Optional.empty();

    public MockController(FsModel model, Optional<? extends FsController> parent) {
        this(model, parent, Optional.empty());
    }

    /**
     * Constructs a new mock controller.
     *
     * @param model  The file system model.
     * @param parent The parent file system controller.
     * @param config The mocking configuration.
     */
    public MockController(final FsModel model,
                          final Optional<? extends FsController> parent,
                          final Optional<FsTestConfig> config) {
        super(model);
        assert model.getParent().equals(parent.map(FsController::getModel));
        this.parent = parent;
        this.config = config.orElseGet(FsTestConfig::get);
    }

    private FsThrowManager getThrowControl() {
        final Optional<FsThrowManager> control = this.control;
        return control.orElseGet(() -> (this.control = Optional.of(config.getThrowControl())).get());
    }

    private void checkAllExceptions(final Object thiz) throws IOException {
        getThrowControl().check(thiz, IOException.class);
        checkUndeclaredExceptions(this);
    }

    private void checkUndeclaredExceptions(final Object thiz) {
        getThrowControl().check(thiz, RuntimeException.class);
        getThrowControl().check(thiz, Error.class);
    }

    @Override
    public Optional<? extends FsController> getParent() {
        checkUndeclaredExceptions(this);
        return parent;
    }

    @Override
    public Optional<? extends FsNode> node(
            final BitField<FsAccessOption> options,
            final FsNodeName name)
            throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final BitField<Access> types)
            throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != options;
        assert null != types;
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly(
            final BitField<FsAccessOption> options,
            final FsNodeName name)
            throws IOException {
        checkAllExceptions(this);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final Map<Access, Long> times)
            throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != times;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final BitField<Access> types, long value)
            throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != types;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public InputSocket<?> input(
            final BitField<FsAccessOption> options,
            final FsNodeName name) {
        checkUndeclaredExceptions(this);
        assert null != name;
        assert null != options;
        return new InputSocket<Entry>() {

            InputSocket<? extends Entry> getSocket() throws IOException {
                checkAllExceptions(this);
                final IoEntry<?> buffer = map.get(name);
                if (null == buffer) {
                    throw new NoSuchFileException(name.toString());
                }
                return buffer.input();
            }

            @Override
            public Entry getTarget() throws IOException {
                return getSocket().getTarget();
            }

            @Override
            public InputStream stream(
                    Optional<? extends OutputSocket<? extends Entry>> peer
            ) throws IOException {
                return new ThrowingInputStream(getSocket().stream(peer), config.getThrowControl());
            }

            @Override
            public SeekableByteChannel channel(
                    Optional<? extends OutputSocket<? extends Entry>> peer
            ) throws IOException {
                return new ThrowingSeekableChannel(getSocket().channel(peer), config.getThrowControl());
            }
        };
    }

    @Override
    public OutputSocket<?> output(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final Optional<? extends Entry> template) {
        checkUndeclaredExceptions(this);
        assert null != name;
        assert null != options;
        return new OutputSocket<Entry>() {

            OutputSocket<? extends Entry> getSocket() throws IOException {
                checkAllExceptions(this);
                final IoEntry<?> n = new MemoryBuffer(name.toString(), config.getDataSize());
                IoEntry<?> o = map.get(name);
                if (null == o) {
                    o = map.putIfAbsent(name, n);
                }
                return (null != o ? o : n).output();
            }

            @Override
            public Entry getTarget() throws IOException {
                return getSocket().getTarget();
            }

            @Override
            public OutputStream stream(
                    Optional<? extends InputSocket<? extends Entry>> peer
            ) throws IOException {
                return new ThrowingOutputStream(getSocket().stream(peer), config.getThrowControl());
            }

            @Override
            public SeekableByteChannel channel(
                    Optional<? extends InputSocket<? extends Entry>> peer
            ) throws IOException {
                return new ThrowingSeekableChannel(getSocket().channel(peer), config.getThrowControl());
            }
        };
    }

    @Override
    public void make(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final Type type,
            final Optional<? extends Entry> template)
            throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != type;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlink(
            final BitField<FsAccessOption> options,
            final FsNodeName name)
            throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public void sync(final BitField<FsSyncOption> options) throws FsSyncException {
        getThrowControl().check(this, FsSyncException.class);
        checkUndeclaredExceptions(this);
        assert null != options;
    }
}
