package red.man10

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

enum class GameStatus(val rawValue :Int)  {
    OFF(1),
    JOIN(2),
    BET(3),
    FIGHT(4)
}

class BookMakerPlugin: JavaPlugin() {

    val gui = BookMakerGUI().returnGUI(this)
    val gameManager = BookMakerGameManager().returnGameManager(this)
    val listener = BookMakerListener().returnListener(this)
    val configManager = BookMakerConfigManager().returnConfigManager(this)

    var vault: VaultManager? = null

    val prefix = "§l[§a§lm§6§lBookMaker§f§l]§r "

    override fun onEnable() {
        logger.info("Man10BookMaker Enabled")
        server.pluginManager.registerEvents(listener, this)

        vault = VaultManager(this)

        configManager.loadConfig(null)
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("Man10BookMaker Disabled")
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (sender is Player) {
            if (args.isEmpty()) {
                gui.openGUI(sender)
            } else {
                when (args[0]) {

                //OPコマンド
                    "reload" -> {
                        configManager.loadConfig(sender)
                    }
                    "open" -> {
                        if (args.size == 2) {
                            gameManager.openNewGame(args[1], sender)
                        } else {
                            sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                        }
                    }
                    "list" -> {
                        sender.sendMessage(prefix + "§6現在" + gameManager.loadedGames.size + "個のゲームがロードされています。")
                        for (item in gameManager.loadedGames) {
                            sender.sendMessage(prefix + item.key + " (" + item.value.gameName + ")")
                        }
                    }
                    "info" -> {
                        if (args.size != 1) {
                            if (gameManager.loadedGames[args[1]] == null) {
                                sender.sendMessage(prefix + "指定されたゲームは存在しません。")
                            } else {
                                var checkingGame: Game = gameManager.loadedGames[args[1]]!!
                                sender.sendMessage(prefix + "")
                                sender.sendMessage(prefix + "ゲーム名: " + checkingGame.gameName)
                                sender.sendMessage(prefix + "GUI表示アイテム: " + checkingGame.item.toString())
                                sender.sendMessage(prefix + "プレイヤー人数: " + checkingGame.playerNumber)
                                sender.sendMessage(prefix + "ステータス: " + checkingGame.status.toString())
                            }
                        } else {
                            sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                        }
                    }
                    "help" -> {
                        showHelp(sender)
                    }


                    else -> {
                        sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                    }

                }
            }
        } else {
            sender.sendMessage(prefix + "コンソールからコマンドは実行できません。")
        }
        return true
    }

    fun showHelp(sender: CommandSender) {
        sender.sendMessage("§f§l=====( §a§lm§6§lBookMaker§f§l )=====")
        sender.sendMessage("§a§l/mb list §r登録中のゲームを表示する")

        sender.sendMessage("/mb create [game] ゲーム作成する")
        sender.sendMessage("===========[プレーヤー管理]===================")
        sender.sendMessage("/mb register [name] ゲームにユーザーを登録する")
        sender.sendMessage("/mb unregister [name] ゲームからユーザーを削除する")
        sender.sendMessage("/mb open ゲームを開催し、BETをうつける")
        sender.sendMessage("/mb start ゲームを開始する")
        sender.sendMessage("§f§l=====================")
    }
}
