package com.github.fixpokebug.fixpokebug.util;

import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import net.minecraft.entity.player.EntityPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;


public class EventHandlerUtil {

    private static void interceptAllMoveInBattle(PlayerEvent event){
        if (!(event instanceof Cancellable)){return;}
        Player player = event.getPlayer();
        EntityPlayer ep = PlayerUtil.getEntityPlayer(player);
        BattleControllerBase bc = BattleRegistry.getBattle(ep);
        if (bc == null){return;}
        player.sendMessage(MsgUtil.getMsg(MsgUtil.unmovable));
        ((Cancellable) event).setCancelled(true);
    }

    public static class PlayerTeleportEvent {
        public static void unableToTeleportDuringBattle(org.bukkit.event.player.PlayerTeleportEvent e) {
            interceptAllMoveInBattle(e);
        }
    }
}
