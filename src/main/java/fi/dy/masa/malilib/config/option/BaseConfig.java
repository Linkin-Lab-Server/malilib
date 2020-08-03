package fi.dy.masa.malilib.config.option;

import java.util.Locale;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.ValueChangeCallback;
import fi.dy.masa.malilib.config.ValueLoadedCallback;
import fi.dy.masa.malilib.util.StringUtils;

public abstract class BaseConfig<T> implements ConfigOption<T>
{
    protected final ConfigType type;
    protected final String name;
    protected String nameTranslationKey;
    protected String prettyNameTranslationKey;
    protected String commentTranslationKey;
    protected Object[] commentArgs;
    protected String modId = "?";
    @Nullable
    protected ValueChangeCallback<T> valueChangeCallback;
    @Nullable
    protected ValueLoadedCallback<T> valueLoadCallback;

    public BaseConfig(ConfigType type, String name)
    {
        this(type, name, name, name, name);
    }

    public BaseConfig(ConfigType type, String name, String commentTranslationKey, Object... commentArgs)
    {
        this(type, name, name, name, commentTranslationKey, commentArgs);
    }

    public BaseConfig(ConfigType type, String name, String nameTranslationKey,
                      String prettyNameTranslationKey, String commentTranslationKey, Object... commentArgs)
    {
        this.type = type;
        this.name = name;
        this.nameTranslationKey = nameTranslationKey;
        this.prettyNameTranslationKey = prettyNameTranslationKey;
        this.commentTranslationKey = commentTranslationKey;
        this.commentArgs = commentArgs;
    }

    @Override
    public ConfigType getType()
    {
        return this.type;
    }

    @Override
    public String getModId()
    {
        return this.modId;
    }

    @Override
    public void setModId(String modId)
    {
        this.modId = modId;

        String nameLower = this.name.toLowerCase(Locale.ROOT);

        // If these are still using the default values, generate the proper keys
        if (this.nameTranslationKey.equals(this.name))
        {
            this.nameTranslationKey = modId + ".config.name." + nameLower;
        }

        if (this.prettyNameTranslationKey.equals(this.name))
        {
            this.prettyNameTranslationKey = this.nameTranslationKey;
        }

        if (this.commentTranslationKey.equals(this.name))
        {
            this.commentTranslationKey = modId + ".config.comment." + nameLower;
        }
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getConfigNameTranslationKey()
    {
        return this.nameTranslationKey;
    }

    @Override
    public String getPrettyName()
    {
        return StringUtils.translate(this.prettyNameTranslationKey);
    }

    @Override
    @Nullable
    public String getCommentTranslationKey()
    {
        return this.commentTranslationKey;
    }

    @Override
    @Nullable
    public String getComment()
    {
        return StringUtils.translate(this.getCommentTranslationKey(), this.commentArgs);
    }

    public BaseConfig<T> setCommentArgs(Object... args)
    {
        this.commentArgs = args;
        return this;
    }

    @Override
    public void setValueChangeCallback(@Nullable ValueChangeCallback<T> callback)
    {
        this.valueChangeCallback = callback;
    }

    @Override
    public void setValueLoadCallback(@Nullable ValueLoadedCallback<T> callback)
    {
        this.valueLoadCallback = callback;
    }

    @Override
    public void onValueChanged(T newValue, T oldValue)
    {
        if (this.valueChangeCallback != null)
        {
            this.valueChangeCallback.onValueChanged(newValue, oldValue);
        }
    }

    @Override
    public void onValueLoaded(T newValue)
    {
        if (this.valueLoadCallback != null)
        {
            this.valueLoadCallback.onValueLoaded(newValue);
        }
    }
}
