package sk.breezelook;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import sk.breezelook.utills.PointsSuggestionProvider;
import sk.breezelook.Main.LookDirection;

import static sk.breezelook.Main.*;

public class Command {
    public static boolean commandUsed;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(ClientCommands.literal("look")
                .then(ClientCommands.literal("add")
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .then(ClientCommands.argument("horizontal", FloatArgumentType.floatArg())
                                        .then(ClientCommands.argument("vertical", FloatArgumentType.floatArg())
                                                .executes(Command::addCommand)
                                        )
                                )
                        )
                )
                .then(ClientCommands.literal("remove")
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .suggests(new PointsSuggestionProvider())
                                .executes(Command::removeCommand)
                        )
                )
                .then(ClientCommands.literal("warp")
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .suggests(new PointsSuggestionProvider())
                                .executes(Command::warpCommand)
                        )
                )
                .then(ClientCommands.literal("config")
                        .then(ClientCommands.literal("reload")
                            .executes(Command::reloadCommand)
                        )
                        .then(ClientCommands.literal("addchamber")
                                .then(ClientCommands.argument("name", StringArgumentType.word())
                                        .suggests(new PointsSuggestionProvider())
                                        .then(ClientCommands.argument("horizontal", FloatArgumentType.floatArg())
                                                .then(ClientCommands.argument("vertical", FloatArgumentType.floatArg())
                                                        .executes(Command::addChamberCommand)
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int reloadCommand(CommandContext<FabricClientCommandSource> context) {
        ModConfig.readConfig();
        savePoints();
        context.getSource().sendFeedback(Component.translatable("breezelook.reload"));
        return 1;
    }

    private static int addCommand(CommandContext<FabricClientCommandSource> context)
    {
        String pointName = StringArgumentType.getString(context, "name");
        float h = FloatArgumentType.getFloat(context, "horizontal");
        float v = FloatArgumentType.getFloat(context, "vertical");
        LookDirection previous = points.get(pointName);
        points.put(pointName, new LookDirection(h, v, 0, 0, previous != null ? previous.icon() : null));
        savePoints();
        context.getSource().sendFeedback(Component.translatable("breezelook.add", pointName));
        return 1;
    }

    private static int removeCommand(CommandContext<FabricClientCommandSource> context)
    {
        String pointName = StringArgumentType.getString(context, "name");
        points.remove(pointName);
        savePoints();
        context.getSource().sendFeedback(Component.translatable("breezelook.remove", pointName));
        return 1;
    }

    private static int warpCommand(CommandContext<FabricClientCommandSource> context)
    {
        String pointName = StringArgumentType.getString(context, "name");
        if (!points.containsKey(pointName)) return 1;
        warpTo(pointName);
        return 1;
    }

    private static int addChamberCommand(CommandContext<FabricClientCommandSource> context) {
        String pointName = StringArgumentType.getString(context, "name");
        float h = FloatArgumentType.getFloat(context, "horizontal");
        float v = FloatArgumentType.getFloat(context, "vertical");
        if (!points.containsKey(pointName)) return 1;
        LookDirection previous = points.get(pointName);
        points.put(pointName, new LookDirection(previous.horizontal(), previous.vertical(), h, v, previous.icon()));
        savePoints();
        context.getSource().sendFeedback(Component.translatable("breezelook.add_chamber"));
        return 1;
    }
}
