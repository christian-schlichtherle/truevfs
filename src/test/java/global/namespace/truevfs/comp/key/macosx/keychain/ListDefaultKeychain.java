/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macosx.keychain;

import global.namespace.truevfs.comp.key.macosx.keychain.Keychain.AttributeClass;
import global.namespace.truevfs.comp.key.macosx.keychain.Keychain.Item;
import global.namespace.truevfs.comp.key.macosx.keychain.Keychain.Visitor;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Map.Entry;

import static global.namespace.truevfs.comp.shed.Buffers.string;

public class ListDefaultKeychain {

    public static void main(final String[] args) throws KeychainException {
        final boolean data = 0 < args.length && "-data".equals(args[0]);

        final Visitor visitor = new Visitor() {

            final PrintStream out = System.out;

            @Override
            public void visit(final Item item) {
                try {
                    out.printf("\nClass: %s\n", item.getItemClass());
                    for (Entry<AttributeClass, ByteBuffer> entry : item.getAttributeMap().entrySet()) {
                        out.printf("Attribute: %s=%s\n", entry.getKey(), string(entry.getValue()));
                    }
                    if (data) {
                        out.printf("Data: %s\n", string(item.getSecret()));
                    }
                } catch (final KeychainException ex) {
                    ex.printStackTrace();
                }
            }
        };

        try (Keychain kc = Keychain.open()) {
            kc.visitItems(null, null, visitor);
        }
    }
}
