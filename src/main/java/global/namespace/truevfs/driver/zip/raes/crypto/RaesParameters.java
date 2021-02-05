/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes.crypto;

/**
 * A marker interface for RAES parameters.
 * RAES files feature different types to model extensibility.
 * Each type determines the algorithms and parameter types used
 * to encrypt and decrypt the pay load data in the RAES file.
 * Hence, for each type a separate parameter interface is used which extends
 * this marker interface.
 * <p>
 * Implementations do not need to be thread-safe.
 * 
 * @author  Christian Schlichtherle
 */
public interface RaesParameters {
}