package com.konoseyouka.cmmfixer.util;

import java.util.function.Predicate;

public final class GrowthGuard {
    private ModelSize requested, applied;
    private boolean waiting;
    private int nextCheck;

    public ModelSize resolve(ModelSize desired, ModelSize vanilla, ModelSize current,
                             int tick, Predicate<ModelSize> fits) {
        if (desired.equals(requested) && (!waiting || tick < nextCheck)) return applied;
        requested = desired;
        // 天井や壁に向かって拡張しない。テレポートもしない。
        waiting = !fits.test(desired);
        // バニラの寸法が、いま使用中のボックスより小さいとは限らない。
        // ボックスが適合しない場合の代替サイズは、両方のボックスの共通部分とする。
        applied = waiting
                ? HitboxRules.capToVanilla(desired,
                    Math.min(vanilla.width(), current.width()),
                    Math.min(vanilla.height(), current.height()))
                : desired;
        nextCheck = tick + 10;
        return applied;
    }
}
