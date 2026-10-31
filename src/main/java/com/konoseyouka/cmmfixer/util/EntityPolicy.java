package com.konoseyouka.cmmfixer.util;

import com.konoseyouka.cmmfixer.CMMFix;
import com.konoseyouka.cmmfixer.CMMFixConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import java.lang.reflect.Method;
import java.util.Optional;

public final class EntityPolicy {
    private static final String HOLDER = "com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantDataHolder";
    private static final ClassValue<Optional<Method>> VARIANT = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                Class<?> holder = Class.forName(HOLDER, false, type.getClassLoader());
                return holder.isAssignableFrom(type)
                        ? Optional.of(holder.getMethod("getVariantData"))
                        : Optional.empty();
            }
            catch (ClassNotFoundException ex) { return Optional.empty(); }
            catch (ReflectiveOperationException ex) { throw new IllegalStateException("Unsupported Vanilla Backport variant API", ex); }
        }
    };

    private record VariantRegistry(Object registry, Method getKey) {}
    private static final ClassValue<VariantRegistry> REGISTRIES = new ClassValue<>() {
        @Override
        protected VariantRegistry computeValue(Class<?> type) {
            try {
                var field = Class.forName(type.getName() + "s", true, type.getClassLoader()).getField("REGISTRIES");
                Object registry = field.get(null);
                return new VariantRegistry(registry, field.getType().getMethod("getKey", Object.class));
            }
            catch (ReflectiveOperationException ex) {
                CMMFix.LOGGER.warn("Cannot identify Vanilla Backport variant {}; excluding its model", type.getName());
                return new VariantRegistry(null, null);
            }
        }
    };

    public static String id(Entity entity) { return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(); }
    public static boolean allowed(Entity entity) {
        String id = id(entity);
        if (!PolicyRules.entityAllowed(
                id,
                CMMFixConfig.SPEC.isLoaded()
                        ? CMMFixConfig.ALLOWED.get()
                        : java.util.List.of()))
            return false;

        var method = VARIANT.get(entity.getClass());
        if (method.isEmpty()) return true;

        try {
            Optional<?> variant = (Optional<?>) method.get().invoke(entity);
            if (variant.isEmpty()) return true;

            // temperate/paleを維持
            VariantRegistry registry = REGISTRIES.get(variant.get().getClass());
            if (registry.registry() == null) return false;

            Object key = registry.getKey().invoke(registry.registry(), variant.get());
            return PolicyRules.originalVariant(
                    id,
                    key == null
                            ? null
                            : key.toString());
        }
        catch (ReflectiveOperationException ex) {
            // 色違いがわからない
            return false;
        }
    }

    private EntityPolicy() {}
}
