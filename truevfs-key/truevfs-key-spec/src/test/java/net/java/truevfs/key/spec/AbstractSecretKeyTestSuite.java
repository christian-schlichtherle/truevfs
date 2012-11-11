/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import static java.nio.charset.StandardCharsets.*;
import static net.java.truevfs.key.spec.util.BufferUtils.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
public abstract class AbstractSecretKeyTestSuite<
        K extends AbstractSecretKey<K>> {

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractSecretKeyTestSuite.class);

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

    private static <T> T cloneViaXmlSerialization(
            final int sizeHint,
            final T original)
    throws IOException {
        final byte[] serialized;
        try (final ByteArrayOutputStream
                bos = new ByteArrayOutputStream(sizeHint)) {
            try (final XMLEncoder _ = new XMLEncoder(bos)) {
                _.writeObject(original);
            }
            bos.flush(); // redundant
            serialized = bos.toByteArray();
        }

        logger.trace("Serialized object to {} bytes.", serialized.length);
        logger.trace("Serialized form:\n{}",
                new String(serialized, StandardCharsets.UTF_8));

        try (final XMLDecoder
                dec = new XMLDecoder(new ByteArrayInputStream(serialized))) {
            return (T) dec.readObject();
        }
    }

    protected K updateTransientProperties(K key) {
        key = key.clone();
        key.setSecret(byteBuffer("This secret must not get serialized!"));
        return key;
    }
}
