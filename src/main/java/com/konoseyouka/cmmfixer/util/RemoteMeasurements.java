package com.konoseyouka.cmmfixer.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** サーバーキャッシュ。再測定まで、最初に完了した測定結果を保持。 */
public final class RemoteMeasurements {
    private record Sample(String key, int generation, ModelSize size) {}
    private final Map<UUID, Sample> samples = new HashMap<>();

    public boolean accept(UUID id, String key, int generation, ModelSize size) {
        if (size == null || !size.valid()) return false;

        Sample previous = samples.get(id);
        if (previous != null
                && previous.key.equals(key)
                && previous.generation == generation)
            return false;

        samples.put(id, new Sample(key, generation, size));
        return true;
    }

    public ModelSize get(UUID id, String key) {
        Sample sample = samples.get(id);

        return sample != null && sample.key.equals(key)
                ? sample.size
                : null;
    }

    public void remove(UUID id) { samples.remove(id); }
    public void clear() { samples.clear(); }
}
