package org.dexflex.basicallyrevolver.sound;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.dexflex.basicallyrevolver.BasicallyRevolver;

public class ModSounds {
    public static final SoundEvent REVOLVER_SHOOT = registerSound("revolver.shoot");

    private static SoundEvent registerSound(String name) {
        Identifier id = new Identifier(BasicallyRevolver.MOD_ID, name);
        SoundEvent soundEvent = new SoundEvent(id);
        return Registry.register(Registry.SOUND_EVENT, id, soundEvent);
    }

    public static void registerModSounds() {
    }
}