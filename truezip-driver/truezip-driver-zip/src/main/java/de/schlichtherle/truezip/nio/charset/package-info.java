/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Provides an implementation of the {@code IBM437} character set.
 * This is the original IBM PC character set for the USA, also known as
 * {@code CP437}, and is the original character set used in ZIP files.
 * <p>
 * This implementation is provided because the IBM437 character set does not
 * need to be present in a JRE.
 * In Oracle's JRE, if installed, it's contained in {@code lib/charsets.jar} in
 * the JRE home directory.
 */
@DefaultAnnotation(NonNull.class)
package de.schlichtherle.truezip.nio.charset;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
