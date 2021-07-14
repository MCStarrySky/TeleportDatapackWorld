package me.mical.teleportdatapackworld;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public final class TeleportDatapackWorld extends JavaPlugin {

    private static List<String> allowWorlds;
    private static String pn;
    private static long timestamp;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        reloadConfig();

        new WorldCreator("world_old").environment(World.Environment.NORMAL).type(WorldType.NORMAL).createWorld();
        Bukkit.getLogger().info(prefix(ChatColor.LIGHT_PURPLE) + "尝试加载老世界.");

        new BukkitRunnable() {
            int time = 60;

            @Override
            public void run() {
                if (getEnd() >= timestamp) {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        final BaseComponent[] baseComponents = TextComponent.fromLegacyText(prefix(ChatColor.RED) + "旧主世界还有" + time + "秒就要从服务器中卸载了,请及时离开! (点击离开)");
                        final TextComponent text = new TextComponent();
                        Arrays.stream(baseComponents).forEach(text::addExtra);
                        text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "old"));
                        player.spigot().sendMessage(text);
                    });
                    time--;
                    if (time == 0) {
                        Bukkit.getOnlinePlayers().forEach(player -> {
                            if (player.getWorld().getName().equalsIgnoreCase("world_old")) {
                                final World world = Bukkit.getWorld("world");
                                assert world != null;
                                player.teleport(world.getSpawnLocation());
                                player.sendMessage(prefix(ChatColor.GREEN) + "已为您传送到主世界!");
                            }
                        });
                        Bukkit.unloadWorld("world_old", true);
                        Bukkit.getLogger().info(prefix(ChatColor.GREEN) + "已卸载老世界.");
                        Bukkit.broadcastMessage(prefix(ChatColor.GREEN) + "已卸载老世界.");
                        cancel();
                    }
                }
            }
        }.runTaskTimer(this, 0, 20L);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        allowWorlds = getConfig().getStringList("AllowWorlds");
        pn = getConfig().getString("PluginName");
        timestamp = getConfig().getLong("Timestamp");
        if (timestamp == -1) {
            timestamp = System.currentTimeMillis();
            getConfig().set("Timestamp", timestamp);
            saveConfig();
            Bukkit.getLogger().info(prefix(ChatColor.YELLOW) + "检测到未设定/已重置初始时间, 已设定为当前时间, 若想重置请设置 Timestamp 为 -1.");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("teleportdatapackworld")) {
            switch (args.length) {
                case 0:
                    sendHelp(sender);
                    break;
                case 1:
                    if (args[0].equalsIgnoreCase("reload")) {
                        if (sender.hasPermission("TeleportDatapackWorld.reload")) {
                            reloadConfig();
                            sender.sendMessage(prefix(ChatColor.GREEN) + "重载配置文件成功.");
                        } else {
                            sender.sendMessage(prefix(ChatColor.YELLOW) + "您没有权限.");
                        }
                    } else {
                        sender.sendMessage(prefix(ChatColor.YELLOW) + "未知命令, 请检查您的命令拼写是否正确.");
                    }
                    break;
                default:
                    sender.sendMessage(prefix(ChatColor.YELLOW) + "参数长度错误!");
                    sendHelp(sender);
                    break;
            }
            return true;
        } else {
            Player p = null;
            if (sender instanceof Player) {
                p = (Player) sender;
            }
            if (p == null) {
                sender.sendMessage(prefix(ChatColor.YELLOW) + "该命令仅玩家可执行.");
                return true;
            }
            if (command.getName().equalsIgnoreCase("new")) {
                if (checkWorld(p)) {
                    final World newWorld = Bukkit.getWorld("world");
                    assert newWorld != null;
                    p.teleport(newWorld.getSpawnLocation());
                    sender.sendMessage(prefix(ChatColor.GREEN) + "您已成功传送到新世界.");
                    return true;
                }
            } else if (command.getName().equalsIgnoreCase("old")) {
                if (checkWorld(p)) {
                    final World oldWorld = Bukkit.getWorld("world_old");
                    assert oldWorld != null;
                    p.teleport(oldWorld.getSpawnLocation());
                    sender.sendMessage(prefix(ChatColor.GREEN) + "您已成功传送到旧世界.");
                    return true;
                }
            }
        }
        return super.onCommand(sender, command, label, args);
    }

    private static boolean checkWorld(Player p) {
        if (allowWorlds.contains(p.getWorld().getName())) {
            return true;
        } else {
            p.sendMessage(prefix(ChatColor.YELLOW) + "您不在许可的世界内, 无法随意传送, 您只能在旧主世界新主世界间随意传送.");
            return false;
        }
    }

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix(ChatColor.GREEN) + "/teleportdatapackworld reload -- 重载插件");
        sender.sendMessage(prefix(ChatColor.GREEN) + "/new -- 传送至洞穴与山崖世界");
        sender.sendMessage(prefix(ChatColor.GREEN) + "/old -- 传送至普通世界 (与 + " + getEndText() + " 到期)");
    }

    private static String prefix(ChatColor level) {
        return ChatColor.BLUE + "" + ChatColor.BOLD + pn + level + ">> " + ChatColor.RESET;
    }

    private static long getEnd() {
        return timestamp + 604800800;
    }

    private static String getEndText() {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        final Date date = new Date(getEnd());
        return format.format(date);
    }
}
