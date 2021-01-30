/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec;

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

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.java.truecommons3.shed.Buffers.byteBuffer;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public abstract class SecretKeyTestSuite<K extends SecretKey<K>> {

    private static final Logger logger =
            LoggerFactory.getLogger(SecretKeyTestSuite.class);

    private K key;

    protected abstract K newKey();

    @Before
    public void before() {
        key = newKey();
    }

    @Test
    public void testNoSecret() {
        assertNull(key.getSecret());
    }

    @Test
    public void testSetKeyMakesAProtectiveCopy() {
        final ByteBuffer b1 = UTF_8.encode("föo");
        key.setSecret(b1);
        assertTrue(UTF_8.newEncoder().encode(CharBuffer.wrap("bär"), b1, true).isUnderflow());
        final ByteBuffer b2 = key.getSecret();
        assertThat(b2, is(not(b1)));
    }

    @Test
    public void testGetKeyMakesAProtectiveCopy() {
        key.setSecret(UTF_8.encode("föo"));
        final ByteBuffer b1 = key.getSecret();
        final ByteBuffer b2 = key.getSecret();
        assertNotSame(b1, b2);
        assertEquals(b1, b2);
    }

    @Test
    public void testCloneMakesAProtectiveCopyOfTheKey() {
        final ByteBuffer secret = UTF_8.encode("föo");
        key.setSecret(secret); // copies byte buffer
        final K clone = key.clone();
        key.setSecret(null); // clears byte buffer
        assertThat(clone.getSecret(), is(secret));
    }

    @Test
    public void testObjectSerialization() throws Exception {
        assertEquals(key, cloneViaObjectSerialization(512,
                updateTransientProperties(key)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneViaObjectSerialization(
            final int sizeHint,
            final T original)
    throws IOException, ClassNotFoundException {
        final byte[] serialized;
        try (final ByteArrayOutputStream
                bos = new ByteArrayOutputStream(sizeHint)) {
            try (final ObjectOutputStream _ = new ObjectOutputStream(bos)) {
                _.writeObject(original);
            }
            bos.flush(); // redundant
            serialized = bos.toByteArray();
        }

        logger.trace("Serialized object to {} bytes.", serialized.length);

        try (final ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(serialized))) {
            return (T) ois.readObject();
        }
    }

    @Test
    public void testXmlSerialization() throws Exception {
        assertEquals(key, cloneViaXmlSerialization(512,
                updateTransientProperties(key)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneViaXmlSerialization(
            final int sizeHint,
            final T original)
    throws IOException {
        final byte[] serialized;
        try (final ByteArrayOutputStream
                bos = new ByteArrayOutputStream(sizeHint)) {
            try (XMLEncoder encoder = new XMLEncoder(bos)) {
                encoder.writeObject(original);
            }
            bos.flush(); // redundant
            serialized = bos.toByteArray();
        }

        logger.trace("Serialized object to {} bytes.", serialized.length);
        logger.trace("Serialized form:\n{}",
                new String(serialized, StandardCharsets.UTF_8));

        try (XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(serialized))) {
            return (T) decoder.readObject();
        }
    }

    protected K updateTransientProperties(K key) {
        key = key.clone();
        key.setSecret(byteBuffer("This secret must not get serialized!"));
        return key;
    }
}
