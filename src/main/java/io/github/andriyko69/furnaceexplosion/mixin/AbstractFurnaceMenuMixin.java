package io.github.andriyko69.furnaceexplosion.mixin;

import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceMenu.class)
public class AbstractFurnaceMenuMixin {

    @Inject(method = "canSmelt", at = @At("HEAD"), cancellable = true)
    private void furnaceExplosion$canSmeltTnt(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        AbstractFurnaceMenu self = (AbstractFurnaceMenu) (Object) this;

        if (stack.is(Items.TNT) && (self instanceof FurnaceMenu)) {
            cir.setReturnValue(true);
        }
    }
}