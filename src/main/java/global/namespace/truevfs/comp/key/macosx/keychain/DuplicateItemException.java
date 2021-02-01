/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macosx.keychain;

/**
 * Indicates that an item is already present in a keychain.
 *
 * @author Christian Schlichtherle
 */
public class DuplicateItemException extends KeychainException {

    DuplicateItemException() { super(Security.errSecDuplicateItem); }
}
