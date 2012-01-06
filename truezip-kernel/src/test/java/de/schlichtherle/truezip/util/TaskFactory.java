/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import java.util.concurrent.Callable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface TaskFactory {
    Callable<Void> newTask(int threadNum);
}
