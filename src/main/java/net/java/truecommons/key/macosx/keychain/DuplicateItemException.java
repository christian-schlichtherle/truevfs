/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx.keychain;

/**
 * Indicates that an item is already present in a keychain.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public class DuplicateItemException extends KeychainException {

    DuplicateItemException() { super(Security.errSecDuplicateItem); }
}
