/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.sl.IoBufferPoolLocator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.annotation.CheckForNull;
import java.io.IOException;

/**
 * A file system driver for the HTTP(S) schemes.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 *
 * @see     #newClient()
 * @author  Christian Schlichtherle
 */
public class HttpDriver extends FsDriver {

    private volatile @CheckForNull HttpClient client;

    final IoBufferPool getPool() {
        return IoBufferPoolLocator.SINGLETON.get();
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
     * {@code HttpClientBuilder.create().useSystemProperties().build()}.
     * If you need special configuration, e.g. for authentication or caching,
     * then you should override this method.
     *
     * @return A new http client.
     */
    protected HttpClient newClient() {
        return HttpClientBuilder.create().useSystemProperties().build();
    }

    /**
     * Executes the HEAD request method for the given URI.
     * Equivalent to {@code getClient().execute(entry.newHead())}.
     */
    protected HttpResponse executeHead(HttpNode entry) throws IOException {
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
    protected HttpResponse executeGet(HttpNode entry) throws IOException {
        return getClient().execute(entry.newGet());
    }

    @Override
    public FsController newController(
            final FsManager manager,
            final FsModel model,
            final @CheckForNull FsController parent) {
        assert null == parent;
        assert null == model.getParent();
        return new HttpController(this, model);
    }
}
