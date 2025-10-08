package net.carbonmc.graphene.mixin.client.title;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
@OnlyIn(Dist.CLIENT)
@Mixin(SplashManager.class)
public class MixinTitleScreen {
    private static final List<String> CUSTOM_SPLASHES_ZH_CN = Arrays.asList(
            "杜甫能动",
            "你好中国！",
            "umm",
            "Ahh",
            "Bot!",
            "我是彩蛋！",
            "MC百科",
            "多多益膳",
            "自己吓自己",
            "每分钟都有233台以上的GT机器发生爆炸！",
            "时光荏苒 不容虚度",
            "跟我读~ yuang duang ruang~",
            "我吓跑了114514个格雷员工！",
            "进来不要发问号",
            "错字受！"
    );

    @Inject(
            method = "getSplash",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetSplash(CallbackInfoReturnable<SplashRenderer> cir) {
        Random random = new Random();
        String language = Minecraft.getInstance().options.languageCode;
        if ("zh_cn".equals(language)) {
            if (random.nextFloat() < 0.35114f) {
                String splashText = CUSTOM_SPLASHES_ZH_CN.get(random.nextInt(CUSTOM_SPLASHES_ZH_CN.size()));
                cir.setReturnValue(new SplashRenderer(splashText));
            }
        }
    }
}