/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macos.keychain;

import global.namespace.truevfs.comp.key.macos.keychain.Keychain.Visitor;
import lombok.val;

import static global.namespace.truevfs.comp.util.Buffers.string;
import static java.lang.System.out;

public class ListDefaultKeychain {

    public static void main(final String... args) throws KeychainException {
        try (val chain = Keychain.open()) {
            val data = 0 < args.length && "-data".equals(args[0]);
            chain.visitItems(null, null, (Visitor) item -> {
                try {
                    out.printf("\nClass: %s\n", item.getItemClass());
                    for (val entry : item.getAttributeMap().entrySet()) {
                        out.printf("Attribute: %s=%s\n", entry.getKey(), string(entry.getValue()));
                    }
                    if (data) {
                        out.printf("Data: %s\n", string(item.getSecret()));
                    }
                } catch (final KeychainException ex) {
                    ex.printStackTrace();
                }
            });
        }
    }
}
