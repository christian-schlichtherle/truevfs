/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access.exp;

import java.lang.ref.WeakReference;
import java.util.NoSuchElementException;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class TConfigs {

    private static final InheritableThreadLocalStack stack =
            new InheritableThreadLocalStack();
    
    public static TConfig get() { return stack.get(); }

    public static void set(TConfig config) { stack.set(config); }

    public static void push(TConfig config) { stack.push(config); }

    public static TConfig pop() { return stack.pop(); }

    private TConfigs() { }

    @SuppressWarnings("PackageVisibleInnerClass")
    static final class InheritableThreadLocalStack {
        private final Node global = new Node(null, TConfig.DEFAULT);
        private final InheritableThreadLocal<Node> nodes =
                new InheritableThreadLocal<>();

        private Node node() {
            final Node node = nodes.get();
            return null != node ? node : global;
        }

        public TConfig get() {
            return node().config;
        }

        public void set(TConfig config) {
            node().config = Objects.requireNonNull(config);
        }

        public void push(final TConfig config) {
            final Node previous = nodes.get();
            final Node next = new Node(previous, config);
            nodes.set(next);
        }

        public TConfig pop() {
            final Node node = nodes.get();
            if (null == node || !Thread.currentThread().equals(node.get()))
                throw new NoSuchElementException();
            final Node previous = node.previous;
            if (null != previous) nodes.set(previous);
            else nodes.remove();
            return node.config;
        }

        private static class Node extends WeakReference<Thread> {
            final @CheckForNull Node previous;

            TConfig config;

            Node(   final @CheckForNull Node previous,
                    final TConfig config) {
                super(Thread.currentThread());
                this.previous = previous;
                this.config = config;
            }
        } // Node
    } // InheritableThreadLocalStack
}
