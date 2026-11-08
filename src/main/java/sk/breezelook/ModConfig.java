package sk.breezelook;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "breezelook/config")
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.Excluded
    public static ModConfig INSTANCE;

    public static void register()
    {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        INSTANCE = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    public static void readConfig()
    {
        AutoConfig.getConfigHolder(ModConfig.class).load();
    }

    @ConfigEntry.Gui.Tooltip()
    public boolean confirm = true;
    @ConfigEntry.Gui.Tooltip()
    public int confirmDelay = 60;
    @ConfigEntry.Gui.Tooltip()
    public float confirmHorizontal = 0;
    @ConfigEntry.Gui.Tooltip()
    public float confirmVertical = 90;
    @ConfigEntry.Gui.Tooltip()
    public boolean returnCamera = true;
    @ConfigEntry.Gui.Tooltip()
    public int returnCameraDelay = 60;
}
