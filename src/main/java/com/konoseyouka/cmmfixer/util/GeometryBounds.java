package com.konoseyouka.cmmfixer.util;

public final class GeometryBounds {
    private double
            minX = Double.POSITIVE_INFINITY,
            minY = Double.POSITIVE_INFINITY,
            minZ = Double.POSITIVE_INFINITY;

    private double
            maxX = Double.NEGATIVE_INFINITY,
            maxY = Double.NEGATIVE_INFINITY,
            maxZ = Double.NEGATIVE_INFINITY;

    public void include(float x, float y, float z) {
        // CMMは、未使用の亜種を10万ピクセル離れた位置に移動させるらしい。
        // 測定結果全体が損なわれるのを防ぐため、それらの頂点を除外。
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
                || Math.abs(x) > 128 || Math.abs(y) > 128 || Math.abs(z) > 128) return;

        minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
    }

    public ModelSize size() { return size(false); }

    public ModelSize size(boolean floating) {
        return floating
                ? ModelSize.floatingEnvelope(minX, minY, minZ, maxX, maxY, maxZ)
                : ModelSize.stableEnvelope(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
