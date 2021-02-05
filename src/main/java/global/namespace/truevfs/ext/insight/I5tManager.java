/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.commons.jmx.JmxManager;
import global.namespace.truevfs.commons.shed.Filter;
import global.namespace.truevfs.commons.shed.Visitor;
import global.namespace.truevfs.kernel.api.FsController;
import global.namespace.truevfs.kernel.api.FsManager;
import lombok.val;

/**
 * @author Christian Schlichtherle
 */
final class I5tManager extends JmxManager<I5tMediator> {

    I5tManager(I5tMediator mediator, FsManager manager) {
        super(mediator, manager);
    }

    @Override
    public void activate() {
        super.activate();
        mediator.activateAllStats(this);
    }

    @Override
    public <X extends Exception, V extends Visitor<? super FsController, X>> V accept(final Filter<? super FsController> filter, final V visitor) throws X {
        new Object() {
            final long start = System.nanoTime();

            boolean allUnmounted = true;

            {
                manager.<X, Visitor<FsController, X>>accept(
                        controller -> {
                            val accepted = filter.accept(controller);
                            allUnmounted &= accepted;
                            return accepted;
                        },
                        controller -> {
                            try {
                                visitor.visit(controller);
                            } finally {
                                allUnmounted &= !controller.getModel().isMounted();
                            }
                        }
                );
                if (allUnmounted) {
                    mediator.logSync(System.nanoTime() - start);
                    mediator.rotateAllStats(I5tManager.this);
                }
            }
        };
        return visitor;
    }
}
