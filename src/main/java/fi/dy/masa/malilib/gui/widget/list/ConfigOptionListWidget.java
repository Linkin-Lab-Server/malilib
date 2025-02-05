package fi.dy.masa.malilib.gui.widget.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.config.option.ConfigInfo;
import fi.dy.masa.malilib.gui.config.ConfigOptionWidgetFactory;
import fi.dy.masa.malilib.gui.config.ConfigTab;
import fi.dy.masa.malilib.gui.config.ConfigWidgetContext;
import fi.dy.masa.malilib.gui.config.KeybindEditingScreen;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetFactory;
import fi.dy.masa.malilib.gui.widget.list.entry.config.BaseConfigWidget;
import fi.dy.masa.malilib.gui.widget.list.search.ConfigsSearchBarWidget;
import fi.dy.masa.malilib.input.CustomHotkeyManager;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.ConfigOnTab;
import fi.dy.masa.malilib.util.data.ModInfo;

public class ConfigOptionListWidget<C extends ConfigInfo> extends DataListWidget<C>
{
    protected final Map<ConfigsSearchBarWidget.Scope, List<ConfigOnTab>> cachedConfigs = new HashMap<>();
    protected final ModInfo modInfo;
    protected final IntSupplier defaultElementWidthSupplier;
    @Nullable protected ConfigsSearchBarWidget configsSearchBarWidget;
    protected boolean showInternalConfigName;
    protected int maxLabelWidth;

    protected ConfigOptionListWidget(IntSupplier defaultElementWidthSupplier,
                                     ModInfo modInfo,
                                     Supplier<List<C>> entrySupplier,
                                     ConfigWidgetContext ctx)
    {
        super(entrySupplier, true);

        this.modInfo = modInfo;
        this.defaultElementWidthSupplier = defaultElementWidthSupplier;
        this.allowKeyboardNavigation = true;
        this.showInternalConfigName = MaLiLibConfigs.Generic.SHOW_INTERNAL_CONFIG_NAME.getBooleanValue();

        this.setDataListEntryWidgetFactory(new ConfigOptionListEntryWidgetFactory<>(ctx));
        this.setEntryFilterStringFunction(ConfigInfo::getSearchStrings);
        this.getBorderRenderer().getNormalSettings().setBorderWidth(0);

        this.listPosition.setTop(0);
    }

    public int getMaxLabelWidth()
    {
        return this.maxLabelWidth;
    }

    public int getElementWidth()
    {
        if (this.isShowingOptionsFromOtherCategories())
        {
            // FIXME how to retrieve the correct max width from the config tabs?
            return 220;
        }

        return this.defaultElementWidthSupplier.getAsInt();
    }

    public boolean getShowInternalConfigName()
    {
        return this.showInternalConfigName;
    }

    public void setShowInternalConfigName(boolean showInternalConfigName)
    {
        this.showInternalConfigName = showInternalConfigName;
    }

    public boolean isShowingOptionsFromOtherCategories()
    {
        return this.configsSearchBarWidget != null &&
               this.configsSearchBarWidget.getCurrentScope() != ConfigsSearchBarWidget.Scope.CURRENT_CATEGORY;
    }

    public void addConfigSearchBarWidget(KeybindEditingScreen screen)
    {
        this.configsSearchBarWidget = new ConfigsSearchBarWidget(this.getWidth(), 32,
                                                                 this::onSearchBarTextChanged,
                                                                 this::onSearchOpenOrClose,
                                                                 this::refreshEntries,
                                                                 DefaultIcons.SEARCH,
                                                                 this::resetFilteredConfigsToDefaults,
                                                                 screen);
        this.configsSearchBarWidget.getMargin().setBottom(2);
        this.searchBarWidget = this.configsSearchBarWidget;
    }

    protected void onSearchOpenOrClose()
    {
        this.updateSubWidgetPositions();
        this.refreshEntries();
    }

    protected void resetFilteredConfigsToDefaults()
    {
        for (C config : this.filteredDataList)
        {
            config.resetToDefault();
        }

        this.refreshEntries();
    }

    @Nullable
    public String getModNameAndCategory(int listIndex, boolean showInternalName)
    {
        if (this.configsSearchBarWidget != null)
        {
            List<ConfigOnTab> configs = this.cachedConfigs.get(this.configsSearchBarWidget.getCurrentScope());

            if (configs != null && listIndex < configs.size())
            {
                ConfigOnTab configOnTab = configs.get(listIndex);
                ConfigTab tab = configOnTab.getTab();
                String modName = tab.getModInfo().getModName();
                String tabName = tab.getDisplayName();

                if (showInternalName)
                {
                    String configName = configOnTab.getConfig().getName();
                    return StringUtils.translate("malilib.label.config.mod_category_internal_name",
                                                 modName, tabName, configName);
                }
                else
                {
                    return StringUtils.translate("malilib.label.config.mod_category", modName, tabName);
                }
            }
        }

        return null;
    }

    @Override
    protected boolean entryMatchesFilter(C entry, List<String> searchTerms)
    {
        return super.entryMatchesFilter(entry, searchTerms) &&
               (this.configsSearchBarWidget == null || this.configsSearchBarWidget.passesFilter(entry));
    }

    @Override
    protected void onEntriesRefreshed()
    {
        super.onEntriesRefreshed();

        List<C> list = this.getNonFilteredDataList();
        final int size = list.size();
        boolean showCategory = this.isShowingOptionsFromOtherCategories();
        int maxLabelWidth = 0;

        for (int i = 0; i < size; ++i)
        {
            ConfigInfo config = list.get(i);
            int width = this.getEntryNameColumnWidth(i, config, showCategory, this.showInternalConfigName);
            maxLabelWidth = Math.max(maxLabelWidth, width);
        }

        this.maxLabelWidth = maxLabelWidth;
    }

    protected int getEntryNameColumnWidth(int listIndex, ConfigInfo config,
                                          boolean showCategory, boolean showInternalName)
    {
        int labelWidth = 0;

        if (showCategory)
        {
            String str = this.getModNameAndCategory(listIndex, showInternalName);
            labelWidth = this.getStringWidth(str);
        }

        if (showInternalName)
        {
            labelWidth = Math.max(labelWidth, this.getStringWidth(config.getName()));
        }

        // The +10 here compensates for the left padding of the label widgets,
        // which is used because of the hover border used for the labels of configs with a click handler
        return Math.max(labelWidth, this.getStringWidth(config.getDisplayName())) + 10;
    }

    @Override
    public ArrayList<C> getNonFilteredDataList()
    {
        if (this.configsSearchBarWidget != null &&
            this.configsSearchBarWidget.getCurrentScope() != ConfigsSearchBarWidget.Scope.CURRENT_CATEGORY)
        {
            ConfigsSearchBarWidget.Scope scope = this.configsSearchBarWidget.getCurrentScope();
            return this.getAndCacheConfigsFromScope(scope);
        }

        return super.getNonFilteredDataList();
    }

    protected ArrayList<C> getAndCacheConfigsFromScope(ConfigsSearchBarWidget.Scope scope)
    {
        List<ConfigOnTab> configsInScope = this.cachedConfigs.get(scope);

        if (configsInScope == null)
        {
            configsInScope = new ArrayList<>();

            if (scope == ConfigsSearchBarWidget.Scope.ALL_MODS)
            {
                List<ConfigTab> allModTabs = Registry.CONFIG_TAB.getAllRegisteredConfigTabs();
                allModTabs.add(CustomHotkeyManager.INSTANCE.getAllCustomHotkeysAsTabForConfigSearch());
                final List<ConfigOnTab> tmpList = configsInScope;
                allModTabs.forEach((tab) -> tab.getTabbedExpandedConfigs(tmpList::add));
            }
            else
            {
                Supplier<List<ConfigTab>> tabProvider = Registry.CONFIG_TAB.getConfigTabProviderFor(this.modInfo);

                if (tabProvider != null)
                {
                    final List<ConfigOnTab> tmpList = configsInScope;
                    tabProvider.get().forEach((tab) -> tab.getTabbedExpandedConfigs(tmpList::add));
                }
            }

            this.cachedConfigs.put(scope, configsInScope);
        }

        ArrayList<C> list = new ArrayList<>();

        for (ConfigOnTab cfg : configsInScope)
        {
            @SuppressWarnings("unchecked")
            C c = (C) cfg.getConfig();
            list.add(c);
        }

        return list;
    }

    /**
     * Clears the cache of config options per search scopes
     */
    public void clearConfigSearchCache()
    {
        this.cachedConfigs.clear();
    }

    public static <C extends ConfigInfo>
    ConfigOptionListWidget<C> createWithExpandedGroups(IntSupplier defaultElementWidthSupplier,
                                                       ModInfo modInfo,
                                                       Supplier<List<C>> entrySupplier,
                                                       ConfigWidgetContext ctx)
    {
        Supplier<List<C>> supplier = createUnNestingConfigSupplier(entrySupplier);
        return new ConfigOptionListWidget<>(defaultElementWidthSupplier, modInfo, supplier, ctx);
    }

    public static <C extends ConfigInfo> Supplier<List<C>> createUnNestingConfigSupplier(Supplier<List<C>> entrySupplier)
    {
        return () -> {
            List<C> originalList = entrySupplier.get();
            ArrayList<C> expandedList = new ArrayList<>();

            for (C config : originalList)
            {
                expandedList.add(config);
                config.addNestedOptionsToList(expandedList, 1);
            }

            return expandedList;
        };
    }

    public static class ConfigOptionListEntryWidgetFactory<C extends ConfigInfo> implements DataListEntryWidgetFactory<C>
    {
        protected final ConfigWidgetContext ctx;

        public ConfigOptionListEntryWidgetFactory(ConfigWidgetContext ctx)
        {
            this.ctx = ctx;
        }

        @Override
        @Nullable
        public BaseConfigWidget<? extends ConfigInfo> createWidget(C config, DataListEntryWidgetData constructData)
        {
            ConfigOptionWidgetFactory<C> factory = Registry.CONFIG_WIDGET.getWidgetFactory(config);
            return factory.create(config, constructData, this.ctx);
        }
    }
}
