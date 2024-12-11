package com.epam.task1.impl;

import java.util.*;

public class CustomSyncThreadSafeMap<K,V> implements Map<K, V> {

    private final Map<K,V> m;
    final Object      mutex;
    private int mod;

    public CustomSyncThreadSafeMap() {
        this.m = new HashMap<K, V>();
        this.mutex = this;
    }

    public int size() {
        synchronized (mutex) {return m.size();}
    }
    public boolean isEmpty() {
        synchronized (mutex) {return m.isEmpty();}
    }
    public boolean containsKey(Object key) {
        synchronized (mutex) {return m.containsKey(key);}
    }
    public boolean containsValue(Object value) {
        synchronized (mutex) {return m.containsValue(value);}
    }
    public V get(Object key) {
        synchronized (mutex) {return m.get(key);}
    }

    public V put(K key, V value) {
        synchronized (mutex) {
            mod = MOD;
            return m.put(key, value);
        }
    }
    public V remove(Object key) {
        synchronized (mutex) {
            mod = MOD;
            return m.remove(key);
        }
    }
    public void putAll(Map<? extends K, ? extends V> map) {
        synchronized (mutex) {
            m.putAll(map);
            mod = MOD;
        }
    }
    public void clear() {
        synchronized (mutex) {
            m.clear();
            mod = MOD;
        }
    }

    private static final int KEY_SET_MOD = 1 << 0;
    private transient Set<K> keySet;
    private static final int ENTRY_SET_MOD = 1 << 1;
    private transient Set<Map.Entry<K,V>> entrySet;
    private transient Collection<V> values;
    private static final int VALS_MOD = 1 << 2;
    private static final int MOD = KEY_SET_MOD | ENTRY_SET_MOD | VALS_MOD;



    public Set<K> keySet() {
        synchronized (mutex) {
            if (keySet==null || (mod & KEY_SET_MOD) == KEY_SET_MOD) {
                keySet = new HashSet<K>(m.keySet());
                mod &= ~(KEY_SET_MOD);
            }
            return keySet;
        }
    }

    public Set<Map.Entry<K,V>> entrySet() {
        synchronized (mutex) {
            if (entrySet==null || (mod & ENTRY_SET_MOD) == ENTRY_SET_MOD) {
                entrySet = new HashSet<Map.Entry<K, V>>(m.entrySet());
                mod &= ~(ENTRY_SET_MOD);
            }
            return entrySet;
        }
    }

    public Collection<V> values() {
        synchronized (mutex) {
            if (values==null || (mod & VALS_MOD) == VALS_MOD) {
                values = new ArrayList<V>(m.values());
                mod &= ~(VALS_MOD);
            }
            return values;
        }
    }

}
