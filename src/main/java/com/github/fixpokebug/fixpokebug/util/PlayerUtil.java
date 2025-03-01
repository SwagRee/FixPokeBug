package com.github.fixpokebug.fixpokebug.util;

import net.minecraft.entity.player.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerUtil {
    /**
     * 由bukkit玩家获取Forge玩家
     * @param player Bukkit玩家
     * @return Forge EntityPlayer
     */
    public static EntityPlayer getEntityPlayer(Player player){
        return (EntityPlayer) EntityUtil.getForgeEntity(player);
    }


    public static Player getBukkitPlayer(EntityPlayer player){
        return Bukkit.getPlayer(EntityUtil.getUUID(player));
    }
}
