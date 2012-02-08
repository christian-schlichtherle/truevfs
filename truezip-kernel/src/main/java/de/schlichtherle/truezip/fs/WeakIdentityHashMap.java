/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Like {@link java.util.WeakHashMap}, but compares keys using object identity
 * (==) instead of object equality ({@link Object#equals}).
 * <p>
 * This class is based on {@code java.util.WeakHashMap} from the
 * <a href="http://openjdk.java.net/">OpenJDK</a> project and has been
 * backported to JSE&nbsp;6.
 *
 * @see     java.util.IdentityHashMap
 * @see     java.util.WeakHashMap
 * @author  Dawid Kurzyniec
 * @author  <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author  Christian Schlichtherle (updated to match JSE 7 and backported to JSE 6)
 * @version $Id$
 */
@DefaultAnnotation(Nullable.class)
final class WeakIdentityHashMap<K,V>
/*extends AbstractMap<K,V>*/
implements Map<K,V> {

    /**
     * The default initial capacity -- MUST be a power of two.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load fast used when none specified in constructor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    private Entry<K,V>[] table;

    /**
     * The number of key-value mappings contained in this weak hash map.
     */
    private int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int threshold;

    /**
     * The load factor for the hash table.
     */
    private final float loadFactor;

    /**
     * Reference queue for cleared WeakEntries
     */
    private final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

    /**
     * The number of times this WeakIdentityHashMap has been structurally
     * modified.
     * Structural modifications are those that change the number of
     * mappings in the map or otherwise modify its internal structure
     * (e.g., rehash).
     * This field is used to make iterators on Collection-views of the map
     * fail-fast.
     */
    private int modCount;

    /**
     * Constructs a new, empty {@code WeakIdentityHashMap} with the given
     * initial capacity and the given load factor.
     *
     * @param  initialCapacity The initial capacity of the
     *                         {@code WeakIdentityHashMap}
     * @param  loadFactor      The load factor of the
     *                         {@code WeakIdentityHashMap}
     * @throws IllegalArgumentException  If the initial capacity is negative,
     *         or if the load factor is nonpositive.
     */
    WeakIdentityHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;
        table = newTable(capacity);
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
    }

    /**
     * Constructs a new, empty {@code WeakIdentityHashMap} with the given
     * initial capacity and the default load factor, which is {@code 0.75}.
     *
     * @param  initialCapacity The initial capacity of the
     *                         {@code WeakIdentityHashMap}
     * @throws IllegalArgumentException  If the initial capacity is negative.
     */
    WeakIdentityHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new, empty {@code WeakIdentityHashMap} with the default
     * initial capacity (16) and the default load factor (0.75).
     */
    WeakIdentityHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = DEFAULT_INITIAL_CAPACITY;
        table = newTable(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs a new {@code WeakIdentityHashMap} with the same mappings as
     * the specified {@code Map}.  The {@code WeakIdentityHashMap} is created
     * with default load factor, which is {@code 0.75} and an initial capacity
     * sufficient to hold the mappings in the specified {@code Map}.
     *
     * @param   m the map whose mappings are to be placed in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    WeakIdentityHashMap(final @Nonnull Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1, 16),
             DEFAULT_LOAD_FACTOR);
        putAll(m);
    }

    // internal utilities

    /**
     * Value representing null keys inside tables.
     */
    private static final Object NULL_KEY = new Object();

    /**
     * Use NULL_KEY for key if it is null.
     */
    private static Object maskNull(Object key) {
        return (key == null ? NULL_KEY : key);
    }

    /**
     * Return internal representation of null key back to caller as null
     */
    private static Object unmaskNull(Object key) {
        return (key == NULL_KEY ? null : key);
    }

    /**
     * Return index for hash code h.
     */
    private static int indexFor(int h, int length) {
        return h & (length-1);
    }

    /**
     * Expunge stale entries from the table.
     */
    private void expungeStaleEntries() {
        for (Object x; (x = queue.poll()) != null; ) {
            synchronized (queue) {
                @SuppressWarnings("unchecked")
                Entry<K,V> e = (Entry<K,V>)x;
                int i = indexFor(e.hash, table.length);

                Entry<K,V> prev = table[i];
                Entry<K,V> p = prev;
                while (p != null) {
                    Entry<K,V> next = p.next;
                    if (p == e) {
                        if (prev == e)
                            table[i] = next;
                        else
                            prev.next = next;
                        // Must not null out e.next;
                        // stale entries may be in use by a HashIterator
                        e.value = null; // Help GC
                        size--;
                        break;
                    }
                    prev = p;
                    p = next;
                }
            }
        }
    }

    /**
     * Return the table after first expunging stale entries
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Entry<K,V>[] getTable() {
        expungeStaleEntries();
        return table;
    }

    private int hash(Object x) {
        return System.identityHashCode(x);
    }

    @SuppressWarnings("unchecked")
    private Entry<K,V>[] newTable(int n) {
        return (Entry<K,V>[]) new Entry<?,?>[n];
    }

    /**
     * Returns the number of key-value mappings in this map.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    @Override
    public int size() {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if the map contains no mapping for this key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     * 
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it is also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    @Override
    public V get(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K,V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K,V> e = tab[index];
        while (e != null) {
            if (e.hash == h && k == e.get())
                return e.value;
            e = e.next;
        }
        return null;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return  {@code true} if there is a mapping for {@code key};
     *          {@code false} otherwise
     */
    @Override
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * Returns the entry associated with the specified key in the HashMap.
     * Returns null if the HashMap contains no mapping for this key.
     */
    private Entry<K,V> getEntry(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K,V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K,V> e = tab[index];
        while (e != null && !(e.hash == h && k == e.get()))
            e = e.next;
        return e;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or {@code null}
     *	       if there was no mapping for key.  A {@code null} return can
     *	       also indicate that the HashMap previously associated
     *	       {@code null} with the specified key.
     */
    @Override
    public V put(K key, V value) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K,V>[] tab = getTable();
        int i = indexFor(h, tab.length);
        
        for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
            if (h == e.hash && k == e.get()) {
                V oldValue = e.value;
                if (value != oldValue)
                    e.value = value;
                return oldValue;
            }
        }

        modCount++;
        Entry<K,V> e = tab[i];
        tab[i] = new Entry<K,V>(k, value, queue, h, e);
        if (++size >= threshold)
            resize(tab.length * 2);
        return null;
    }

    /**
     * Rehashes the contents of this map into a new {@code HashMap} instance
     * with a larger capacity. This method is called automatically when the
     * number of keys in this map exceeds its capacity and load factor.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    private void resize(int newCapacity) {
        Entry<K,V>[] oldTable = getTable();
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry<K,V>[] newTable = newTable(newCapacity);
        transfer(oldTable, newTable);
        table = newTable;

        /*
         * If ignoring null elements and processing ref queue caused massive
         * shrinkage, then restore old table.  This should be rare, but avoids
         * unbounded expansion of garbage-filled tables.
         */
        if (size >= threshold / 2) {
            threshold = (int)(newCapacity * loadFactor);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    /** Transfer all entries from src to dest tables */
    private void transfer(Entry<K,V>[] src, Entry<K,V>[] dest) {
        for (int j = 0; j < src.length; ++j) {
            Entry<K,V> e = src[j];
            src[j] = null;
            while (e != null) {
                Entry<K,V> next = e.next;
                Object key = e.get();
                if (key == null) {
                    e.next = null;  // Help GC
                    e.value = null; //  "   "
                    size--;
                } else {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map These
     * mappings will replace any mappings that this map had for any of the
     * keys currently in the specified map.<p>
     *
     * @param m mappings to be stored in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0)
            return;

        /*
         * Expand the map if the map if the number of mappings to be added
         * is greater than or equal to threshold.  This is conservative; the
         * obvious condition is (m.size() + size) >= threshold, but this
         * condition could result in a map with twice the appropriate capacity,
         * if the keys to be added overlap with the keys already in this map.
         * By using the conservative calculation, we subject ourself
         * to at most one extra resize.
         */
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * Removes the mapping for a key from this weak hash map if it is present.
     * More formally, if this map contains a mapping from key <tt>k</tt> to
     * value <tt>v</tt> such that <code>(key==null ?  k==null :
     * key.equals(k))</code>, that mapping is removed.  (The map can contain
     * at most one such mapping.)
     *
     * <p>Returns the value to which this map previously associated the key,
     * or <tt>null</tt> if the map contained no mapping for the key.  A
     * return value of <tt>null</tt> does not <i>necessarily</i> indicate
     * that the map contained no mapping for the key; it's also possible
     * that the map explicitly mapped the key to <tt>null</tt>.
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>
     */
    @Override
    public V remove(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K,V>[] tab = getTable();
        int i = indexFor(h, tab.length);
        Entry<K,V> prev = tab[i];
        Entry<K,V> e = prev;

        while (e != null) {
            Entry<K,V> next = e.next;
            if (h == e.hash && k == e.get()) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return e.value;
            }
            prev = e;
            e = next;
        }

        return null;
    }

    /** Special version of remove needed by Entry set */
    boolean removeMapping(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Entry<K,V>[] tab = getTable();
        Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
        Object k = maskNull(entry.getKey());
        int h = hash(k);
        int i = indexFor(h, tab.length);
        Entry<K,V> prev = tab[i];
        Entry<K,V> e = prev;

        while (e != null) {
            Entry<K,V> next = e.next;
            if (h == e.hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return true;
            }
            prev = e;
            e = next;
        }

        return false;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    @Override
    @SuppressWarnings("empty-statement")
    public void clear() {
        // clear out ref queue. We don't need to expunge entries
        // since table is getting cleared.
        while (queue.poll() != null)
            ;

        modCount++;
        Arrays.fill(table, null);
        size = 0;

        // Allocation of array may have caused GC, which may have caused
        // additional entries to go stale.  Removing these entries from the
        // reference queue will make them eligible for reclamation.
        while (queue.poll() != null)
            ;
   }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return {@code true} if this map maps one or more keys to the
     *         specified value.
     */
    @Override
    public boolean containsValue(Object value) {
        if (value==null)
            return containsNullValue();

        Entry<K,V> tab[] = getTable();
        for (int i = tab.length ; i-- > 0 ;)
            for (Entry<K,V> e = tab[i] ; e != null ; e = e.next)
                if (value.equals(e.value))
                    return true;
        return false;
    }

    /**
     * Special-case code for containsValue with null argument
     */
    private boolean containsNullValue() {
        Entry<K,V> tab[] = getTable();
        for (int i = tab.length ; i-- > 0 ;)
            for (Entry<K,V> e = tab[i] ; e != null ; e = e.next)
                if (e.value==null)
                    return true;
        return false;
    }

    /**
     * The entries in this hash table extend WeakReference, using its main ref
     * field as the key.
     */
    private static class Entry<K,V> extends WeakReference<Object> implements Map.Entry<K,V> {
        V value;
        final int hash;
        Entry<K,V> next;

        /**
         * Create new entry.
         */
        Entry(Object key, V value,
              ReferenceQueue<Object> queue,
              int hash, Entry<K,V> next) {
            super(key, queue);
            this.value = value;
            this.hash  = hash;
            this.next  = next;
        }

        @SuppressWarnings("unchecked")
        @Override
        public K getKey() {
            return (K) WeakIdentityHashMap.unmaskNull(this.get());
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            K k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2) {
                V v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            K k = getKey();
            V v = getValue();
            return  ((k==null ? 0 : System.identityHashCode(k)) ^
                     (v==null ? 0 : v.hashCode()));
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private abstract class HashIterator<T> implements Iterator<T> {
        int index;
        Entry<K,V> entry = null;
        Entry<K,V> lastReturned = null;
        int expectedModCount = modCount;

        /**
         * Strong reference needed to avoid disappearance of key
         * between hasNext and next
         */
        Object nextKey = null;

        /**
         * Strong reference needed to avoid disappearance of key
         * between nextEntry() and any use of the entry
         */
        Object currentKey = null;

        HashIterator() {
            index = isEmpty() ? 0 : table.length;
        }

        @Override
        public boolean hasNext() {
            Entry<K,V>[] t = table;

            while (nextKey == null) {
                Entry<K,V> e = entry;
                int i = index;
                while (e == null && i > 0)
                    e = t[--i];
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    return false;
                }
                nextKey = e.get(); // hold on to key in strong ref
                if (nextKey == null)
                    entry = entry.next;
            }
            return true;
        }

        /** The common parts of next() across different types of iterators */
        Entry<K,V> nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextKey == null && !hasNext())
                throw new NoSuchElementException();

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            WeakIdentityHashMap.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

    }

    private class ValueIterator extends HashIterator<V> {
        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends HashIterator<K> {
        @Override
        public K next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends HashIterator<Map.Entry<K,V>> {
        @Override
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    // Views

    private transient Set<Map.Entry<K,V>> entrySet = null;

    /**
     * Each of these fields are initialized to contain an instance of the
     * appropriate view the first time this view is requested.  The views are
     * stateless, so there's no reason to create more than one of each.
     */
    private transient volatile Set<K>        keySet = null;
    private transient volatile Collection<V> values = null;

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     */
    @Override
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return ks != null ? ks : (keySet = new KeySet());
    }

    private class KeySet extends AbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return WeakIdentityHashMap.this.size();
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public boolean remove(Object o) {
            if (containsKey(o)) {
                WeakIdentityHashMap.this.remove(o);
                return true;
            }
            else
                return false;
        }

        @Override
        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        private Collection<K> deepCopy() {
            final Collection<K> list = new ArrayList<K>(size());
            for (final K k : this)
                list.add(k);
            return list;
        }

        @Override
        public Object[] toArray() {
            return deepCopy().toArray();
        }

        @Override
        public <T> T[] toArray(T a[]) {
            return deepCopy().toArray(a);
        }
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     */
    @Override
    public Collection<V> values() {
        Collection<V> vs = values;
        return vs != null ?  vs : (values = new Values());
    }

    private class Values extends AbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return WeakIdentityHashMap.this.size();
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        private Collection<V> deepCopy() {
            final Collection<V> list = new ArrayList<V>(size());
            for (final V v : this)
                list.add(v);
            return list;
        }

        @Override
        public Object[] toArray() {
            return deepCopy().toArray();
        }

        @Override
        public <T> T[] toArray(T a[]) {
            return deepCopy().toArray(a);
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     */
    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        @Override
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            Entry<K,V> candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }

        @Override
        public boolean remove(Object o) {
            return removeMapping(o);
        }

        @Override
        public int size() {
            return WeakIdentityHashMap.this.size();
        }

        @Override
        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        private List<Map.Entry<K,V>> deepCopy() {
            List<Map.Entry<K,V>> list = new ArrayList<Map.Entry<K,V>>(size());
            for (Map.Entry<K,V> e : this)
                list.add(new AbstractMap.SimpleEntry<K,V>(e));
            return list;
        }

        @Override
        public Object[] toArray() {
            return deepCopy().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return deepCopy().toArray(a);
        }
    }
}
