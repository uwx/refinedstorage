package com.raoulvdberge.refinedstorage.render;

import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingPattern;
import com.raoulvdberge.refinedstorage.container.ContainerCrafter;
import com.raoulvdberge.refinedstorage.container.ContainerCrafterManager;
import com.raoulvdberge.refinedstorage.container.slot.SlotCrafterManager;
import com.raoulvdberge.refinedstorage.gui.GuiBase;
import com.raoulvdberge.refinedstorage.item.ItemPattern;
import com.raoulvdberge.refinedstorage.util.RenderUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

public class BakedModelPattern implements IBakedModel {
    private IBakedModel base;

    public BakedModelPattern(IBakedModel base) {
        this.base = base;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        TRSRTransformation transform = RenderUtils.getDefaultItemTransforms().get(cameraTransformType);

        return Pair.of(this, transform == null ? RenderUtils.EMPTY_MATRIX_TRANSFORM : transform.getMatrix());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return base.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return base.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return base.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return base.getParticleTexture();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return base.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return new ItemOverrideList(base.getOverrides().getOverrides()) {
            @Override
            public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity) {
                CraftingPattern pattern = ItemPattern.getPatternFromCache(world, stack);

                if (canDisplayPatternOutput(stack, pattern)) {
                    return Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(pattern.getOutputs().get(0), world, entity);
                }

                return super.handleItemState(originalModel, stack, world, entity);
            }
        };
    }

    public static boolean canDisplayPatternOutput(ItemStack patternStack, CraftingPattern pattern) {
        return (GuiBase.isShiftKeyDown() || isPatternInDisplaySlot(patternStack)) && pattern.isValid() && pattern.getOutputs().size() == 1;
    }

    public static boolean isPatternInDisplaySlot(ItemStack stack) {
        Container container = Minecraft.getMinecraft().player.openContainer;

        if (container instanceof ContainerCrafterManager) {
            for (Slot slot : container.inventorySlots) {
                if (slot instanceof SlotCrafterManager && slot.getStack() == stack) {
                    return true;
                }
            }
        } else if (container instanceof ContainerCrafter) {
            for (int i = 0; i < 9; ++i) {
                if (container.getSlot(i).getStack() == stack) {
                    return true;
                }
            }
        }

        return false;
    }
}
