package sk.breezelook;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import sk.breezelook.gui.WarpWheelScreen;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "breezelook";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Map<String, LookDirection> points = new HashMap<>();

    private static final Gson gson = new Gson();
    private static final File points_config = new File(FabricLoader.getInstance().getConfigDir().toFile(), "breezelook/points.json");

    public static LookDirection lookDirection;

    /** Hold this key to open the warp wheel (rebindable in Controls). */
    public static KeyMapping wheelKey;

    private int tick;

    @Override
    public void onInitializeClient() {
        ModConfig.register();
        wheelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.breezelook.open_wheel",
                InputConstants.KEY_G,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));
        ClientCommandRegistrationCallback.EVENT.register(Command::register);
        ClientLifecycleEvents.CLIENT_STARTED.register(this::clientStarted);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void clientStarted(Minecraft client) {
        loadPoints();
    }

    public static void loadPoints() {
        if (points_config.exists()) {
            try (FileReader reader = new FileReader(points_config)) {
                Type type = new TypeToken<Map<String, LookDirection>>() {}.getType();
                Map<String, LookDirection> loadedPoints = gson.fromJson(reader, type);
                points = loadedPoints != null ? loadedPoints : new HashMap<>();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void savePoints() {
        File dir = points_config.getParentFile();
        if (dir != null) dir.mkdirs();
        try (FileWriter writer = new FileWriter(points_config)) {
            gson.toJson(points, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Turns the camera to the saved point and starts the confirm sequence if enabled. */
    public static void warpTo(String name) {
        LookDirection direction = points.get(name);
        LocalPlayer player = Minecraft.getInstance().player;
        if (direction == null || player == null) return;
        lookDirection = direction;
        player.setYRot(direction.horizontal());
        player.setXRot(direction.vertical());
        if (ModConfig.INSTANCE.confirm) Command.commandUsed = true;
    }

    private void tick(Minecraft client) {
        if (client.player != null && client.level != null && client.gui.screen() == null) {
            boolean open = false;
            while (wheelKey.consumeClick()) open = true;
            if (open) client.setScreenAndShow(new WarpWheelScreen(wheelKey));
        }

        if (Command.commandUsed) tick++;
        if (tick > ModConfig.INSTANCE.confirmDelay) {
            if (client.player != null) {
                client.player.setYRot(ModConfig.INSTANCE.confirmHorizontal);
                client.player.setXRot(ModConfig.INSTANCE.confirmVertical);
                if (ModConfig.INSTANCE.returnCamera && (tick > ModConfig.INSTANCE.confirmDelay + ModConfig.INSTANCE.returnCameraDelay)) {
                    client.player.setYRot(lookDirection.chamberHorizontal());
                    client.player.setXRot(lookDirection.chamberVertical());
                    Command.commandUsed = false;
                    tick = 0;
                }
                else if (!ModConfig.INSTANCE.returnCamera)
                {
                    Command.commandUsed = false;
                    tick = 0;
                }
            }
        }
    }

    /** {@code icon} is an item id such as "minecraft:stone"; null means the default icon. */
    public record LookDirection(float horizontal, float vertical, float chamberHorizontal, float chamberVertical, String icon) {
        public LookDirection(float horizontal, float vertical, float chamberHorizontal, float chamberVertical) {
            this(horizontal, vertical, chamberHorizontal, chamberVertical, null);
        }
    }
}
