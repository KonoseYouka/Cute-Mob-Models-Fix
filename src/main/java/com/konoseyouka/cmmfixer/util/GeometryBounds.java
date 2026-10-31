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
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return;

        minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
    }

    public ModelSize size() { return ModelSize.stableEnvelope(minX, minY, minZ, maxX, maxY, maxZ); }
}
