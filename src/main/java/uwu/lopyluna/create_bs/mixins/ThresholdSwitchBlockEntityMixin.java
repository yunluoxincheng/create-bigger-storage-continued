package uwu.lopyluna.create_bs.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.compat.thresholdSwitch.ThresholdSwitchCompat;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(value = ThresholdSwitchBlockEntity.class, remap = false)
public abstract class ThresholdSwitchBlockEntityMixin {

    @WrapOperation(
        method = "updateCurrentLevel",
        at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"),
        require = 1,
        allow = 1
    )
    private Stream<ThresholdSwitchCompat> create_bs$selectCompatOnce(
        Stream<ThresholdSwitchCompat> stream,
        Predicate<? super ThresholdSwitchCompat> predicate,
        Operation<Stream<ThresholdSwitchCompat>> original,
        @Share("create_bs$compatResolved") LocalBooleanRef resolved,
        @Share("create_bs$selectedCompat") LocalRef<ThresholdSwitchCompat> selected
    ) {
        // The predicate only depends on the target block entity, not the slot.
        // Shared locals belong to this invocation, including recursive calls.
        if (!resolved.get()) {
            selected.set(original.call(stream, predicate).findFirst().orElse(null));
            resolved.set(true);
        }

        // Preserve Create's first-match order and its per-slot map/fallback.
        // A separate resolved flag also caches the ordinary-inventory no-match case.
        ThresholdSwitchCompat compat = selected.get();
        return compat == null ? Stream.empty() : Stream.of(compat);
    }
}
