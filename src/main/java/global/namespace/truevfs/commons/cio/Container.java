/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.cio;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * An auto-closeable container for entries.
 * Mind you that you need to call {@link #close()} on this stream after use, best with a {@code try}-with-resources
 * statement, like so:
 * <pre>{@code
 * try (Container<Entry> container = ...) {
 *     // Use container here...
 * }
 * }</pre>
 * The {@code try}-with-resources statement ensures that the container gets {@code close()}d after the {@code for}-loop,
 * even if it terminates with an exception.
 *
 * @param <E> the type of the entries in this container.
 * @author Christian Schlichtherle
 */
public interface Container<E extends Entry> extends AutoCloseable {

    /**
     * Returns an iterable view of all entries in this container.
     * <p>
     * First, the returned iteration <em>must</em> be consistent:
     * Multiple iterators must iterate the same entries in the same order again unless the set of entries has changed.
     * <p>
     * Next, the iteration <em>should</em> also reflect the natural order of the entries in this container:
     * For example, if this container represents an archive file, the iteration should reflect the natural order of the
     * entries in the archive file.
     *
     * @return An iterable returning a new iterator for all entries in this container.
     */
    Collection<E> entries() throws IOException;

    /**
     * Returns the entry for the given {@code name} or empty if no entry with this name exists in this container.
     *
     * @param name the name of the entry.
     */
    Optional<E> entry(String name) throws IOException;

    /**
     * Closes this container.
     * It is an error to call any other method on this container once this method has terminated without an exception
     * and the result of any violation is undefined.
     * Implementations should throw an exception from all other methods to indicate this error condition.
     */
    @Override
    void close() throws IOException;
}
