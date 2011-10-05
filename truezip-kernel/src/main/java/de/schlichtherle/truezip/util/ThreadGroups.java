/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

/**
 * Static utility methods for working with {@link ThreadGroup}s.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ThreadGroups {

    /** You cannot instantiate this class. */
    private ThreadGroups() {
    }

    /**
     * Returns the top level thread group of the current thread.
     * 
     * @return The top level thread group of the current thread.
     */
    public static ThreadGroup getTopLevel() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (ThreadGroup ntg = tg; null != ntg; tg = ntg, ntg = tg.getParent()) {
        }
        return tg;
    }
}

