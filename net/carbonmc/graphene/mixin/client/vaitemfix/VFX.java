package net.carbonmc.graphene.mixin.client.vaitemfix;

import com.mojang.datafixers.util.Either;
import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.optimization.modelable.ItemFix;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static net.carbonmc.graphene.optimization.modelable.ItemFix.iseeyou;

@Mixin(ItemModelGenerator.class)
public class VFX {

    @Inject(method = "generateBlockModel", at = @At("HEAD"), cancellable = true)
    private void graphene$replaceGenerator(Function<Material, TextureAtlasSprite> spriteGetter,
                                           BlockModel model,
                                           CallbackInfoReturnable<BlockModel> cir) {

        // 安全检查：确保模型有parent
        if (model.parent == null) {
            return;
        }

        // 安全检查：确保有layer0纹理
        Map<String, Either<Material, String>> texMap = ((BlockModelAccessor) model).graphene$getTextureMap();
        Either<Material, String> either = texMap.get("layer0");
        if (either == null) {
            return;
        }

        // 获取材质
        Material material = either.map(
                mat -> mat,
                rl -> {
                    String rlStr = rl;
                    if (!rlStr.contains(":")) rlStr = "minecraft:" + rlStr;
                    return new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation(rlStr));
                }
        );

        // 获取精灵
        TextureAtlasSprite sprite = spriteGetter.apply(material);
        if (sprite == null || sprite.contents().name().toString().equals("missingno")) {
            return;
        }
        if (!iseeyou(sprite)) {
            return;
        }

        // 调试信息
        System.out.println("Processing texture: " + sprite.contents().name() +
                ", Size: " + sprite.contents().width() + "x" + sprite.contents().height());

        // 根据配置选择模式
        List<BlockElement> elements = null;

        if (CoolConfig.isOutline()) {
            System.out.println("Using OUTLINE mode");
            elements = generateSafeOutline(model, sprite);
        } else if (CoolConfig.isPixel()) {
            System.out.println("Using PIXEL mode");
            elements = generateSafePixel(model, sprite);
        } else if (CoolConfig.isFull_QUAD()) {
            System.out.println("Using FULL_QUAD mode");
            elements = ItemFix.i(0, "layer0", sprite);
        }

        // 如果元素生成失败，回退到原版生成器
        if (elements == null || elements.isEmpty()) {
            System.out.println("Fallback to vanilla generator");
            return; // 让原版方法继续执行
        }

        // 创建新模型
        BlockModel newModel = new BlockModel(
                model.parent.getParentLocation(),
                elements,
                texMap,
                model.hasAmbientOcclusion(),
                model.getGuiLight(),
                model.getTransforms(),
                model.getOverrides()
        );

        cir.setReturnValue(newModel);
    }

    /**
     * 安全的Outline模式生成
     */
    private List<BlockElement> generateSafeOutline(BlockModel originalModel, TextureAtlasSprite sprite) {
        try {
            List<BlockElement> elements = ItemFix.b(0, "layer0", sprite);

            // 安全检查
            if (elements == null || elements.isEmpty()) {
                System.out.println("Outline generation failed, falling back to full quad");
                return ItemFix.i(0, "layer0", sprite);
            }

            // 检查元素数量是否合理
            if (elements.size() > 1000) { // 防止生成过多面
                System.out.println("Too many elements in outline: " + elements.size() + ", falling back");
                return ItemFix.i(0, "layer0", sprite);
            }

            return elements;
        } catch (Exception e) {
            System.err.println("Error in outline generation: " + e.getMessage());
            return ItemFix.i(0, "layer0", sprite); // 回退到全四边形
        }
    }

    /**
     * 安全的Pixel模式生成
     */
    private List<BlockElement> generateSafePixel(BlockModel originalModel, TextureAtlasSprite sprite) {
        try {
            // 对某些已知有问题的纹理使用全四边形
            String textureName = sprite.contents().name().toString();
            if (textureName.contains("mekanism:") ||
                    textureName.contains("sculk") ||
                    sprite.contents().width() != 16 ||
                    sprite.contents().height() != 16) {
                System.out.println("Using full quad for texture: " + textureName);
                return ItemFix.i(0, "layer0", sprite);
            }

            List<BlockElement> elements = ItemFix.j(0, "layer0", sprite);

            if (elements == null || elements.isEmpty()) {
                System.out.println("Pixel generation failed, falling back to full quad");
                return ItemFix.i(0, "layer0", sprite);
            }

            if (elements.size() > 500) { // 限制像素数量
                System.out.println("Too many pixels: " + elements.size() + ", using outline instead");
                return generateSafeOutline(originalModel, sprite);
            }

            return elements;
        } catch (Exception e) {
            System.err.println("Error in pixel generation: " + e.getMessage());
            return ItemFix.i(0, "layer0", sprite);
        }
    }

    @Inject(method = "generateBlockModel", at = @At("TAIL"))
    private void graphene$applyUnlerp(Function<Material, TextureAtlasSprite> spriteGetter,
                                      BlockModel model,
                                      CallbackInfoReturnable<BlockModel> cir) {
        // 简化：暂时禁用unlerp功能，先解决主要问题
        // 原有的unlerp逻辑可能引入额外问题
    }
}