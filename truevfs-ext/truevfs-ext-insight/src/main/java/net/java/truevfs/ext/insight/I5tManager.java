/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import lombok.val;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.Visitor;
import net.java.truevfs.comp.jmx.JmxManager;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
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
