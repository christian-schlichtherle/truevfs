/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.macos.keychain;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import global.namespace.truevfs.commons.key.macos.keychain.Keychain.AttributeClass;
import global.namespace.truevfs.commons.key.macos.keychain.Security.SecKeychainAttribute;
import global.namespace.truevfs.commons.key.macos.keychain.Security.SecKeychainAttributeInfo;
import global.namespace.truevfs.commons.key.macos.keychain.Security.SecKeychainAttributeList;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static global.namespace.truevfs.commons.key.macos.keychain.Security.kSecFormatUnknown;

/**
 * Utilities for Apple's Keychain Services API.
 *
 * @author Christian Schlichtherle
 */
final class KeychainUtils {

    private KeychainUtils() {
    }

    static SecKeychainAttributeList list(final Map<AttributeClass, ByteBuffer> map) {
        val list = new SecKeychainAttributeList();
        val size = map.size();
        if (0 < size) {
            val array = (SecKeychainAttribute[]) new SecKeychainAttribute().toArray(size);
            int count = 0;
            for (val entry : map.entrySet()) {
                val optId = Optional.ofNullable(entry.getKey());
                if (optId.isPresent()) {
                    val attr = array[count++];
                    attr.tag = optId.get().getTag();
                    Optional.ofNullable(entry.getValue()).ifPresent(buffer -> {
                        val length = buffer.remaining();
                        val data = malloc(length);
                        buffer.mark();
                        data.getByteBuffer(0, length).put(buffer);
                        buffer.reset();
                        attr.length = length;
                        attr.data = data;
                    });
                    attr.write();
                }
            }
            list.count = count;
            if (0 < count) {
                list.attr = array[0].getPointer();
            }
        }
        return list;
    }

    private static Pointer malloc(final int size) {
        if (0 < size) {
            return new Memory(size);
        } else if (0 == size) {
            return new Memory(4).share(0, 0); // fix
        } else {
            throw new IllegalArgumentException("" + size);
        }
    }

    static Map<AttributeClass, ByteBuffer> map(final SecKeychainAttributeList list) {
        val map = new EnumMap<AttributeClass, ByteBuffer>(AttributeClass.class);
        val count = list.count;
        if (0 < count) {
            final SecKeychainAttribute[] array;
            {
                final SecKeychainAttribute attr = new SecKeychainAttribute(list.attr);
                attr.read();
                array = (SecKeychainAttribute[]) attr.toArray(count);
            }
            for (val attr : array) {
                AttributeClass.lookup(attr.tag).ifPresent(id -> {
                    Optional.ofNullable(attr.data).ifPresent(data -> {
                        val length = attr.length;
                        val buffer = (ByteBuffer) ByteBuffer
                                .allocateDirect(length)
                                .put(data.getByteBuffer(0, length))
                                .flip();
                        map.put(id, buffer);
                    });
                });
            }
        }
        return map;
    }

    static SecKeychainAttributeInfo info(final AttributeClass... ids) {
        val info = new SecKeychainAttributeInfo();
        val length = ids.length;
        info.count = length;
        val size = length << 2;
        val tag = new Memory((long) size << 1);
        val format = tag.share(size, size);
        info.tag = tag;
        info.format = format;
        int offset = 0;
        for (val id : ids) {
            tag.setInt(offset, id.getTag());
            format.setInt(offset, kSecFormatUnknown);
            offset += 4;
        }
        return info;
    }
}
