package com.konoseyouka.cmmfixer.util;

public record DimensionProfile(
        String entityId,
        ModelSize size,
        float babyScale
) {
    public static DimensionProfile parse(String text) {
        try {
            String[] p = text.split(",",-1);
            if ((p.length!=4 && p.length!=10)
                    || !p[0].matches("[a-z0-9_.-]+:[a-z0-9_./-]+(?:#baby|#size[0-9]+)?"))
                return null;

            float w = Float.parseFloat(p[1]), h = Float.parseFloat(p[2]), s = Float.parseFloat(p[3]);
            ModelSize size = p.length == 4
                    ? new ModelSize(w, h)
                    : new ModelSize(w, h,
                Float.parseFloat(p[4]), Float.parseFloat(p[5]), Float.parseFloat(p[6]),
                Float.parseFloat(p[7]), Float.parseFloat(p[8]), Float.parseFloat(p[9]));

            if (!size.valid()
                    || !Float.isFinite(s)
                    || s <= 0
                    || s > 4)
                return null;

            return new DimensionProfile(p[0],size,s);
        }
        catch (RuntimeException ex) { return null; }
    }
}
