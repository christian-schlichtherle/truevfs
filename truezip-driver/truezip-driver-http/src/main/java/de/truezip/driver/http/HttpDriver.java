/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.http;

import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.cio.IOPoolProvider;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsDriver;
import de.truezip.kernel.FsManager;
import de.truezip.kernel.FsModel;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

/**
 * A file system driver for the HTTP(S) schemes.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @see     #newClient()
 * @author  Christian Schlichtherle
 */
@Immutable
public class HttpDriver extends FsDriver {

    private final IOPoolProvider provider;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
    private volatile @CheckForNull HttpClient client;

    public HttpDriver(final IOPoolProvider provider) {
        if (null == provider)
            throw new NullPointerException();
        this.provider = provider;
    }

    final IOPool<?> getPool() {
        return provider.getIOPool();
    }

    /**
     * Returns the cached http client obtained by calling {@link #newClient()}.
     * 
     * @return The cached http client obtained by calling {@link #newClient()}.
     */
    protected final HttpClient getClient() {
        final HttpClient client = this.client;
        return null != client ? client : (this.client = newClient());
    }

    /**
     * Returns a new http client.
     * <p>
     * The implementation in the class {@link HttpDriver} simply returns
     * {@code new DefaultHttpClient(new ThreadSafeClientConnManager())}.
     * If you need special configuration, e.g. for authentication or caching,
     * then you should override this method.
     * 
     * @return A new http client.
     */
    protected HttpClient newClient() {
        return new DefaultHttpClient(new ThreadSafeClientConnManager());
    }

    /**
     * Executes the HEAD request method for the given URI.
     * Equivalent to {@code getClient().execute(entry.newHead())}.
     */
    protected HttpResponse executeHead(HttpEntry entry) throws IOException {
        // This version could be better when using a CachingHttpDriver:
        /*HttpUriRequest request = entry.newGet();
        HttpResponse response = getClient().execute(request);
        request.abort();
        return response;*/

        return getClient().execute(entry.newHead());
    }

    /**
     * Executes the GET request method for the given URI.
     * Equivalent to {@code getClient().execute(entry.newGet())}.
     */
    protected HttpResponse executeGet(HttpEntry entry) throws IOException {
        return getClient().execute(entry.newGet());
    }

    @Override
    public FsController<?>
    controller(  final FsManager manager,
                    final FsModel model,
                    final @CheckForNull FsController<?> parent) {
        assert null == parent;
        return new HttpController(this, model);
    }
}