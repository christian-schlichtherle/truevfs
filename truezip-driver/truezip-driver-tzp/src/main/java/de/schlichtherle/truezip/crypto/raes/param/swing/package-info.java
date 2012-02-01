/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Provides the Swing based implementation of the interface
 * {@link de.schlichtherle.truezip.key.PromptingKeyProvider.View}.
 * If a key file is used for authentication instead of a password, then the
 * file size must be 512 bytes or more, of which only the first 512 bytes are
 * used.
 */
@DefaultAnnotation(NonNull.class)
package de.schlichtherle.truezip.crypto.raes.param.swing;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
