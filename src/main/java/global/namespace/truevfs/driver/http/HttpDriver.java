/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.http;

import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.kernel.api.FsController;
import global.namespace.truevfs.kernel.api.FsDriver;
import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.FsModel;
import global.namespace.truevfs.kernel.api.sl.IoBufferPoolLocator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Optional;

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
/*
        HttpUriRequest request = entry.newGet();
        HttpResponse response = getClient().execute(request);
        request.abort();
        return response;
*/
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
            final Optional<? extends FsController> parent) {
        assert !parent.isPresent();
        assert !model.getParent().isPresent();
        return new HttpController(this, model);
    }
}
