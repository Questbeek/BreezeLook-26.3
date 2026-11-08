package sk.breezelook.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Icon helpers for warp points: icons are stored as item ids and picked from the block items. */
public final class WarpIcons {
    private static final Map<String, ItemStack> CACHE = new HashMap<>();
    private static List<Item> blockItems;
    private static List<String> searchKeys;

    private WarpIcons() {}

    public static ItemStack defaultStack() {
        return new ItemStack(Items.ENDER_PEARL);
    }

    /** The stack to draw for a stored icon id; falls back to the default icon. */
    public static ItemStack stackFor(String id) {
        if (id == null || id.isEmpty()) return CACHE.computeIfAbsent("", k -> defaultStack());
        return CACHE.computeIfAbsent(id, key -> {
            Identifier identifier = Identifier.tryParse(key);
            if (identifier != null) {
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(identifier);
                if (item.isPresent() && item.get() != Items.AIR) return new ItemStack(item.get());
            }
            return defaultStack();
        });
    }

    public static String idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** All block items, in registry order. */
    public static List<Item> blockItems() {
        if (blockItems == null) {
            LinkedHashSet<Item> items = new LinkedHashSet<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                Item item = block.asItem();
                if (item != Items.AIR) items.add(item);
            }
            blockItems = new ArrayList<>(items);
            searchKeys = new ArrayList<>(blockItems.size());
            for (Item item : blockItems) {
                String path = BuiltInRegistries.ITEM.getKey(item).getPath();
                String name = new ItemStack(item).getHoverName().getString();
                searchKeys.add((path + " " + name).toLowerCase(Locale.ROOT));
            }
        }
        return blockItems;
    }

    /** Block items whose id or display name contains the query. */
    public static List<Item> search(String query) {
        List<Item> all = blockItems();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return all;
        List<Item> result = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (searchKeys.get(i).contains(q)) result.add(all.get(i));
        }
        return result;
    }
}
