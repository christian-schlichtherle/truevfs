/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import lombok.val;

import javax.annotation.concurrent.ThreadSafe;
import java.util.function.Supplier;

/**
 * Arms and disarms a configured shutdown hook.
 * A shutdown fuse allows to repeatedly register and remove its configured shutdown hook for execution when the JVM
 * shuts down.
 * The configured shutdown hook will only get executed when the JVM shuts down and if the shutdown fuse is currently
 * "arm"ed.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ShutdownFuse {

    private final Thread thread;
    private final ThreadRegistry registry;

    volatile boolean armed;

    ShutdownFuse(Runnable hook) {
        this(hook, ThreadRegistry.INSTANCE);
    }

    ShutdownFuse(final Runnable hook, final ThreadRegistry registry) {
        // HC SVNT DRACONES!
        // MUST void any calls to disarm() during shutdown hook execution!
        this.thread = new Thread(() -> onDisarm(hook)); // could call disarm()!
        this.registry = registry;
    }

    /**
     * Arms this shutdown fuse.
     */
    ShutdownFuse arm() {
        return onArm(() -> registry.add(thread));
    }

    /**
     * Disarms this shutdown fuse.
     */
    ShutdownFuse disarm() {
        return onDisarm(() -> registry.remove(thread));
    }

    private ShutdownFuse onArm(final Runnable block) {
        return onCondition(() -> !armed, () -> {
            armed = true;
            block.run();
        });
    }

    private ShutdownFuse onDisarm(final Runnable block) {
        return onCondition(() -> armed, () -> {
            armed = false;
            block.run();
        });
    }

    private ShutdownFuse onCondition(final Supplier<Boolean> condition, final Runnable block) {
        if (condition.get()) {
            synchronized (this) {
                if (condition.get()) {
                    block.run();
                }
            }
        }
        return this;
    }

    /**
     * For testing only!
     */
    @SuppressWarnings("CallToThreadRun")
    void blowUp() {
        thread.run();
    }

    interface ThreadRegistry {

        ThreadRegistry INSTANCE = new ThreadRegistry() {
        };

        default void add(final Thread thread) {
            try {
                Runtime.getRuntime().addShutdownHook(thread);
            } catch (IllegalStateException ignored) {
            }
        }

        default void remove(final Thread thread) {
            try {
                Runtime.getRuntime().removeShutdownHook(thread);
            } catch (IllegalStateException ignored) {
            }
        }
    }
}
