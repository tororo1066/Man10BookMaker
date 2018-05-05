//package red.man10
//
//import org.bukkit.Bukkit
//import org.bukkit.command.CommandSender
//import org.bukkit.configuration.file.FileConfiguration
//import org.bukkit.entity.Player
//
//class BookMakerConfigManager {
//
//    companion object {
//        var pl: BookMakerPlugin? = null
//    }
//
//    fun returnConfigManager(plugin: BookMakerPlugin) : BookMakerConfigManager{
//        pl = plugin
//        return BookMakerConfigManager()
//    }
//
//    fun loadConfig(sender: CommandSender?){
//        pl!!.saveDefaultConfig()
//        pl!!.reloadConfig()
//        pl!!.saveDefaultConfig()
//        val config = pl!!.config
//
//        if (!config.getKeys(true).isEmpty()) {
//            Bukkit.broadcastMessage(config.getKeys(false).toString())
//
//            if (sender != null) {
//                pl!!.gameManager.setUpGames(config, (sender as Player))
//                sender.sendMessage(pl!!.prefix + "config.ymlのゲームが読み込まれました。")
//            } else {
//                pl!!.gameManager.setUpGames(config, null)
//                print(pl!!.prefix + "config.ymlのゲームが読み込まれました。")
//            }
//
//        } else {
//
//            if (sender != null) {
//                sender.sendMessage(pl!!.prefix + "config.ymlにゲームがありません！")
//            } else {
//                print(pl!!.prefix + "config.ymlにゲームがありません！")
//            }
//
//        }
//    }
//
//}