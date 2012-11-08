package net.java.truevfs.key.osx.keychain;

/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import static net.java.truevfs.key.spec.BufferUtils.*;
import net.java.truevfs.key.osx.keychain.Keychain.AttributeClass;
import net.java.truevfs.key.osx.keychain.Keychain.Item;
import net.java.truevfs.key.osx.keychain.Keychain.Visitor;

public class ListDefaultKeychain {

    public static void main(final String[] args) throws KeychainException {
        final boolean data = 0 < args.length && "-data".equals(args[0]);

        final Visitor visitor = new Visitor() {
            final PrintStream out = System.out;

            @Override
            public void visit(final Item item) {
                try {
                    out.printf("\nClass: %s\n", item.getItemClass());
                    for (final Entry<AttributeClass, ByteBuffer> entry
                            : item.getAttributes().entrySet())
                        out.printf("Attribute: %s=%s\n",
                                entry.getKey(), string(entry.getValue()));
                    if (data) out.printf("Data: %s\n", string(item.getSecret()));
                } catch (final KeychainException ex) {
                    ex.printStackTrace();
                }
            }
        };

        try (final Keychain kc = Keychain.open()) {
            kc.visitItems(null, null, visitor);
        }
    }
}
