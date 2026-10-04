package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentItemName;

/**
 * Vanilla {@code ItemStack.func_151000_E()} bakes the already-translated {@code getDisplayName()} into a
 * {@link ChatComponentText} before the component is sent to the client. Because that translation happens on the server,
 * clients end up rendering the server's language instead of their own for death messages, {@code /give} feedback,
 * statistics, etc.
 *
 * <p>
 * This mixin replaces the name text with a {@link ChatComponentItemName}, which carries the raw {@link ItemStack}. The
 * stack is serialized as-is and only resolved via {@code getDisplayName()} on the client, using the client's locale.
 */
@Mixin(ItemStack.class)
public class MixinItemStack_GetChatComponent {

    @Inject(method = "func_151000_E", at = @At("HEAD"), cancellable = true)
    private void gtnhlib$localizeItemNameOnClient(CallbackInfoReturnable<IChatComponent> cir) {
        ItemStack stack = (ItemStack) (Object) this;

        // ChatComponentItemName already renders as "[name]" (with brackets) like vanilla func_151000_E().
        IChatComponent ichatcomponent = new ChatComponentItemName(stack);

        if (stack.getItem() != null) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            stack.writeToNBT(nbttagcompound);
            ichatcomponent.getChatStyle().setChatHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ChatComponentText(nbttagcompound.toString())));
            ichatcomponent.getChatStyle().setColor(stack.getRarity().rarityColor);
        }

        cir.setReturnValue(ichatcomponent);
    }
}
