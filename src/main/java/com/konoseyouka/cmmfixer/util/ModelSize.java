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

    /** 中心と高さを保ちながら底面を縮小。 */
    public ModelSize compactFootprint() {
        float factor = 2.0f / 3.0f; // 目測ベース
        float centerX = (minX + maxX) / 2, centerZ = (minZ + maxZ) / 2;
        float halfX = (maxX - minX) * factor / 2, halfZ = (maxZ - minZ) * factor / 2;

        return new ModelSize(
                Math.max(0.05f, width * factor), height,
                centerX - halfX, minY, centerZ - halfZ,
                centerX + halfX, maxY, centerZ + halfZ);
    }

    public static ModelSize stableEnvelope(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        // 足元は固定。頭が入るようにする。
        double radius = Math.max(
                Math.max(Math.abs(minX), Math.abs(maxX)),
                Math.max(Math.abs(minZ), Math.abs(maxZ)));

        ModelSize size = new ModelSize(roundUp(radius * 2), roundUp(Math.max(maxY, maxY - minY)));

        return size.valid() ? size : null;
    }

    private static float roundUp(double value) { return (float) (Math.ceil(value * 16) / 16); }

    /** 浮いているモデルは、垂直方向のオフセット・ローカル境界を保持。ドラゴンも使用。 */
    public static ModelSize floatingEnvelope(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        double radius = Math.max(
                Math.max(Math.abs(minX), Math.abs(maxX)),
                Math.max(Math.abs(minZ), Math.abs(maxZ)));

        ModelSize size = new ModelSize(
                roundUp(radius * 2), roundUp(maxY - minY),
                (float) minX, (float) minY, (float) minZ,
                (float) maxX, (float) maxY, (float) maxZ);

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
