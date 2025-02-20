package com.github.fixpokebug.fixpokebug;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/*待写*/
public class CMD implements CommandExecutor , TabCompleter {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings[0].equalsIgnoreCase("uuid")){
            String string = strings[1];
            try{
                Integer i = Integer.valueOf(string);
                Pokemon pokemon = Pixelmon.storageManager.getParty(Bukkit.getPlayer(commandSender.getName()).getUniqueId()).get(i - 1);
                commandSender.sendMessage("这只精灵的uuid是"+pokemon.getUUID());
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
