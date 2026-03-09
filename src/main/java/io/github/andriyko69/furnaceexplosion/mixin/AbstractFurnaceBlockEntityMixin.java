package io.github.andriyko69.furnaceexplosion.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Shadow
    protected NonNullList<ItemStack> items;

    @Shadow
    int litTime;
    @Shadow
    int litDuration;
    @Shadow
    int cookingProgress;
    @Shadow
    int cookingTotalTime;

    @Shadow
    protected abstract int getBurnDuration(ItemStack fuel);

    @Shadow
    private boolean isLit() {
        throw new AssertionError();
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void furnaceExplosion$handleTntSmelling(
            Level level,
            BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfo ci
    ) {
        AbstractFurnaceBlockEntityMixin self = (AbstractFurnaceBlockEntityMixin) (Object) furnace;

        ItemStack input = self.items.get(0);
        ItemStack fuel = self.items.get(1);

        if (!input.is(Items.TNT) || !(state.getBlock() instanceof FurnaceBlock)) {
            return;
        }

        boolean wasLit = self.isLit();
        boolean changed = false;

        if (self.isLit()) {
            self.litTime--;
        }

        if (self.cookingTotalTime <= 0) {
            self.cookingTotalTime = 200;
        }

        boolean hasFuel = !fuel.isEmpty();

        if (!self.isLit() && hasFuel) {
            self.litTime = self.getBurnDuration(fuel);
            self.litDuration = self.litTime;

            if (self.isLit()) {
                changed = true;

                if (fuel.hasCraftingRemainingItem()) {
                    self.items.set(1, fuel.getCraftingRemainingItem());
                } else {
                    fuel.shrink(1);
                    if (fuel.isEmpty()) {
                        self.items.set(1, ItemStack.EMPTY);
                    }
                }
            }
        }

        if (self.isLit()) {
            self.cookingProgress++;

            if (self.cookingProgress >= self.cookingTotalTime - 1) {
                self.items.set(0, ItemStack.EMPTY);
                self.items.set(1, ItemStack.EMPTY);
                self.items.set(2, ItemStack.EMPTY);

                level.explode(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        4.0F,
                        Level.ExplosionInteraction.BLOCK
                );

                ci.cancel();
                return;
            }

            changed = true;
        } else {
            if (self.cookingProgress > 0) {
                self.cookingProgress = Math.max(self.cookingProgress - 2, 0);
                changed = true;
            }
        }

        if (wasLit != self.isLit()) {
            state = state.setValue(AbstractFurnaceBlock.LIT, self.isLit());
            level.setBlock(pos, state, 3);
            changed = true;
        }

        if (changed) {
            furnace.setChanged();
        }

        ci.cancel();
    }

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void furnaceExplosion$allowTntInInput(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (furnaceExplosion$acceptTnt(slot, stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
    private void furnaceExplosion$allowTntThroughFace(int slot, ItemStack stack, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (furnaceExplosion$acceptTnt(slot, stack)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean furnaceExplosion$acceptTnt(int slot, ItemStack stack) {
        AbstractFurnaceBlockEntity self = (AbstractFurnaceBlockEntity) (Object) this;
        return (self instanceof FurnaceBlockEntity) && slot == 0 && stack.is(Items.TNT);
    }
}