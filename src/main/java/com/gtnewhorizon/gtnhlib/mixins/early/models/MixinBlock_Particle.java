package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import cpw.mods.fml.client.FMLClientHandler;

@Mixin(Block.class)
public abstract class MixinBlock_Particle {

    @Shadow
    public abstract int getRenderType();

    @Shadow
    protected IIcon blockIcon;

    @Shadow
    public abstract IIcon getBlockTextureFromSide(int side);

    @WrapMethod(method = "getIcon(Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;")
    private IIcon nhlib$wrapGetIcon(IBlockAccess world, int x, int y, int z, int side, Operation<IIcon> original) {
        return getRenderType() == ModelISBRH.JSON_ISBRH_ID
                ? getParticleIcon(world, x, y, z, world.getBlockMetadata(x, y, z))
                : original.call(world, x, y, z, side);
    }

    @WrapMethod(method = "getIcon(II)Lnet/minecraft/util/IIcon;")
    private IIcon nhlib$wrapGetIcon(int side, int meta, Operation<IIcon> original) {
        return getRenderType() == ModelISBRH.JSON_ISBRH_ID ? getParticleIcon(null, 0, 0, 0, meta)
                : original.call(side, meta);
    }

    @WrapMethod(method = "func_149735_b")
    private IIcon nhlib$wrapGetIconObf(int side, int meta, Operation<IIcon> original) {
        return getRenderType() == ModelISBRH.JSON_ISBRH_ID ? getParticleIcon(null, 0, 0, 0, meta)
                : original.call(side, meta);
    }

    @WrapMethod(method = "getBlockTextureFromSide")
    private IIcon nhlib$wrapGetIcon(int side, Operation<IIcon> original) {
        // Set the blockIcon here so anything outside accessing it with ATs still gets the correct icon.
        // Can't do it in registericons because the icon isn't baked or somethimg, it crashes.
        return getRenderType() == ModelISBRH.JSON_ISBRH_ID ? getParticleIcon(null, 0, 0, 0, 0) : original.call(side);
    }

    @WrapMethod(method = "registerBlockIcons")
    private void nhlib$setField(IIconRegister reg, Operation<Void> original) {
        original.call(reg);
        if (getRenderType() == ModelISBRH.JSON_ISBRH_ID && blockIcon == null) {
            blockIcon = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("missingno");
        }
    }

    private IIcon getParticleIcon(@Nullable IBlockAccess world, int x, int y, int z, int meta) {
        IIcon particle = ModelISBRH.INSTANCE.getParticleIcon((Block) (Object) this, world, x, y, z, meta);
        return particle == null
                ? FMLClientHandler.instance().getClient().getTextureMapBlocks().registerIcon("missingno")
                : particle;
    }
}
