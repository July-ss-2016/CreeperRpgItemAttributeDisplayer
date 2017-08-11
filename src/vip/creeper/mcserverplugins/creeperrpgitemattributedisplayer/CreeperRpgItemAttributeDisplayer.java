package vip.creeper.mcserverplugins.creeperrpgitemattributedisplayer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by July_ on 2017/8/3.
 */
public class CreeperRpgItemAttributeDisplayer extends JavaPlugin implements Listener {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String HEAD_MSG = "§a[Crad] §b";
    private static CreeperRpgItemAttributeDisplayer instance;
    private Logger logger = getLogger();
    private List<String> disabledPlayers = new ArrayList<>();
    private HashMap<String, List<String>> items = new HashMap<>();

    public void onEnable() {
        instance = this;

        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("criad").setExecutor(this);
        logger.info("初始化完毕!");
    }

    private void loadConfig() {
        items.clear();
        File itemFolder = new File(getDataFolder().getAbsolutePath() + File.separator + "items");
        File[] itemFolderFiles = itemFolder.listFiles();

        if (!itemFolder.exists()) {
            if (!itemFolder.mkdirs()) {
                logger.info("创建目录失败!");
                setEnabled(false);
                return;
            }
        }

        if (itemFolderFiles == null || itemFolderFiles.length == 0) {
            copySrcFile("items_0_16.yml", itemFolder + File.separator + "items_0_16.yml");
            itemFolderFiles = itemFolder.listFiles();
        }

        assert itemFolderFiles != null;
        for (File file : itemFolderFiles) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            for (Map.Entry<String, Object> entry : yml.getConfigurationSection("Items").getValues(true).entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (!(value instanceof  List)) {
                    logger.info("配置类型错误!");
                    continue;
                }

                logger.info("物品 = " + key + " 被载入.");
                //noinspection unchecked
                items.put(key, (List<String>) value);
            }
        }
    }

    public boolean onCommand(CommandSender cs, Command cmd, String lable, String[] args) {
        if (cs.hasPermission("CreeperRpgItemAttributeDisplayer.use")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                loadConfig();
                cs.sendMessage("配置重载完毕!");
                return true;
            }
        }

        if (cs instanceof  Player) {
            Player player = (Player) cs;
            String playerName = player.getName();

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("off")) {

                    if (!disabledPlayers.contains(playerName)) {
                        disabledPlayers.add(playerName);
                    }

                    player.sendMessage(HEAD_MSG + "已关闭属性提示功能,输入 §e/crad on §e开启.");
                    return true;
                }

                if (args[0].equalsIgnoreCase("on")) {
                    disabledPlayers.remove(playerName);

                    player.sendMessage(HEAD_MSG + "已开启属性提示功能,输入 §e/crad off §e关闭.");
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String itemCode = getItemCode(player.getItemInHand());
        org.bukkit.event.block.Action action = event.getAction();

        if (itemCode != null && player.isSneaking() && (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) && !disabledPlayers.contains(player.getName())) {
            if (items.containsKey(itemCode)) {
                player.sendMessage("");

                for (String str : items.get(itemCode)) {
                    player.sendMessage(str);
                }

                player.sendMessage("");
                return;
            }

            player.sendMessage(HEAD_MSG + "§c该物品属性尚未填写,请等待管理员更新!");
        }
    }

    @SuppressWarnings("unused")
    public static CreeperRpgItemAttributeDisplayer getInstance() {
        return instance;
    }

    @SuppressWarnings("unused")
    public List<String> getItemAttributeInformations(final String itemCode) {
        return this.items.get(itemCode);
    }

    private String getItemCode(final ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        List<String> lores = item.getItemMeta().getLore();

        if (lores == null || lores.size() == 1) {
            return null;
        }

        String firstLore = lores.get(0);

        if (firstLore.startsWith("§7- §f代码 §b> §f")) {
            return firstLore.replace("§7- §f代码 §b> §f", "");
        }

        return null;

    }

    //从jar包复制文件
    @SuppressWarnings("UnusedReturnValue")
    private boolean copySrcFile(@SuppressWarnings("SameParameterValue") final String srcFilePath, final String localFilePath) {
        try {
            InputStream is = instance.getClass().getClassLoader().getResourceAsStream(srcFilePath); //读取文件内容
            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            String lineText;
            StringBuilder sb = new StringBuilder();

            while ((lineText = bufferedReader.readLine()) != null){
                sb.append(lineText).append(LINE_SEPARATOR);
            }

            bufferedReader.close();
            reader.close();
            return writeFile(localFilePath, sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    //写文件
    private boolean writeFile(final String path, final String data) {
        File file = new File(path);

        try {
            FileWriter fw = new FileWriter(file);

            if (!file.exists()) {
                if (!file.createNewFile()) {
                    return false;
                }
            }

            fw.write(data);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
