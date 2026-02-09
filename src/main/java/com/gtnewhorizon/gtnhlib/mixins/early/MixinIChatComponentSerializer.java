package com.gtnewhorizon.gtnhlib.mixins.early;

import java.lang.reflect.Type;

import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.gson.*;
import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;
import com.gtnewhorizon.gtnhlib.chat.ChatComponentCustomRegistry;

@Mixin(IChatComponent.Serializer.class)
public abstract class MixinIChatComponentSerializer {

    @Unique
    private static final String GTNHLIB_ID = "gtnhlib_id";
    @Unique
    private static final String GTNHLIB_PAYLOAD = "gtnhlib_payload";

    // Vanilla helper that flattens ChatStyle fields onto the component JsonObject
    @Shadow
    private void func_150695_a(ChatStyle style, JsonObject object, JsonSerializationContext ctx) {}

    @Inject(
            method = "serialize(Lnet/minecraft/util/IChatComponent;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;",
            at = @At("HEAD"),
            cancellable = true)
    private void gtnhlib$serializeCustom(IChatComponent component, Type type, JsonSerializationContext ctx,
            CallbackInfoReturnable<JsonElement> cir) {
        if (!(component instanceof AbstractChatComponentCustom custom)) {
            return; // Leave vanilla to handle others stuff, e.g. ChatComponentText/Translation.
        }

        JsonObject root = new JsonObject();

        // Custom tag + payload
        root.addProperty(GTNHLIB_ID, custom.getID());
        root.add(GTNHLIB_PAYLOAD, custom.serialize());

        // vanilla style flattening
        if (!component.getChatStyle().isEmpty()) {
            this.func_150695_a(component.getChatStyle(), root, ctx);
        }

        // Vanilla siblings stored under ("extra")
        if (!component.getSiblings().isEmpty()) {
            JsonArray extra = new JsonArray();
            for (IChatComponent sib : component.getSiblings()) {
                // Use gson to serialize siblings so they can themselves be custom or vanilla
                extra.add(ctx.serialize(sib, IChatComponent.class));
            }
            root.add("extra", extra);
        }

        cir.setReturnValue(root);
    }

    @Inject(
            method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/util/IChatComponent;",
            at = @At("HEAD"),
            cancellable = true)
    private void gtnhlib$deserializeCustom(JsonElement element, Type type, JsonDeserializationContext ctx,
            CallbackInfoReturnable<IChatComponent> cir) {
        if (!element.isJsonObject()) return;

        JsonObject obj = element.getAsJsonObject();
        if (!obj.has(GTNHLIB_ID)) return; // Not a custom registration, let vanilla handle it.

        JsonElement idEl = obj.get(GTNHLIB_ID);
        if (idEl == null || !idEl.isJsonPrimitive()) {
            throw new JsonParseException("gtnhlib_id must be a JSON string");
        }

        String id = idEl.getAsString();
        AbstractChatComponentCustom chatComponent = ChatComponentCustomRegistry.get(id);

        JsonElement payload = obj.get(GTNHLIB_PAYLOAD);
        if (payload == null) {
            throw new JsonParseException("Missing gtnhlib_payload for id: " + id);
        }

        // Fill the custom component.
        chatComponent.deserialize(payload);

        // Vanilla siblings
        if (obj.has("extra")) {
            JsonArray extra = obj.getAsJsonArray("extra");
            if (extra.size() <= 0) {
                throw new JsonParseException("Unexpected empty array of components");
            }
            for (int i = 0; i < extra.size(); i++) {
                chatComponent.appendSibling(ctx.deserialize(extra.get(i), IChatComponent.class));
            }
        }

        // Vanilla style (deserialize ChatStyle from the whole object)
        chatComponent.setChatStyle(ctx.deserialize(element, ChatStyle.class));

        cir.setReturnValue(chatComponent);
    }
}
