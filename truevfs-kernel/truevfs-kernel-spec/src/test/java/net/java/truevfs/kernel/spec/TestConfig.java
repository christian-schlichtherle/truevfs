/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.kernel.spec.cio.ByteArrayIoBufferPool;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truecommons.shed.InheritableThreadLocalStack;
import net.java.truecommons.shed.Resource;

/**
 * A container for configuration options with global or inheritable thread
 * local scope.
 * <p>
 * A thread can call {@link #ioBufferPool()} to ioBufferPool access to the
 * <i>current configuration</i> at any time .
 * If no configuration has been pushed onto the inheritable thread local
 * configuration stack before, this will return the <i>global configuration</i>
 * which is shared by all threads (hence its name).
 * Mind that access to the global configuration is <em>not</em> synchronized.
 * <p>
 * To create an <i>inheritable thread local configuration</i>, a thread can
 * simply call {@link #push()}.
 * This will copy the <i>current configuration</i> (which may be identical to
 * the global configuration) and push the copy on top of the inheritable thread
 * local configuration stack.
 * <p>
 * Later, the thread can use {@link #pop()} or {@link #close()} to
 * pop the current configuration or {@code this} configuration respectively
 * off the top of the inheritable thread local configuration stack again.
 * <p>
 * Finally, whenever a child thread gets started, it will share the
 * <em>same</em> current configuration with its parent thread.
 * This is achieved by copying the top level element of the parent's
 * inheritable thread local configuration stack.
 * If the parent's inheritable thread local configuration stack is empty, then
 * the child will share the global configuration as its current configuration
 * with its parent.
 * As an implication, {@link #pop()} or {@link #close()} can be called at most
 * once in the child thread.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
@CleanupObligation
public final class TestConfig extends Resource<RuntimeException> {

    public static final int DEFAULT_NUM_ENTRIES = 10;
    public static final int DEFAULT_DATA_LENGTH = 1024;

    private static final InheritableThreadLocalStack<TestConfig>
            configs = new InheritableThreadLocalStack<>();

    private static final TestConfig GLOBAL = new TestConfig();

    // I don't think this field should be volatile.
    // This would make a difference if and only if two threads were changing
    // the GLOBAL configuration concurrently, which is discouraged.
    // Instead, the global configuration should only ioBufferPool changed once at
    // application startup and then each thread should modify only its thread
    // local configuration which has been obtained by a call to TestConfig.push().
    private final ThrowManager throwControl;
    private int numEmtries = DEFAULT_NUM_ENTRIES;
    private int dataSize = DEFAULT_DATA_LENGTH;
    private IoBufferPool<? extends IoBuffer<?>> pool;

    /**
     * Returns the current configuration.
     * First, this method peeks the inheritable thread local configuration
     * stack.
     * If no configuration has been {@link #push() pushed} yet, the global
     * configuration is returned.
     * Mind that the global configuration is shared by all threads.
     * 
     * @return The current configuration.
     * @see    #push()
     */
    public static TestConfig get() {
        return configs.peekOrElse(GLOBAL);
    }

    /**
     * Creates a new current configuration by copying the current configuration
     * and pushing the copy onto the inheritable thread local configuration
     * stack.
     * 
     * @return The new current configuration.
     * @see    #ioBufferPool()
     */
    @CreatesObligation
    public static TestConfig push() {
        return configs.push(new TestConfig(get()));
    }

    /**
     * Pops the {@link #ioBufferPool() current configuration} off the inheritable thread
     * local configuration stack.
     * 
     * @throws IllegalStateException If the {@link #ioBufferPool() current configuration}
     *         is the global configuration.
     */
    public static void pop() {
        configs.popIf(get());
    }

    /** Default constructor for the global configuration. */
    private TestConfig() {
        this.throwControl = new ThrowManager();
    }

    /** Copy constructor for inheritable thread local configurations. */
    private TestConfig(final TestConfig template) {
        this.throwControl = new ThrowManager(template.getThrowControl());
        this.numEmtries = template.getNumEntries();
        this.dataSize = template.getDataSize();
        this.pool = template.getPool();
    }

    public ThrowManager getThrowControl() {
        return this.throwControl;
    }

    public int getNumEntries() {
        return this.numEmtries;
    }

    public void setNumEntries(final int numEntries) {
        if (0 > numEntries)
            throw new IllegalArgumentException();
        this.numEmtries = numEntries;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(final int size) {
        if (0 > size)
            throw new IllegalArgumentException();
        dataSize = size;
    }

    public IoBufferPool<? extends IoBuffer<?>> getPool() {
        final IoBufferPool<? extends IoBuffer<?>> pool = this.pool;
        return null != pool ? pool : (this.pool = new ByteArrayIoBufferPool(getDataSize()));
    }

    public void setIoBufferPool(final @CheckForNull IoBufferPool<? extends IoBuffer<?>> pool) {
        this.pool = pool;
    }

    @Override
    @DischargesObligation
    public void close() {
        super.close();
    }

    @Override
    protected void onClose() {
        configs.popIf(this);
    }
}
