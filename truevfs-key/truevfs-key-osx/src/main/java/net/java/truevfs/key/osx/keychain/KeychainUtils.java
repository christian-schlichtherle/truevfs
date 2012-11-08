/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.osx.keychain;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import net.java.truevfs.key.osx.keychain.Keychain.AttributeClass;
import static net.java.truevfs.key.osx.keychain.Security.*;
import net.java.truevfs.key.osx.keychain.Security.SecKeychainAttribute;
import net.java.truevfs.key.osx.keychain.Security.SecKeychainAttributeInfo;
import net.java.truevfs.key.osx.keychain.Security.SecKeychainAttributeList;

/**
 * @author Christian Schlichtherle
 */
class KeychainUtils {

    private KeychainUtils() { }

    static @CheckForNull SecKeychainAttributeList list(
            final @CheckForNull Map<AttributeClass, ByteBuffer> map) {
        if (null == map) return null;
        final SecKeychainAttributeList list = new SecKeychainAttributeList();
        final int size = map.size();
        if (0 >= size) return list;
        final SecKeychainAttribute[] array = (SecKeychainAttribute[])
                new SecKeychainAttribute().toArray(size);
        int count = 0;
        for (final Map.Entry<AttributeClass, ByteBuffer> entry : map.entrySet()) {
            final AttributeClass id = entry.getKey();
            if (null == id) continue;
            final SecKeychainAttribute attr = array[count++];
            attr.tag = id.getTag();
            final ByteBuffer buffer = entry.getValue();
            if (null != buffer) {
                final int length = buffer.remaining();
                final Pointer data = malloc(length);
                buffer.mark();
                data.getByteBuffer(0, length).put(buffer);
                buffer.reset();
                attr.length = length;
                attr.data = data;
            }
            attr.write();
        }
        list.count = count;
        if (0 < count) list.attr = array[0].getPointer();
        return list;
    }

    private static Pointer malloc(int size) {
        if (0 < size) return new Memory(size);
        else if (0 == size) return (Memory) new Memory(4).share(0, 0); // fix
        else throw new IllegalArgumentException("" + size);
    }

    static @CheckForNull Map<AttributeClass, ByteBuffer> map(
            final @CheckForNull SecKeychainAttributeList list) {
        if (null == list) return null;
        final Map<AttributeClass, ByteBuffer>
                map = new EnumMap<>(AttributeClass.class);
        final int count = list.count;
        if (0 >= count) return map;
        final SecKeychainAttribute[] array;
        {
            final SecKeychainAttribute
                    attr = new SecKeychainAttribute(list.attr);
            attr.read();
            array = (SecKeychainAttribute[]) attr.toArray(count);
        }
        for (final SecKeychainAttribute attr : array) {
            final AttributeClass id = AttributeClass.lookup(attr.tag);
            if (null == id) continue;
            final Pointer data = attr.data;
            if (null == data) continue;
            final int length = attr.length;
            final ByteBuffer buffer = (ByteBuffer) ByteBuffer
                    .allocateDirect(length)
                    .put(data.getByteBuffer(0, length))
                    .flip();
            map.put(id, buffer);
        }
        return map;
    }

    static SecKeychainAttributeInfo info(final AttributeClass... ids) {
        final SecKeychainAttributeInfo info = new SecKeychainAttributeInfo();
        final int length = ids.length;
        info.count = length;
        final int size = length << 2;
        final Pointer tag = new Memory(size << 1);
        final Pointer format = tag.share(size, size);
        info.tag = tag;
        info.format  = format;
        int offset = 0;
        for (final AttributeClass id : ids) {
            tag.setInt(offset, id.getTag());
            format.setInt(offset, kSecFormatUnknown);
            offset += 4;
        }
        return info;
    }
}
