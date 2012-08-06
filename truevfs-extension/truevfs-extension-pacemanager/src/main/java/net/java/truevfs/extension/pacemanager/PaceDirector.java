/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.pacemanager;

import javax.annotation.concurrent.ThreadSafe;

/**
 * The singleton pace director.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class PaceDirector {

    static final PaceDirector SINGLETON = new PaceDirector();

    private PaceDirector() { }
}
