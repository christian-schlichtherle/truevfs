/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Provides a singleton to locate an I/O buffer pool service provider on the
 * class path.
 * <p>
 * Note that the TrueZIP Kernel module has no dependency on this package
 * - so using it is entirely optional.
 */
@DefaultAnnotation(NonNull.class)
package de.schlichtherle.truezip.socket.sl;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
