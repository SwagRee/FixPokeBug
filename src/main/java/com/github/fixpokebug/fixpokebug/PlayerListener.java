package com.github.fixpokebug.fixpokebug;

import catserver.api.bukkit.event.ForgeEvent;
import com.github.fixpokebug.fixpokebug.util.EventHandlerUtil;
import com.github.fixpokebug.fixpokebug.util.MsgUtil;
import com.github.fixpokebug.fixpokebug.util.PlayerUtil;

import com.pixelmonmod.pixelmon.api.events.BattleStartedEvent;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.events.HeldItemChangedEvent;
import com.pixelmonmod.pixelmon.api.events.pokemon.ItemFormChangeEvent;
import com.pixelmonmod.pixelmon.api.events.pokemon.MovesetEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.abilities.AbilityBase;
import com.pixelmonmod.pixelmon.entities.pixelmon.abilities.Magician;
import com.pixelmonmod.pixelmon.items.ItemHeld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {
    public static List<UUID> isCapturedPokemon = new ArrayList();
    @EventHandler
    public void onForge(ForgeEvent event) {
        /**
         * 复制宝可梦部分的修复 1.区块传送机制 2.同步进入战斗 3.区块加载机制 4.神兽刷新逻辑漏洞
         * 空技能卡巢穴的修复
         */
        try {
            if (event.getForgeEvent() instanceof CaptureEvent.StartCapture) {
                CaptureEvent.StartCapture e = (CaptureEvent.StartCapture)event.getForgeEvent();
                EntityPixelmon entityPixelmon = e.getPokemon();

                if(isCapturedPokemon.contains(entityPixelmon.getPokemonData().getUUID())){
                    e.setCanceled(true);
                    BattleControllerBase battleController = BattleRegistry.getBattle(e.player);
                    battleController.endBattle();
                    entityPixelmon.setHealth(0);
                }
            }

            if (event.getForgeEvent() instanceof CaptureEvent.SuccessfulCapture) {
                CaptureEvent.SuccessfulCapture e = (CaptureEvent.SuccessfulCapture)event.getForgeEvent();
                EntityPixelmon pokemon = e.getPokemon();
                isCapturedPokemon.add(pokemon.getPokemonData().getUUID());
            }

            if (event.getForgeEvent() instanceof BattleStartedEvent) {
                BattleStartedEvent e = (BattleStartedEvent)event.getForgeEvent();
                for (BattleParticipant participant : e.bc.participants) {
                    PixelmonWrapper[] allPokemon = participant.allPokemon;
                    for (PixelmonWrapper pixelmonWrapper : allPokemon) {
                        if (pixelmonWrapper.getMoveset().isEmpty()) {
                            pixelmonWrapper.pokemon.rerollMoveset();
                            pixelmonWrapper.setTemporaryMoveset(pixelmonWrapper.pokemon.getMoveset());
                            PlayerUtil.getBukkitPlayer(pixelmonWrapper.getPlayerOwner()).sendMessage(MsgUtil.getMsg(MsgUtil.skillIsEmpty));
                        }
                        if (pixelmonWrapper.isWildPokemon() && !pixelmonWrapper.getOriginalTrainer().isEmpty() ) {
                            if (isCapturedPokemon.contains(pixelmonWrapper.pokemon.getUUID())) {
                                e.setCanceled(true);
                                pixelmonWrapper.setHealth(0);
                            }
                        }

                    }
                }
            }

            /**
             * 强制装载携带物的修复
             */
            if (event.getForgeEvent() instanceof HeldItemChangedEvent) {
                HeldItemChangedEvent e = (HeldItemChangedEvent)event.getForgeEvent();
                if (fixAnyHeldItem(e)) return;
                Pokemon pokemon = e.pokemon;

                fixMoveChange(pokemon, e);
            }
            if (event.getForgeEvent() instanceof MovesetEvent) {
                CaptureEvent.StartCapture e = (CaptureEvent.StartCapture)event.getForgeEvent();
                EntityPixelmon entityPixelmon = e.getPokemon();

                if(isCapturedPokemon.contains(entityPixelmon.getPokemonData().getUUID())){
                    e.setCanceled(true);
                    BattleControllerBase battleController = BattleRegistry.getBattle(e.player);
                    battleController.endBattle();
                    entityPixelmon.setHealth(0);
                }
            }

        } catch (Exception e2) {

        }

    }

    private static void fixMoveChange(Pokemon pokemon, HeldItemChangedEvent e) {
        if (pokemon.getPixelmonWrapperIfExists() != null) {
            PixelmonWrapper wrapper = pokemon.getPixelmonWrapperIfExists();
            List<Attack> attacks = new ArrayList(wrapper.getMoveset());
            List<AbilityBase> bases = new ArrayList();
            wrapper.getOpponentPokemon().forEach((pixelmonWrapper) -> attacks.addAll(pixelmonWrapper.getMoveset()));
            bases.add(wrapper.getAbility());
            wrapper.getOpponentPokemon().forEach((pixelmonWrapper) -> bases.add(pixelmonWrapper.getAbility()));
            attacks.forEach((attack) -> {
                if (attack.isAttack(new String[]{"Covet", "Bestow", "Trick", "Thief", "Switcheroo"})) {
                    e.setCanceled(true);
                    if (e.player != null) {
                            Bukkit.getPlayer(e.player.getUniqueID()).sendMessage("该技能有BUG，禁止使用，将不会生效");
                    } else if (pokemon.getOwnerPlayer() != null) {
                        Bukkit.getPlayer(e.player.getUniqueID()).sendMessage("该技能有BUG，禁止使用，将不会生效");
                    }
                }

            });
            bases.forEach((abilityBase) -> {
                if (abilityBase.isAbility(Magician.class)) {
                    e.setCanceled(true);
                    if (e.player != null) {
                        Bukkit.getPlayer(e.player.getUniqueID()).sendMessage("该特性有BUG，禁止使用，将不会生效");
                    } else if (pokemon.getOwnerPlayer() != null) {
                        Bukkit.getPlayer(e.player.getUniqueID()).sendMessage("该特性有BUG，禁止使用，将不会生效");
                    }
                }

            });
        }
    }

    private static boolean fixAnyHeldItem(HeldItemChangedEvent e) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        ItemStack newHeldItem = e.newHeldItem;

        Method getItemMethod = ItemStack.class.getDeclaredMethod("func_77973_b");
        getItemMethod.setAccessible(true); // 使私有方法可访问


        // 调用方法获取 Item 对象
        Item item = (Item) getItemMethod.invoke(newHeldItem);
        if (item.delegate.name().toString().equals("minecraft:air") || item.delegate.name().toString().toLowerCase().startsWith("pixelmon:tm_gen") || (item.delegate.name().toString().toLowerCase().startsWith("pixelmon:tr") )) {
            return true;
        }

        if (!(item instanceof ItemHeld)) {
            e.setCanceled(true);
            e.pokemon.setHeldItem(null);
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.isCancelled()) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Block block = event.getClickedBlock();
                if (block.getType().name().endsWith("APRICORN_TREE")) {
                    Block top = block.getLocation().clone().add(0.0D, 1.0D, 0.0D).getBlock();
                    if (top != null) {
                        Player player = event.getPlayer();
                        if (this.isDenyItem(player.getInventory().getItemInMainHand()) || this.isDenyItem(player.getInventory().getItemInOffHand())) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(ChatColor.YELLOW + "对不起，请空手采集");

                            BlockBreakEvent e = new BlockBreakEvent(top, player);
                            if (e.isCancelled()) {
                                return;
                            }
                            top.breakNaturally();
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType().name().endsWith("APRICORN_TREE")) {
            Block aboveBlock = block.getLocation().clone().add(0.0D, 1.0D, 0.0D).getBlock();
            if (aboveBlock != null && aboveBlock.getType() != Material.AIR) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "上方空间不足，无法放置此方块！");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlaceBeLow(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced(); // 获取放置的方块
        Block belowBlock = block.getLocation().clone().add(0.0D, -1.0D, 0.0D).getBlock(); // 获取下方的方块

        // 检查下方方块是否为 APRICORN_TREE
        if (belowBlock != null && belowBlock.getType().name().endsWith("APRICORN_TREE")) {
            event.setCancelled(true); // 取消放置事件
//            event.getPlayer().sendMessage(ChatColor.RED + "你不能将方块放置在树果上方！");
        }
    }



    // 检查物品是否被禁止使用的方法
    private boolean isDenyItem(org.bukkit.inventory.ItemStack item) {
        // 在这里实现对物品的检查逻辑
        return item != null && item.getType() != Material.AIR;
    }

    @EventHandler(
            ignoreCancelled = true,
            priority = EventPriority.HIGHEST
    )
    public void onInteract(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getItem() != null && (event.getItem().getType().name().endsWith("_INCENSE") || event.getItem().getType().name().endsWith("_BURNER"))) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onPlayerTp(PlayerTeleportEvent e){
        /*玩家不可在对战中传送*/
        EventHandlerUtil.PlayerTeleportEvent.unableToTeleportDuringBattle(e);
    }



    @EventHandler
    public void onInteractChest(BlockPlaceEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {

            ArrayList<String> arrayList = new ArrayList<String>();
            arrayList.add("PIXELMON_POKE_CHEST");
            arrayList.add("PIXELMON_ULTRA_CHEST");
            arrayList.add("PIXELMON_BEAST_CHEST");
            arrayList.add("PIXELMON_MASTER_CHEST");
            arrayList.add("PIXELMON_POKE_GIFT");

            Block block = event.getBlock();
            if (block != null) {
                String name = block.getType().name();
                if (arrayList.contains(name)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onRide(VehicleEnterEvent event) {
        if ("PIXELMON_PIXELMON".equals(event.getEntered().getType().name())) {
            event.setCancelled(true);
        }
    }

    public static String parseColor(String str) {
        return str.replace("&", "§");
    }

    @EventHandler
    public void onForgeEvent(ForgeEvent event) {
        if (event.getForgeEvent() instanceof ItemFormChangeEvent) {
            ItemFormChangeEvent e = (ItemFormChangeEvent)event.getForgeEvent();
            EntityPixelmon ep = e.pokemon;

            Method getItemMethod = null;
            try {
                getItemMethod = NBTTagCompound.class.getDeclaredMethod("func_74764_b", String.class); // 确保提供正确的参数类型
            } catch (NoSuchMethodException ex) {

            }

            // 使私有方法可访问
            getItemMethod.setAccessible(true);

            // 假设 ep.getPokemonData().getPersistentData() 返回的是一个 NBTTagCompound 对象
            Object nbtTagCompound = ep.getPokemonData().getPersistentData();

            try {
                // 使用反射调用 func_74764_b 方法，并传递 "FusedPokemon" 作为参数
                boolean hasFusedPokemon = (boolean) getItemMethod.invoke(nbtTagCompound, "FusedPokemon");

                if (!hasFusedPokemon) {
                    Pokemon pokemon1 = e.fusion;
                    Pokemon pokemon2 = e.pokemon.getPokemonData();
                    Player player = Bukkit.getPlayer(e.player.getUniqueID());
                    if (pokemon1.hasSpecFlag("untradeable") ^ pokemon2.hasSpecFlag("untradeable")) {
                        if (Main.sendMap.get(player.getName()) != null && (Main.sendMap.get(player.getName())).equals("是")) {
                            Main.sendMap.remove(player.getName());
                            pokemon1.addSpecFlag("untradeable");
                            pokemon2.addSpecFlag("untradeable");
                            return;
                        }

                        e.setCanceled(true);
                        player.sendMessage(parseColor(Main.main.getConfig().getString("Message.tips")));
                        Main.sendMap.put(player.getName(), "检测");
                    }
                }
            } catch (Exception e1) {

            }


        }

    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onChatEvent(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (Main.sendMap.get(player.getName()) != null && (Main.sendMap.get(player.getName())).equals("检测")) {
            if (event.getMessage().equals("是")) {
                event.setCancelled(true);
                Main.sendMap.put(player.getName(), "是");
            } else {
                Main.sendMap.remove(player.getName());
            }
        }

    }
}
