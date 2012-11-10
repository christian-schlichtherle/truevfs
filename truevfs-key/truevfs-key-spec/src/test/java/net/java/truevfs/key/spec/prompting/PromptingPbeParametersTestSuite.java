package net.java.truevfs.key.spec.prompting;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import net.java.truevfs.key.spec.safe.KeyStrength;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
public abstract class PromptingPbeParametersTestSuite<
        P extends PromptingPbeParameters<P, S>,
        S extends KeyStrength> {

    private static final Logger logger = LoggerFactory.getLogger(
            PromptingPbeParametersTestSuite.class);

    protected abstract P newParam();

    private P createParam() {
        final P param = newParam();
        param.setChangeRequested(true);
        final S[] strengths = param.getAllKeyStrengths();
        param.setKeyStrength(strengths[strengths.length - 1]);
        // param.setPassword("töp secret".toCharArray()); // transient!
        return param;
    }

    @Test
    public void testObjectSerialization() throws Exception {
        final P original = createParam();
        original.setPassword("föo".toCharArray());
        final P clone  = cloneViaObjectSerialization(512, original);
        original.setPassword(null);
        assertEquals(original, clone);
    }

    private static <T> T cloneViaObjectSerialization(
            final int sizeHint,
            final T original)
    throws IOException, ClassNotFoundException {
        final byte[] serialized;
        try (final ByteArrayOutputStream
                bos = new ByteArrayOutputStream(sizeHint)) {
            try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(original);
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
        final P original = createParam();
        original.setPassword("föo".toCharArray());
        final P clone  = cloneViaXmlSerialization(512, original);
        original.setPassword(null);
        assertEquals(original, clone);
    }

    private static <T> T cloneViaXmlSerialization(
            final int sizeHint,
            final T original)
    throws IOException {
        final byte[] serialized;
        try (final ByteArrayOutputStream
                bos = new ByteArrayOutputStream(sizeHint)) {
            try (final XMLEncoder enc = new XMLEncoder(bos)) {
                enc.writeObject(original);
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
}
