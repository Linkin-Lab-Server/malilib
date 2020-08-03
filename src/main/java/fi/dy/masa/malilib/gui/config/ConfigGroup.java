package fi.dy.masa.malilib.gui.config;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.option.ConfigInfo;
import fi.dy.masa.malilib.util.StringUtils;

public class ConfigGroup implements ConfigInfo
{
    protected final ConfigType type;
    protected final String nameTranslationKey;
    protected final String commentTranslationKey;
    protected final Object[] commentArgs;
    protected ImmutableList<ConfigInfo> configs = ImmutableList.of();

    public ConfigGroup(ConfigType type, String name, String modId)
    {
        this(type, modId + ".config_group.name." + name, modId + ".config_group.comment." + name, new Object[0]);
    }

    public ConfigGroup(ConfigType type, String nameTranslationKey, String commentTranslationKey, Object... commentArgs)
    {
        this.type = type;
        this.nameTranslationKey = nameTranslationKey;
        this.commentTranslationKey = commentTranslationKey;
        this.commentArgs = commentArgs;
    }

    /**
     * Sets the list of contained configs, overriding any old values
     * @param configs
     * @return
     */
    public ConfigGroup setConfigs(ConfigInfo... configs)
    {
        this.configs = ImmutableList.copyOf(configs);
        return this;
    }

    /**
     * Returns the list of configs contained within this group
     * @return
     */
    public ImmutableList<ConfigInfo> getConfigs()
    {
        return this.configs;
    }

    @Override
    public ConfigType getType()
    {
        return this.type;
    }

    @Override
    public String getName()
    {
        return StringUtils.translate(this.getConfigNameTranslationKey());
    }

    @Override
    public String getConfigNameTranslationKey()
    {
        return this.nameTranslationKey;
    }

    @Override
    @Nullable
    public String getCommentTranslationKey()
    {
        return this.commentTranslationKey;
    }
}
