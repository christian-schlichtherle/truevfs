/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access.exp;

import java.lang.ref.WeakReference;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class TConfigs {

    private static final Node global = new Node(null, null, TConfig.DEFAULT);
    private static final WeakHashMap<Thread, Node> nodes = new WeakHashMap<>();
    private static final ReentrantReadWriteLock.ReadLock readLock;
    private static final ReentrantReadWriteLock.WriteLock writeLock;
    static {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    private static Node node() {
        final Thread thread = Thread.currentThread();
        readLock.lock();
        try {
            final Node node = nodes.get(thread);
            return null != node ? node : global;
        } finally {
            readLock.unlock();
        }
    }

    public static TConfig get() { return node().config; }

    public static void set(final TConfig config) {
        node().config = Objects.requireNonNull(config);
    }

    public static void push(final TConfig config) {
        final Thread thread = Thread.currentThread();
        writeLock.lock();
        try {
            final Node previous = nodes.get(thread);
            final Node next = new Node(thread, previous, config);
            nodes.put(thread, next);
        } finally {
            writeLock.unlock();
        }
    }

    public static TConfig pop() {
        final Thread thread = Thread.currentThread();
        writeLock.lock();
        try {
            final Node node = nodes.get(thread);
            if (null == node) throw new NoSuchElementException();
            final Node previous = node.previous;
            if (null != previous && thread.equals(previous.get()))
                nodes.put(thread, previous);
            else
                nodes.remove(thread);
            return node.config;
        } finally {
            writeLock.unlock();
        }
    }

    private TConfigs() { }

    private static class Node extends WeakReference<Thread> {
        final @CheckForNull Node previous;

        TConfig config;

        Node(   final @CheckForNull Thread owner,
                final @CheckForNull Node previous,
                final TConfig config) {
            super(owner);
            this.previous = previous;
            this.config = config;
        }
    }
}
