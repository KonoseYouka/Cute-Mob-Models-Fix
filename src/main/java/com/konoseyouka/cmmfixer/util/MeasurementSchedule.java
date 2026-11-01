package com.konoseyouka.cmmfixer.util;

import java.util.HashMap;
import java.util.Map;

/** キーとスナップショットは1:1。無効なレンダリング試行の制限、Tickレート制限あり。 */
public final class MeasurementSchedule {
    private static final class Entry {
        int tick, attempts, samples;
        boolean accepted;
    }

    private final Map<String,Entry> entries = new HashMap<>();

    public boolean begin(String key,int tick) {
        Entry entry=entries.get(key);
        if (entry != null
                && (entry.accepted || entry.attempts >= 20 || tick - entry.tick < 5))
            return false;

        if (entry == null) {
            entry=new Entry();
            entries.put(key,entry);
        }

        entry.tick = tick;
        entry.attempts++;

        return true;
    }

    public boolean accept(String key) {
        Entry entry = entries.get(key);
        entry.accepted = ++entry.samples >= 3;
        return entry.accepted;
    }

    public boolean exhausted(String key) {
        Entry e = entries.get(key);
        return e != null && e.attempts >= 20 && !e.accepted;
    }

    public void forget(String prefix) { entries.keySet().removeIf(key -> key.startsWith(prefix)); }
    public void reset() { entries.clear(); }
}
