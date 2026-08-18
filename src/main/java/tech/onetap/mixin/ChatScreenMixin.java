package tech.onetap.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.chat.ChatPrivacyController;
import tech.onetap.util.draggable.DragManager;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen {
    @Unique
    private static final int ONETAP_PRIVACY_BUTTON_GAP = 4;

    @Shadow protected TextFieldWidget chatField;

    @Unique
    private ButtonWidget onetap$privacyButton;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void initPrivacyControls(CallbackInfo ci) {
        if (this.chatField != null) {
            this.chatField.setRenderTextProvider((text, cursor) -> onetap$renderProtectedText(text));
            int buttonSize = Math.max(16, this.chatField.getHeight() + 4);
            int reservedWidth = buttonSize + ONETAP_PRIVACY_BUTTON_GAP;
            this.chatField.setWidth(Math.max(40, this.chatField.getWidth() - reservedWidth));
            this.onetap$privacyButton = this.addDrawableChild(ButtonWidget.builder(onetap$getPrivacyButtonText(), button -> {
                        ChatPrivacyController.toggle();
                        onetap$updatePrivacyButton();
                    })
                    .dimensions(0, 0, buttonSize, buttonSize)
                    .build());
            onetap$updatePrivacyButton();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void removed(CallbackInfo ci) {
        Interface interfaceModule = Onetap.getInstance().getModuleStorage().get(Interface.class);
        if (interfaceModule != null) {
            interfaceModule.onChatClosed();
        }
        DragManager.onReleaseAll(0);
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"), cancellable = true)
    private void injectDragClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        Interface interfaceModule = Onetap.getInstance().getModuleStorage().get(Interface.class);
        if (interfaceModule != null && interfaceModule.handleChatOverlayClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }
        DragManager.onClickAll(button);
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        onetap$updatePrivacyButton();
        DragManager.onDrawAll();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        DragManager.onReleaseAll(button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Unique
    private OrderedText onetap$renderProtectedText(String text) {
        return OrderedText.styledForwardsVisitedString(ChatPrivacyController.maskSensitiveArguments(text), Style.EMPTY);
    }

    @Unique
    private Text onetap$getPrivacyButtonText() {
        int color = ChatPrivacyController.isHideSensitiveInfo() ? 0xFFE8B866 : 0xFFFFFFFF;
        return Text.literal("B").setStyle(Style.EMPTY.withColor(color));
    }

    @Unique
    private void onetap$updatePrivacyButton() {
        if (this.onetap$privacyButton != null) {
            int buttonSize = Math.max(16, this.chatField.getHeight() + 4);
            int buttonX = this.chatField.getX() + this.chatField.getWidth() + ONETAP_PRIVACY_BUTTON_GAP;
            int buttonY = this.chatField.getY() - 2;
            this.onetap$privacyButton.setDimensions(buttonSize, buttonSize);
            this.onetap$privacyButton.setX(buttonX);
            this.onetap$privacyButton.setY(buttonY);
            this.onetap$privacyButton.setMessage(onetap$getPrivacyButtonText());
        }
    }
}
