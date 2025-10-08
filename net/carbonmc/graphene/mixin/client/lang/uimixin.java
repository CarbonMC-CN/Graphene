package net.carbonmc.graphene.mixin.client.lang;

import net.carbonmc.graphene.lang.flang;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LanguageSelectScreen.class)
public abstract class uimixin extends OptionsSubScreen {

    public uimixin(Screen p_96284_, Options p_96285_, Component p_96286_) {
        super(p_96284_, p_96285_, p_96286_);
    }

    @Redirect(method = "init",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LanguageSelectScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",ordinal = 1))
    public <T extends GuiEventListener & Renderable & NarratableEntry> T notReloadResourcePacks(LanguageSelectScreen instance, T guiEventListener){
        if(guiEventListener instanceof Button button){
            final Button.OnPress fi = button.onPress;
            button.onPress = (p_96099_) -> {
                flang.langReload = true;
                fi.onPress(p_96099_);
            };
            this.addRenderableWidget(button);
        }
        return this.addRenderableWidget(guiEventListener);
    }
}
