package com.konoseyouka.cmmfixer.mixin;

import com.konoseyouka.cmmfixer.pack.PackBootstrap;
import com.konoseyouka.cmmfixer.pack.PackMerger;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @Shadow
    public abstract Pack getPack(String id);

    @Inject(
            method = "discoverAvailable",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cmmfix$hideDuplicate(CallbackInfoReturnable<Map<String,Pack>> cir) {
        if (!cir.getReturnValue().containsKey(PackBootstrap.PACK_ID)) return;

        Map<String,Pack> packs = new TreeMap<>(cir.getReturnValue());
        packs.remove("file/" + PackMerger.OUTPUT);

        cir.setReturnValue(Collections.unmodifiableMap(packs));
    }

    @Inject(
            method = "rebuildSelected",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cmmfix$top(Collection<String> ids,CallbackInfoReturnable<List<Pack>> cir) {
        Pack required = getPack(PackBootstrap.PACK_ID);
        if (required == null) return; // サーバーサイド

        List<Pack> packs = new ArrayList<>(cir.getReturnValue());
        packs.removeIf(p -> p.getId().equals(PackBootstrap.PACK_ID));
        packs.add(required); // ResourceManagerは最後のpackに最高の優先順位をつける。

        cir.setReturnValue(List.copyOf(packs));
    }

    @Inject(
            method = "openAllSelected",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cmmfix$resourcePriority(CallbackInfoReturnable<List<net.minecraft.server.packs.PackResources>> cir) {
        var packs = new ArrayList<>(cir.getReturnValue());
        var required = packs.stream()
                .filter(p -> p.packId().equals(PackBootstrap.PACK_ID))
                .findFirst();

        if (required.isPresent()) {
            packs.remove(required.get());
            packs.add(required.get());
            cir.setReturnValue(List.copyOf(packs));
        }
    }

    @Inject(
            method = "removePack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cmmfix$required(String id,CallbackInfoReturnable<Boolean> cir) {
        if (id.equals(PackBootstrap.PACK_ID)) cir.setReturnValue(false);
    }
}
