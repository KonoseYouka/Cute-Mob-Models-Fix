package com.konoseyouka.cmmfixer.util;

import net.minecraft.world.phys.AABB;

public final class CollisionBox {
    public static AABB constrain(AABB box,ModelSize size) {
        if (size == null) return box;

        if (Math.abs(box.getXsize() - size.width()) < 0.001
                && Math.abs(box.getYsize() - size.height()) < 0.001
                && Math.abs(box.getZsize() - size.width()) < 0.001)
            return box;

        double x = (box.minX + box.maxX) / 2, z = (box.minZ + box.maxZ) / 2, w = size.width() / 2;

        return new AABB(
                x - w,
                box.minY,
                z - w,
                x + w,
                box.minY + size.height(),
                z + w
        );
    }

    private CollisionBox() {}
}
