package me.steven.indrev.mixin;

import me.steven.indrev.api.CustomEnchantmentProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemPredicate.class)
public class MixinItemPredicate {
    @Shadow @Final private EnchantmentPredicate[] enchantments;

    @Inject(
            method = "test",
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/predicate/item/EnchantmentPredicate;test(Ljava/util/Map;)Z")
            ),
            at = @At(value = "RETURN"),
            cancellable = true)
    private void a(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof CustomEnchantmentProvider && enchantments.length > 0) {
            for (EnchantmentPredicate predicate : enchantments) {
                AccessorEnchantmentPredicate accessor = (AccessorEnchantmentPredicate) predicate;
                if (accessor.getLevels() != null && accessor.getLevels() != null) {
                    int level = ((CustomEnchantmentProvider) stack.getItem()).getLevel(accessor.getEnchantment(), stack);
                    if (level > -1 && accessor.getLevels().test(level)) cir.setReturnValue(true);
                }
            }
        }
    }
}
