package fi.dy.masa.malilib.mixin;

import java.io.IOException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import fi.dy.masa.malilib.event.dispatch.RenderEventDispatcherImpl;
import fi.dy.masa.malilib.input.InputDispatcherImpl;
import fi.dy.masa.malilib.registry.Registry;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen extends Gui
{
    @Shadow protected Minecraft mc;

    @Inject(method = "renderToolTip", at = @At("RETURN"))
    private void onRenderToolTip(ItemStack stack, int x, int y, CallbackInfo ci)
    {
        ((RenderEventDispatcherImpl) Registry.RENDER_EVENT_DISPATCHER).onRenderTooltipPost(stack, x, y, this.mc);
    }

    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityPlayerSP;sendChatMessage(Ljava/lang/String;)V"),
            cancellable = true)
    private void onSendMessage(String msg, boolean addToChat, CallbackInfo ci)
    {
        if (Registry.CLIENT_COMMAND_HANDLER.executeCommand(this.mc.player, msg) != 0)
        {
            ci.cancel();
        }
    }

    @Inject(method = "handleInput", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;handleKeyboardInput()V"))
    private void onKeyboardInputGui(CallbackInfo ci) throws IOException
    {
        InputDispatcherImpl handler = (InputDispatcherImpl) Registry.INPUT_DISPATCHER;

        if (handler.onKeyInput())
        {
            // Use up the rest of the events
            while (Keyboard.next())
            {
                if (handler.onKeyInput() == false)
                {
                    ((GuiScreen) (Object) this).handleKeyboardInput();
                }
            }

            ci.cancel();
        }
    }

    @Inject(method = "handleInput", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;handleMouseInput()V"))
    private void onMouseInputGui(CallbackInfo ci) throws IOException
    {
        InputDispatcherImpl handler = (InputDispatcherImpl) Registry.INPUT_DISPATCHER;

        if (handler.onMouseInput())
        {
            // Use up the rest of the events
            while (Mouse.next())
            {
                if (handler.onMouseInput() == false)
                {
                    ((GuiScreen) (Object) this).handleMouseInput();
                }
            }

            // Use up the rest of the events
            // The vanilla keyboard handling that comes after the mouse handling will get skipped when this method cancels,
            // and these events would then leak to the non-GUI handling code in Minecraft#runTick())
            while (Keyboard.next())
            {
                if (handler.onKeyInput() == false)
                {
                    ((GuiScreen) (Object) this).handleKeyboardInput();
                }
            }

            ci.cancel();
        }
    }
}
