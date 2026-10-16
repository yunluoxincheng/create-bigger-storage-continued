package uwu.lopyluna.create_bs.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import uwu.lopyluna.create_bs.content.vault.PackagerScanControl;
import uwu.lopyluna.create_bs.content.vault.PackagerScanControl.RequestScope;

import java.util.List;

@Mixin(value = LogisticsManager.class, remap = false)
public abstract class PackagerRequestScopeMixin {
    @WrapOperation(method = "performPackageRequests",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/PackagerBlockEntity;attemptToSend(Ljava/util/List;)V"),
        require = 1, allow = 1)
    private static void create_bs$scopePackageScan(
        PackagerBlockEntity owner, List<PackagingRequest> requests, Operation<Void> original,
        @Share("create_bs$requestScope") LocalRef<RequestScope> checkedScope
    ) {
        RequestScope scope = checkedScope.get();
        if (scope == null || !scope.matches(owner, requests)) {
            // Classify the original queue once, before any context-carrying entry is removed.
            scope = PackagerScanControl.prepare(owner, requests);
            checkedScope.set(scope);
        }
        RequestScope previous = PackagerScanControl.enter(scope);
        try {
            original.call(owner, requests);
        } finally {
            PackagerScanControl.restore(previous);
        }
    }
}
