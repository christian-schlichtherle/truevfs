/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import static global.namespace.truevfs.comp.util.Buffers.byteBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public abstract class SecretKeyTestSuite<K extends SecretKey<K>> {

    private static final Logger logger = LoggerFactory.getLogger(SecretKeyTestSuite.class);

    private K key;

    protected abstract K newKey();

    @Before
    public void before() {
        key = newKey();
    }

    @Test
    public void testNoSecret() {
        assertFalse(key.getSecret().hasRemaining());
        assertThat(key.getSecret().position(), is(0));
    }

    @Test
    public void testSetKeyMakesAProtectiveCopy() {
        val b1 = UTF_8.encode("föo");
        key.setSecret(b1);
        assertTrue(UTF_8.newEncoder().encode(CharBuffer.wrap("bär"), b1, true).isUnderflow());
        val b2 = key.getSecret();
        assertThat(b2, is(not(b1)));
    }

    @Test
    public void testGetKeyMakesAProtectiveCopy() {
        key.setSecret(UTF_8.encode("föo"));
        val b1 = key.getSecret();
        val b2 = key.getSecret();
        assertNotSame(b1, b2);
        assertEquals(b1, b2);
    }

    @Test
    public void testCloneMakesAProtectiveCopyOfTheKey() {
        val secret = UTF_8.encode("föo");
        key.setSecret(secret); // copies byte buffer
        val clone = key.clone();
        key.setSecret(ByteBuffer.allocate(0)); // clears byte buffer
        assertThat(clone.getSecret(), is(secret));
    }

    @Test
    public void testObjectSerialization() throws Exception {
        assertEquals(key, cloneViaObjectSerialization(512, updateTransientProperties(key)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneViaObjectSerialization(
            final int sizeHint,
            final T original
    ) throws IOException, ClassNotFoundException {
        final byte[] serialized;
        try (val bos = new ByteArrayOutputStream(sizeHint)) {
            try (val out = new ObjectOutputStream(bos)) {
                out.writeObject(original);
            }
            bos.flush(); // redundant
            serialized = bos.toByteArray();
        }
        logger.trace("Serialized object to {} bytes.", serialized.length);
        try (val ois = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            return (T) ois.readObject();
        }
    }

    @Test
    public void testXmlSerialization() throws Exception {
        assertEquals(key, cloneViaXmlSerialization(512, updateTransientProperties(key)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneViaXmlSerialization(
            final int sizeHint,
            final T original
    ) throws IOException {
        final byte[] serialized;
        try (val bos = new ByteArrayOutputStream(sizeHint)) {
            try (val encoder = new XMLEncoder(bos)) {
                encoder.writeObject(original);
            }
            bos.flush(); // redundant
            serialized = bos.toByteArray();
        }
        logger.trace("Serialized object to {} bytes.", serialized.length);
        logger.trace("Serialized form:\n{}", new String(serialized, StandardCharsets.UTF_8));
        try (val decoder = new XMLDecoder(new ByteArrayInputStream(serialized))) {
            return (T) decoder.readObject();
        }
    }

    protected K updateTransientProperties(K key) {
        key = key.clone();
        key.setSecret(byteBuffer("This secret must not get serialized!"));
        return key;
    }
}
