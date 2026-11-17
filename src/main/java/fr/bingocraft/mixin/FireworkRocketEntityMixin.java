package fr.bingocraft.mixin;

import fr.bingocraft.util.Feedback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Celebration rockets (tagged {@link Feedback#HARMLESS_TAG}) skip the explosion damage.
 * The visual burst is a separate entity status sent before {@code explode()}, so it still shows.
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void bingocraft$harmless(CallbackInfo ci) {
        if (((Entity) (Object) this).getCommandTags().contains(Feedback.HARMLESS_TAG)) {
            ci.cancel();
        }
    }
}
