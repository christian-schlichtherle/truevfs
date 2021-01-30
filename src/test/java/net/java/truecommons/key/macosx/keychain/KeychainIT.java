/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx.keychain;

import net.java.truecommons.key.macosx.keychain.Keychain.AttributeClass;
import net.java.truecommons.key.macosx.keychain.Keychain.Item;
import net.java.truecommons.key.macosx.keychain.Keychain.Visitor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

import static net.java.truecommons.key.macosx.keychain.Keychain.AttributeClass.*;
import static net.java.truecommons.key.macosx.keychain.Keychain.ItemClass.GENERIC_PASSWORD;
import static net.java.truecommons.shed.Buffers.byteBuffer;
import static net.java.truecommons.shed.Buffers.string;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Schlichtherle
 */
public class KeychainIT {

    private static Keychain kc;

    @BeforeClass
    public static void beforeClass() throws KeychainException {
        kc = Keychain.open(/*"test1234", "test1234".toCharArray()*/);
    }

    @AfterClass
    public static void afterClass() throws KeychainException {
        if (null == kc) return;
        try {
            //kc.delete();
        } finally {
            kc.close();
        }
    }

    @Test
    public void createAndDeleteItemWithoutSearchAttributes()
    throws KeychainException {
        kc.createItem(GENERIC_PASSWORD,
                build("createAndDeleteItemWithoutSearchAttributes").get(),
                byteBuffer("töp secret"));
        final Delete delete = new Delete("createAndDeleteItemWithoutSearchAttributes");
        kc.visitItems(null, null, delete);
        assertTrue(delete.deleted);
    }

    @Test
    public void createAndDeleteItemWithSearchAttributes()
    throws KeychainException {
        kc.createItem(GENERIC_PASSWORD,
                build("createAndDeleteItemWithSearchAttributes").get(),
                byteBuffer("töp secret"));
        final Delete delete = new Delete("createAndDeleteItemWithSearchAttributes");
        kc.visitItems(GENERIC_PASSWORD,
                build("createAndDeleteItemWithSearchAttributes").get(),
                delete);
        assertTrue(delete.deleted);
    }

    @Test
    public void createItemAndModifyServiceAttributeAndDelete()
    throws KeychainException {
        kc.createItem(GENERIC_PASSWORD,
                build("öld createItemAndModifyServiceAttributeAndDelete").get(),
                byteBuffer("töp secret"));
        final ModifyAttribute modify = new ModifyAttribute(
                "öld createItemAndModifyServiceAttributeAndDelete",
                SERVICE, "nëw createItemAndModifyServiceAttributeAndDelete");
        kc.visitItems(null, null, modify);
        assertTrue(modify.modified);
        final Delete delete = new Delete("nëw createItemAndModifyServiceAttributeAndDelete");
        kc.visitItems(null, null, delete);
        assertTrue(delete.deleted);
    }

    @Test
    public void createItemAndModifyGenericAttributeAndDelete()
    throws KeychainException {
        kc.createItem(GENERIC_PASSWORD,
                build("createItemAndModifyGenericAttributeAndDelete")
                    .put(GENERIC, "öld generic")
                    .get(),
                byteBuffer("töp secret"));
        final ModifyAttribute modify = new ModifyAttribute(
                "createItemAndModifyGenericAttributeAndDelete",
                GENERIC, "nëw generic");
        kc.visitItems(null, null, modify);
        assertTrue(modify.modified);
        final Delete delete = new Delete("createItemAndModifyGenericAttributeAndDelete");
        kc.visitItems(GENERIC_PASSWORD,
                build("createItemAndModifyGenericAttributeAndDelete")
                    .put(GENERIC, "nëw generic")
                    .get(),
                delete);
        assertTrue(delete.deleted);
    }

    @Test
    public void createItemAndModifyPasswordAndDelete()
    throws KeychainException {
        kc.createItem(GENERIC_PASSWORD,
                build("createItemAndModifyPasswordAndDelete").get(),
                byteBuffer("öld secret"));
        final ModifyData modify = new ModifyData(
                "createItemAndModifyPasswordAndDelete",
                "nëw secret");
        kc.visitItems(null, null, modify);
        assertTrue(modify.modified);
        final Delete delete = new Delete("createItemAndModifyPasswordAndDelete",
                "nëw secret");
        kc.visitItems(null, null, delete);
        assertTrue(delete.deleted);
    }

    private static MapBuilder build(final String service) {
        return new MapBuilder()
                //.put(CREATION_DATE, null)
                //.put(MOD_DATE, null)
                .put(DESCRIPTION, "description")
                .put(COMMENT, "comment")
                .put(CREATOR, "crtr") // Fourchar code
                .put(TYPE, "type") // Fourchar code
                //.put(SCRIPT_CODE, null)
                //.put(LABEL, "service")
                //.put(INVISIBLE, null)
                //.put(NEGATIVE, null)
                //.put(CUSTOM_ICON, null)
                .put(ACCOUNT, "account")
                .put(SERVICE, service)
                .put(GENERIC, "generic")
                .put(ALIAS, "alias"); // not supported when reading
    }

    private static class MapBuilder {

        private Map<AttributeClass, ByteBuffer> map = new EnumMap<>(AttributeClass.class);

        MapBuilder put(final AttributeClass id, final @Nullable String string) {
            if (null != string)
                map.put(id, byteBuffer(string));
            else
                map.remove(id);
            return this;
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        Map<AttributeClass, ByteBuffer> get() { return map; }
    }

    private static class ModifyAttribute implements Visitor {

        final String service;
        final AttributeClass id;
        final @Nullable String string;
        boolean modified;

        ModifyAttribute(
                final String service,
                final AttributeClass id,
                final @Nullable String string) {
            this.service = service;
            this.id = id;
            this.string = string;
        }

        @Override
        public void visit(final Item item) throws KeychainException {
            final Map<AttributeClass, ByteBuffer> attributes = item.getAttributeMap();
            final String service = string(attributes.get(SERVICE));
            if (!this.service.equals(service))
                return;
            attributes.put(id, byteBuffer(string));
            item.putAttributeMap(attributes);
            modified = true;
        }
    }

    private static class ModifyData implements Visitor {

        final String service, data;
        boolean modified;

        ModifyData(
                final String service,
                final String data) {
            this.service = service;
            this.data = data;
        }

        @Override
        public void visit(final Item item) throws KeychainException {
            final Map<AttributeClass, ByteBuffer> attributes = item.getAttributeMap();
            final String service = string(attributes.get(SERVICE));
            if (!this.service.equals(service))
                return;
            item.setSecret(byteBuffer(data));
            modified = true;
        }
    }

    private static class Delete implements Visitor {

        final String service;
        final @Nullable String data;
        boolean deleted;

        Delete(final String service) { this(service, null); }
        Delete(final String service, final @Nullable String data) {
            this.service = service;
            this.data = data;
        }

        @Override
        public void visit(final Item item) throws KeychainException {
            final ByteBuffer service = item.getAttributeMap().get(SERVICE);
            if (!this.service.equals(string(service)))
                return;
            if (null != data)
                assertEquals(data, string(item.getSecret()));
            item.delete();
            deleted = true;
        }
    }
}
