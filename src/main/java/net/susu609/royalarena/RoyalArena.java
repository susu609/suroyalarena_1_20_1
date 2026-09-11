package net.susu609.royalarena;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.susu609.royalarena.network.ModNetwork;
import org.slf4j.Logger;

@Mod(RoyalArena.MOD_ID) public final class RoyalArena {
    public static final String MOD_ID = "royalarena";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RoyalArena() {
        ModNetwork.register();
        LOGGER.info("{} loaded", MOD_ID);
    }
}
