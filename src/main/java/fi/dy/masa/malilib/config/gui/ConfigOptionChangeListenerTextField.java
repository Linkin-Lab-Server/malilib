package fi.dy.masa.malilib.config.gui;

import fi.dy.masa.malilib.config.IStringRepresentable;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class ConfigOptionChangeListenerTextField
{
    protected final IStringRepresentable config;
    protected final TextFieldWidget textField;
    protected final ButtonBase buttonReset;

    public ConfigOptionChangeListenerTextField(IStringRepresentable config, TextFieldWidget textField, ButtonBase buttonReset)
    {
        this.config = config;
        this.textField = textField;
        this.buttonReset = buttonReset;
    }

    public void onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        this.buttonReset.active = this.config.isModified(this.textField.getText());
    }
}
