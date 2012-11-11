/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.macosx.keychain;

/**
 * Indicates that an item is already present in a keychain.
 *
 * @author Christian Schlichtherle
 */
public class DuplicateItemException extends KeychainException {

    DuplicateItemException() { super(Security.errSecDuplicateItem); }
}
