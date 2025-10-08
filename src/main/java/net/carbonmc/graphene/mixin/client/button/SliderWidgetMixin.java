package net.carbonmc.graphene.mixin.client.button;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class SliderWidgetMixin {

    @Inject(method = "onClick", at = @At("RETURN"))
    public void onClick(double mouseX, double mouseY, CallbackInfo ci) {
        AbstractSliderButton self = (AbstractSliderButton) (Object) this;
        self.setFocused(false);
    }

    @Inject(method = "onDrag", at = @At("RETURN"))
    public void onDrag(double mouseX, double mouseY, double dragX, double dragY, CallbackInfo ci) {
        AbstractSliderButton self = (AbstractSliderButton) (Object) this;
        self.setFocused(false);
    }

    @Inject(method = "onRelease", at = @At("RETURN"))
    public void onRelease(double mouseX, double mouseY, CallbackInfo ci) {
        AbstractSliderButton self = (AbstractSliderButton) (Object) this;
        self.setFocused(false);
    }
}