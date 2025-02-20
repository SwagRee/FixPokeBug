package com.github.fixpokebug.fixpokebug;

import com.github.fixpokebug.fixpokebug.util.MsgUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class Main extends JavaPlugin {
    public static Main main;
    public static HashMap<String, String> sendMap = new HashMap();
    @Override
    public void onEnable() {
        main = this;
        reloadConfig();
        getServer().getPluginManager().registerEvents(new PlayerListener(), main);
        getCommand("fixpokebug").setExecutor(new CMD());
        getLogger().info("§a插件已启用!--§3更多bug可请评论于:§a https://bbs.mc9y.net/resources/720/");
    }

    @Override
    public void reloadConfig() {
        main.saveDefaultConfig();
        super.reloadConfig();
        MsgUtil.init();
    }
}
