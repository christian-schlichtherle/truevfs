package net.java.truevfs.key.macosx.keychain;

/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import net.java.truevfs.key.macosx.keychain.Keychain.AttributeClass;
import net.java.truevfs.key.macosx.keychain.Keychain.Item;
import net.java.truevfs.key.macosx.keychain.Keychain.Visitor;
import static net.java.truevfs.key.spec.util.Buffers.*;

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
                            : item.getAttributeMap().entrySet())
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
