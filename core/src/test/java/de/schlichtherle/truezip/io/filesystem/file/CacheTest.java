package de.schlichtherle.truezip.io.filesystem.file;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.After;
import org.junit.Before;

public class CacheTest {

    private static String TEXT = "Hello World!";

    Cache<Entry> cache;

    @Before
    public final void setUp() {
        cache = newCache().configure(new Input()).configure(new Output());
    }

    protected Cache<Entry> newCache() {
        return Cache.Strategy.WRITE_THROUGH.newCache(Entry.class);
    }

    @After
    public final void tearDown() {
        cache = null;
    }

    private static class Input extends InputSocket<Entry> {
        int reads;

        @Override
        public Entry getLocalTarget() throws IOException {
            return Entry.NULL;
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InputStream newInputStream() throws IOException {
            reads++;
            return new ByteArrayInputStream(TEXT.getBytes());
        }
    }

    private static class Output extends OutputSocket<Entry> {
        ByteArrayOutputStream out;
        int writes;

        @Override
        public Entry getLocalTarget() throws IOException {
            return Entry.NULL;
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            writes++;
            return out = new ByteArrayOutputStream();
        }
    }
}
