/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes;

/**
 * A marker interface for RAES parameters.
 * RAES files feature different types to model extensibility.
 * Each type determines the algorithms and parameter types used
 * to encrypt and decrypt the pay load data in the RAES file.
 * Hence, for each type a separate parameter interface is used which extends
 * this marker interface.
 * <p>
 * Implementations do not need to be safe for multi-threading.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface RaesParameters {
}
