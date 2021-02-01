 /*
  * Copyright © 2005 - 2021 Schlichtherle IT Services.
  * All rights reserved. Use is subject to license terms.
  */
 package global.namespace.truevfs.kernel.impl;

 import lombok.Value;
 import lombok.val;
 import global.namespace.truevfs.comp.shed.ExceptionHandler;
 import global.namespace.truevfs.comp.shed.HashMaps;

 import java.io.Closeable;
 import java.io.IOException;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.Lock;

 /**
  * Accounts for {@link java.io.Closeable} resources.
  * <p>
  * For synchronization, each accountant uses a lock which has to be provided to its constructor.
  * In order to start accounting for a closeable resource, call {@link #startAccountingFor(Closeable)}.
  * In order to stop  accounting for a closeable resource, call {@link #stopAccountingFor(Closeable)}.
  * <p>
  * Note that you <em>must make sure</em> not to use two instances of this class which share the same lock.
  * Otherwise {@link #awaitClosingOfOtherThreadsResources(long)} will not work as designed.
  *
  * @author Christian Schlichtherle
  * @see ResourceController
  */
 final class ResourceAccountant implements LockAspect<Lock> {

     private final Lock lock;
     private final Condition condition;

     ResourceAccountant(final Lock lock) {
         this.lock = lock;
         this.condition = lock.newCondition();
     }

     /**
      * The map of all accounted closeable resources.
      * The initial capacity for the hash map accounts for the number of available processors, a 90% blocking factor for
      * typical I/O and a 2/3 map resize threshold.
      */
     private static final Map<Closeable, Account> accounts;

     static {
         val threads = Runtime.getRuntime().availableProcessors() * 10;
         val initialCapacity = HashMaps.initialCapacity(threads);
         accounts = new ConcurrentHashMap<>(initialCapacity, 0.75f, threads);
     }

     @Value
     private static class Account {

         Thread owner = Thread.currentThread();
         ResourceAccountant accountant;
     }

     @Value
     static class Resources {

         int local, total;
         boolean needsWaiting;

         Resources(final int local, final int total) {
             this.local = local;
             this.total = total;
             needsWaiting = local < total;
         }
     }

     @Override
     public Lock lock() {
         return lock;
     }

     /**
      * Starts accounting for the given closeable resource.
      *
      * @param resource the closeable resource to start accounting for.
      */
     void startAccountingFor(Closeable resource) {
         accounts.put(resource, new Account(this));
     }

     /**
      * Stops accounting for the given closeable resource.
      * This method should be called from the implementation of the {@link Closeable#close()} method of the given
      * {@link Closeable}.
      *
      * @param resource the closeable resource to stop accounting for.
      */
     void stopAccountingFor(Closeable resource) {
         if (null != accounts.remove(resource)) {
             locked(new Op<Object, RuntimeException>() {

                 @Override
                 public Object call() {
                     condition.signalAll();
                     return null;
                 }
             });
         }
     }

     /**
      * Waits until all closeable resources which have been started accounting for by <em>other</em> threads get stopped
      * accounting for or a timeout occurs or the current thread gets interrupted, whatever happens first.
      * <p>
      * Waiting for such resources can get cancelled immediately by interrupting the current thread.
      * Unless the number of closeable resources which have been accounted for by <em>all</em> threads is zero, this
      * will leave the interrupt status of the current thread cleared.
      * If no such foreign resources exist, then interrupting the current thread does not have any effect.
      * <p>
      * Upon return of this method, threads may immediately start accounting for closeable resources again unless the
      * caller has acquired the lock provided to the constructor - use with care!
      * <p>
      * Note that this method <strong>will not work</strong> if any two instances of this class share the same lock
      * provided to their constructor!
      *
      * @param timeout the number of milliseconds to await the closing of resources which have been accounted for by
      *                <em>other</em> threads once the lock has been acquired.
      *                If this is non-positive, then there is no timeout for waiting.
      */
     void awaitClosingOfOtherThreadsResources(long timeout) {
         locked(new Op<Void, RuntimeException>() {

             @Override
             public Void call() {
                 try {
                     if (0 < timeout) {
                         long toWait = TimeUnit.MILLISECONDS.toNanos(timeout);
                         while (0 < toWait && resources().isNeedsWaiting()) {
                             toWait = condition.awaitNanos(toWait);
                         }
                     } else {
                         while (resources().isNeedsWaiting()) {
                             condition.await();
                         }
                     }
                 } catch (final InterruptedException e) {
                     // Fix rare racing condition between Thread.interrupt() and
                     // Condition.signalAll() events.
                     if (0 == resources().getTotal()) {
                         Thread.currentThread().interrupt();
                     }
                 }
                 return null;
             }
         });
     }

     /**
      * Returns the number of closeable resources which have been accounted for.
      * The first element contains the number of closeable resources which have been created by the current thread
      * ({@code local}).
      * The second element contains the number of closeable resources which have been created by all threads
      * ({@code total}).
      * Mind that this value may reduce concurrently, even while the lock is held, so it should <em>not</em> get cached!
      *
      * @return The number of closeable resources which have been accounted for.
      */
     Resources resources() {
         val currentThread = Thread.currentThread();
         int local = 0;
         int total = 0;
         for (final Account account : accounts.values()) {
             if (this.equals(account.getAccountant())) {
                 if (currentThread.equals(account.getOwner())) {
                     local += 1;
                 }
                 total += 1;
             }
         }
         return new Resources(local, total);
     }

     /**
      * For each accounted closeable resource, stops accounting for it and closes it.
      * Upon return of this method, threads may immediately start accounting for closeable resources again unless the
      * caller also locks the lock provided to the constructor - use with care!
      */
     <X extends Exception> void closeAllResources(final ExceptionHandler<? super IOException, X> handler) throws X {
         assert null != handler;
         lock.lock();
         try {
             for (final Map.Entry<Closeable, Account> entry : accounts.entrySet()) {
                 val account = entry.getValue();
                 if (this.equals(account.getAccountant())) {
                     val closeable = entry.getKey();
                     accounts.remove(closeable);
                     try {
                         // This should trigger an attempt to remove the closeable from the
                         // map, but it can cause no ConcurrentModificationException because
                         // the entry is already removed and a ConcurrentHashMap doesn't do
                         // that anyway.
                         closeable.close();
                     } catch (IOException e) {
                         handler.warn(e); // may throw an exception!
                     }
                 }
             }
         } finally {
             try {
                 condition.signalAll();
             } finally {
                 lock.unlock();
             }
         }
     }

 }
