package com.konoseyouka.cmmfixer.util;

public record ModelSize(
        float width, float height,
        float minX, float minY, float minZ,
        float maxX, float maxY, float maxZ
) {
    public ModelSize(float width, float height) {
        this(
                width, height,
                -width / 2, 0, -width / 2,
                width / 2, height, width / 2);
    }

    public boolean valid() {
        return Float.isFinite(width) && Float.isFinite(height)
                && width >= 0.05f && height >= 0.05f
                && width <= 128 && height <= 128
                && finiteBound(minX) && finiteBound(minY) && finiteBound(minZ)
                && finiteBound(maxX) && finiteBound(maxY) && finiteBound(maxZ)
                && minX <= maxX && minY <= maxY && minZ <= maxZ;
    }

    private static boolean finiteBound(float n) { return Float.isFinite(n) && Math.abs(n) <= 128; }

    public ModelSize scale(float n) {
        return new ModelSize(
                width*n, height*n,
                minX*n, minY*n, minZ*n,
                maxX*n, maxY*n, maxZ*n);
    }

    public static ModelSize stableEnvelope(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        double height = maxY - Math.min(0, minY);
        double visualWidth = Math.max(maxX - minX, maxZ - minZ);
        double width = Math.min(visualWidth, height * 0.45);

        ModelSize size = new ModelSize((float) (Math.ceil(width * 16) / 16), (float) (Math.ceil(height * 16) / 16));

        return size.valid() ? size : null;
    }

    public static ModelSize enclosing(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        double radius = Math.max(
                Math.max(Math.abs(minX), Math.abs(maxX)),
                Math.max(Math.abs(minZ), Math.abs(maxZ)));

        ModelSize size = new ModelSize(
                (float) (Math.ceil(radius * 32) / 16), (float) (Math.ceil(Math.max(maxY, maxY - minY) * 16) / 16),
                (float) minX, (float) minY, (float) minZ,
                (float) maxX, (float) maxY, (float) maxZ);

        return size.valid() ? size : null;
    }
}
