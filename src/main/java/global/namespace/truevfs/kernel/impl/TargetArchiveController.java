/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import bali.Cache;
import bali.Lookup;
import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.io.ClosedInputException;
import global.namespace.truevfs.comp.io.ClosedOutputException;
import global.namespace.truevfs.comp.util.BitField;
import global.namespace.truevfs.comp.util.ControlFlowException;
import global.namespace.truevfs.comp.util.Operation;
import global.namespace.truevfs.kernel.api.*;
import lombok.val;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import static bali.CachingStrategy.NOT_THREAD_SAFE;
import static global.namespace.truevfs.comp.cio.Entry.ALL_SIZES;
import static global.namespace.truevfs.comp.cio.Entry.Access.READ;
import static global.namespace.truevfs.comp.cio.Entry.Access.WRITE;
import static global.namespace.truevfs.comp.cio.Entry.Size.DATA;
import static global.namespace.truevfs.comp.cio.Entry.Type.DIRECTORY;
import static global.namespace.truevfs.comp.cio.Entry.Type.SPECIAL;
import static global.namespace.truevfs.comp.cio.Entry.UNKNOWN;
import static global.namespace.truevfs.kernel.api.FsAccessOption.CACHE;
import static global.namespace.truevfs.kernel.api.FsAccessOption.GROW;
import static global.namespace.truevfs.kernel.api.FsAccessOptions.ACCESS_PREFERENCES_MASK;
import static global.namespace.truevfs.kernel.api.FsSyncOption.ABORT_CHANGES;

/**
 * Manages I/O to the entry which represents the target archive file in its parent file system, detects archive entry
 * collisions and implements a sync of the target archive file.
 * <p>
 * This controller is an emitter of {@link global.namespace.truevfs.comp.util.ControlFlowException}s, for example when
 * {@linkplain global.namespace.truevfs.kernel.impl.FalsePositiveArchiveException detecting a false positive archive file}, or
 * {@linkplain global.namespace.truevfs.kernel.impl.NeedsSyncException requiring a sync}.
 *
 * @param <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
abstract class TargetArchiveController<E extends FsArchiveEntry> extends FileSystemArchiveController<E> {

    private static final BitField<FsAccessOption> MOUNT_OPTIONS = BitField.of(CACHE);
    private static final BitField<Entry.Access> WRITE_ACCESS = BitField.of(WRITE);

    /**
     * The (possibly cached) {@link InputArchive} which is used to mount the (virtual) archive file system and read the
     * entries from the target archive file.
     */
    private Optional<InputArchive<E>> _inputArchive = Optional.empty();

    /**
     * The (possibly cached) {@link OutputArchive} which is used to write the entries to the target archive file.
     */
    private Optional<OutputArchive<E>> _outputArchive = Optional.empty();

    private boolean invariants() {
        assert getModel().getParent().equals(Optional.of(getParent().getModel()));
        val fs = getFileSystem();
        assert !_inputArchive.isPresent() || fs.isPresent();
        assert !_outputArchive.isPresent() || fs.isPresent();
        assert !fs.isPresent() || _inputArchive.isPresent() || _outputArchive.isPresent();
        return true;
    }

    @Lookup(param = "driver")
    @Override
    public abstract FsArchiveDriver<E> getDriver();

    @Lookup(param = "model")
    abstract FsModel getUnderlyingModel();

    @Cache(NOT_THREAD_SAFE)
    @Override
    public ArchiveModel<E> getModel() {
        return new TargetArchiveModel(getDriver(), getUnderlyingModel());
    }

    /**
     * The entry name of the target archive file in the parent file system.
     */
    @Cache(NOT_THREAD_SAFE)
    FsNodeName getName() {
        val path = getMountPoint().getPath();
        assert path.isPresent();
        return path.get().getNodeName();
    }

    private Optional<InputArchive<E>> getInputArchive() {
        if (_inputArchive.isPresent() && !_inputArchive.get().isOpen()) {
            throw NeedsSyncException.apply();
        }
        return _inputArchive;
    }

    private void setInputArchive(final Optional<InputArchive<E>> ia) {
        assert !ia.isPresent() || !_inputArchive.isPresent();
        ia.ifPresent(a -> setMounted(true));
        _inputArchive = ia;
    }

    private Optional<OutputArchive<E>> getOutputArchive() {
        if (_outputArchive.isPresent() && !_outputArchive.get().isOpen()) {
            throw NeedsSyncException.apply();
        }
        return _outputArchive;
    }

    private void setOutputArchive(final Optional<OutputArchive<E>> oa) {
        assert !oa.isPresent() || !_outputArchive.isPresent();
        oa.ifPresent(a -> setMounted(true));
        _outputArchive = oa;
    }

    @Override
    void mount(final BitField<FsAccessOption> options, final boolean autoCreate) throws IOException {
        try {
            mount0(options, autoCreate);
        } finally {
            assert invariants();
        }
    }

    private void mount0(final BitField<FsAccessOption> options, final boolean autoCreate) throws IOException {
        // HC SVNT DRACONES!

        // Check parent file system node.
        final Optional<? extends FsNode> opn;
        try {
            opn = getParent().node(options, getName());
        } catch (FalsePositiveArchiveException e) {
            throw new AssertionError(e);
        } catch (IOException e) {
            if (autoCreate) {
                throw e;
            }
            throw new FalsePositiveArchiveException(e);
        }

        // Obtain file system by creating or loading it from the parent node.
        final ArchiveFileSystem<E> fs;
        if (opn.isPresent()) {
            val pn = opn.get();

            // ro must be init first because the parent filesystem controller could be a
            // global.namespace.truevfs.driver.file.FileController and then on Windoze this property changes to `TRUE` once the
            // file is opened for reading!
            // FIXME: Produce a new exception on each call!
            val ro = checkReadOnly().map(e -> (Supplier<IOException>) () -> e);
            final InputContainer<E> is;
            try {
                is = getDriver().newInput(getModel(), MOUNT_OPTIONS, getParent(), getName());
            } catch (FalsePositiveArchiveException e) {
                throw new AssertionError(e);
            } catch (IOException e) {
                if (pn.isType(SPECIAL)) {
                    throw new FalsePositiveArchiveException(e);
                } else {
                    throw new PersistentFalsePositiveArchiveException(e);
                }
            }
            fs = ArchiveFileSystem.create(getModel(), is, pn, ro);
            setInputArchive(Optional.of(new InputArchive<>(is)));
            assert isMounted();
        } else {
            if (autoCreate) {
                // This may fail e.g. if the container file is a RAES encrypted ZIP file and the user cancels password
                // prompting:
                outputArchive(options);
                fs = ArchiveFileSystem.create(getModel());
            } else {
                throw new FalsePositiveArchiveException(new NoSuchFileException(getName().toString()));
            }
        }
        setFileSystem(Optional.of(fs));
    }

    private Optional<IOException> checkReadOnly() {
        try {
            getParent().checkAccess(MOUNT_OPTIONS, getName(), WRITE_ACCESS);
            return Optional.empty();
        } catch (FalsePositiveArchiveException e) {
            throw new AssertionError(e);
        } catch (IOException e) {
            return Optional.of(e);
        }
    }

    /**
     * Ensures that `outputArchive` is not empty.
     *
     * @return The output archive.
     */
    private OutputArchive<E> outputArchive(final BitField<FsAccessOption> options) throws IOException {
        if (getOutputArchive().isPresent()) {
            assert isMounted();
            return getOutputArchive().get();
        }
        val is = getInputArchive().map(InputArchive::getDriverProduct).orElse(null);
        final OutputContainer<E> os;
        try {
            os = getDriver().newOutput(getModel(), options.and(ACCESS_PREFERENCES_MASK).set(CACHE), getParent(), getName(), is);
        } catch (FalsePositiveArchiveException e) {
            throw new AssertionError(e);
        } catch (final ControlFlowException e) {
            assert e instanceof NeedsLockRetryException : e;
            throw e;
        }
        val oa = new OutputArchive<>(os);
        setOutputArchive(Optional.of(oa));
        assert isMounted();
        return oa;
    }

    @Override
    InputSocket<E> input(String name) {
        return new InputSocket<E>() {

            InputSocket<E> socket;

            InputSocket<E> socket() {
                val s = socket;
                return null != s ? s : (this.socket = getInputArchive().get().input(name));
            }

            @Override
            public E getTarget() throws IOException {
                return socket().getTarget();
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return syncOn(ClosedInputException.class, () -> socket().stream(peer));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return syncOn(ClosedInputException.class, () -> socket().channel(peer));
            }
        };
    }

    @Override
    OutputSocket<E> output(BitField<FsAccessOption> options, E entry) {
        return new OutputSocket<E>() {

            OutputSocket<E> socket;

            OutputSocket<E> socket() throws IOException {
                val s = socket;
                return null != s ? s : (this.socket = outputArchive(options).output(entry));
            }

            @Override
            public E getTarget() {
                return entry;
            }

            @Override
            public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                return syncOn(ClosedOutputException.class, () -> socket().stream(peer));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer)
                    throws IOException {
                return syncOn(ClosedOutputException.class, () -> socket().channel(peer));
            }
        };
    }

    private static <A, X extends IOException> A syncOn(final Class<? extends X> klass, final Operation<A, X> op) throws X {
        try {
            return op.run();
        } catch (IOException e) {
            if (klass.isInstance(e)) {
                throw NeedsSyncException.apply();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void sync(final BitField<FsSyncOption> options) throws FsSyncException {
        try {
            val builder = new FsSyncExceptionBuilder();
            if (!options.get(ABORT_CHANGES)) {
                copy(builder);
            }
            close(options, builder);
            builder.check();
        } finally {
            assert invariants();
        }
    }

    /**
     * Synchronizes all entries in the (virtual) archive file system with the (temporary) output archive file.
     *
     * @param handler the strategy for assembling sync exceptions.
     */
    private void copy(final FsSyncExceptionBuilder handler) throws FsSyncException {
        // Skip (In|Out)putArchive for better performance.
        // This is safe because the ResourceController has already shut down all concurrent access by closing the
        // respective resources (streams, channels etc).
        // The Disconnecting(In|Out)putService should not get skipped however:
        // If these would throw an (In|Out)putClosedException, then this would be an artifact of a bug.

        val ois = _inputArchive
                .map(InputArchive::clutch)
                .filter(DisconnectingInputContainer::isOpen);
        final InputContainer<E> is;
        if (ois.isPresent()) {
            is = ois.get();
        } else {
            is = new DummyInputContainer<>();
        }

        val oos = _outputArchive
                .map(OutputArchive::clutch)
                .filter(DisconnectingOutputContainer::isOpen);
        final OutputContainer<E> os;
        if (oos.isPresent()) {
            os = oos.get();
        } else {
            return;
        }

        assert getFileSystem().isPresent();
        for (val cn : getFileSystem().get()) {
            for (val ae : cn.getEntries()) {
                val aen = ae.getName();
                try {
                    if (!os.entry(aen).isPresent()) {
                        if (DIRECTORY == ae.getType()) {
                            if (!cn.isRoot()) { // never output the root directory!
                                if (UNKNOWN != ae.getTime(WRITE)) { // never output a ghost directory!
                                    os.output(ae).stream(Optional.empty()).close();
                                }
                            }
                        } else if (is.entry(aen).isPresent()) {
                            IoSockets.copy(is.input(aen), os.output(ae));
                        } else {
                            // The file system entry is a newly created
                            // non-directory entry which hasn't received any
                            // content yet, e.g. as a result of make()
                            // => output an empty file system entry.
                            for (val size : ALL_SIZES) {
                                ae.setSize(size, UNKNOWN);
                            }
                            ae.setSize(DATA, 0);
                            os.output(ae).stream(Optional.empty()).close();
                        }
                    }
                } catch (IOException e) {
                    throw handler.fail(new FsSyncException(getMountPoint(), e));
                }
            }
        }
    }

    /**
     * Discards the file system, closes the input archive and finally the output archive.
     * Note that this order is critical: The parent file system controller is expected to replace the entry for the
     * target archive file with the output archive when it gets closed, so this must be done last.
     * Using a finally block ensures that this is done even in the unlikely event of an exception when closing the input
     * archive.
     * Note that in this case closing the output archive is likely to fail and override the IOException thrown by this
     * method, too.
     *
     * @param handler the strategy for assembling sync exceptions.
     */
    private void close(final BitField<FsSyncOption> options, final FsSyncExceptionBuilder handler) {
        // HC SVNT DRACONES!

        if (_inputArchive.isPresent()) {
            val ia = _inputArchive.get();
            try {
                ia.close();
            } catch (final ControlFlowException e) {
                assert e instanceof NeedsLockRetryException : e;
                throw e;
            } catch (IOException e) {
                handler.warn(new FsSyncWarningException(getMountPoint(), e));
            }
            setInputArchive(Optional.empty());
        }

        if (_outputArchive.isPresent()) {
            val oa = _outputArchive.get();
            try {
                oa.close();
            } catch (final ControlFlowException e) {
                assert e instanceof NeedsLockRetryException : e;
                throw e;
            } catch (IOException e) {
                handler.warn(new FsSyncException(getMountPoint(), e));
            }
            setOutputArchive(Optional.empty());
        }

        setFileSystem(Optional.empty());

        if (options.get(ABORT_CHANGES)) {
            setMounted(false);
        }
    }

    @Override
    void checkSync(final BitField<FsAccessOption> options, final FsNodeName name, final Entry.Access intention) throws NeedsSyncException {
        // HC SVNT DRACONES!

        // If no file system exists then pass the test.
        if (!getFileSystem().isPresent()) {
            return;
        }
        val fs = getFileSystem().get();

        // If GROWing and the driver supports the respective access method, then pass the test.
        if (options.get(GROW)) {
            switch (intention) {
                case READ:
                    break;
                case WRITE:
                    if (getDriver().getRedundantContentSupport()) {
                        getOutputArchive(); // side-effect!
                        return;
                    }
                    break;
                default:
                    if (getDriver().getRedundantMetaDataSupport()) {
                        return;
                    }
            }
        }

        // If the file system does not contain an entry with the given name, then pass the test.
        val optNode = fs.node(options, name);
        if (!optNode.isPresent()) {
            return;
        }
        val node = optNode.get();
        assert null != node.getEntry();
        val aen = node.getEntry().getName();

        // If the entry name addresses the file system root, then pass the test because the root entry cannot get input
        // or output anyway.
        if (name.isRoot()) {
            return;
        }

        // Check if the entry is already written to the output archive.
        if (getOutputArchive().isPresent()) {
            val oa = getOutputArchive().get();
            try {
                if (oa.entry(aen).isPresent()) {
                    throw NeedsSyncException.apply();
                }
            } catch (IOException e) {
                throw NeedsSyncException.apply();
            }
        }

        // If our intention is reading the entry then check if it's present in the input archive.
        if (intention == READ) {
            if (getInputArchive().isPresent()) {
                val ia = getInputArchive().get();
                try {
                    if (!ia.entry(aen).isPresent()) {
                        throw NeedsSyncException.apply();
                    }
                } catch (IOException e) {
                    throw NeedsSyncException.apply();
                }
            } else {
                throw NeedsSyncException.apply();
            }
        }
    }

    private final class TargetArchiveModel extends ArchiveModel<E> {

        TargetArchiveModel(FsArchiveDriver<E> driver, FsModel model) {
            super(driver, model);
        }

        @Override
        void touch(BitField<FsAccessOption> options) throws IOException {
            outputArchive(options);
        }
    }

    private static final class InputArchive<E extends FsArchiveEntry> extends LockInputContainer<E> {

        final InputContainer<E> driverProduct;

        InputArchive(InputContainer<E> driverProduct) {
            super(new DisconnectingInputContainer<>(driverProduct));
            this.driverProduct = driverProduct;
        }

        InputContainer<E> getDriverProduct() {
            return driverProduct;
        }

        boolean isOpen() {
            return clutch().isOpen();
        }

        DisconnectingInputContainer<E> clutch() {
            return (DisconnectingInputContainer<E>) getContainer();
        }
    }

    private static final class OutputArchive<E extends FsArchiveEntry> extends LockOutputContainer<E> {

        final OutputContainer<E> driverProduct;

        OutputArchive(OutputContainer<E> driverProduct) {
            super(new DisconnectingOutputContainer<>(driverProduct));
            this.driverProduct = driverProduct;
        }

        OutputContainer<E> getDriverProduct() {
            return driverProduct;
        }

        boolean isOpen() {
            return clutch().isOpen();
        }

        DisconnectingOutputContainer<E> clutch() {
            return (DisconnectingOutputContainer<E>) getContainer();
        }
    }

    private static final class DummyInputContainer<E extends Entry> implements InputContainer<E> {

        private int size() {
            return 0;
        }

        @Override
        public Collection<E> entries() {
            return Collections.emptyList();
        }

        @Override
        public Optional<E> entry(String name) {
            return Optional.empty();
        }

        @Override
        public InputSocket<E> input(String name) {
            throw new AssertionError();
        }

        @Override
        public void close() throws IOException {
            throw new AssertionError();
        }
    }
}
