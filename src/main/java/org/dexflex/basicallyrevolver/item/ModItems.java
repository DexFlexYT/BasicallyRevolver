package org.dexflex.basicallyrevolver.item;

import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import org.dexflex.basicallyrevolver.BasicallyRevolver;

public class ModItems {
    public static final Item REVOLVER = new RevolverItem(new Item.Settings().maxCount(1));

    public static void registerModItems() {
        Registry.register(Registry.ITEM, new Identifier(BasicallyRevolver.MOD_ID, "revolver"), REVOLVER);
    }
}
