package red.man10

import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.RegionContainer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

enum class GameStatus {
    OFF,
    JOIN,
    BET,
    FIGHT
}

class BookMakerPlugin: JavaPlugin() {

    val gui = BookMakerGUI().returnGUI(this)
    val gameManager = BookMakerGameManager().returnGameManager(this)
    val listener = BookMakerListener().returnListener(this)
    val configManager = BookMakerConfigManager().returnConfigManager(this)
    var sidebar: BookMakerSidebar? = null
    var data: BookMakerData? = null // = BookMakerData().returnData(this)

    var isLocked = true

    lateinit var vault : VaultManager

    val prefix = "§l[§a§lm§6§lBookMaker§f§l]§r "

    lateinit var worldguard: RegionContainer

    var freezedPlayer = mutableListOf<UUID>()

    override fun onEnable() {
        logger.info("Man10BookMaker Enabled")
        server.pluginManager.registerEvents(listener, this)

        vault = VaultManager()

        worldguard = WorldGuard.getInstance().platform.regionContainer

        configManager.loadConfig(null)

        sidebar = BookMakerSidebar().returnSidebar(this)
        data = BookMakerData().returnData(this)
    }

    override fun onDisable() {
        sidebar!!.removeAll()
        for (gameID in gameManager.runningGames.keys) {
            gameManager.stopGame(gameID)
        }
        // Plugin shutdown logic
        logger.info("Man10BookMaker Disabled")
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (sender is Player) {
            if (args.isEmpty()) {
                if (sender.hasPermission("mp.play")) {
                    if (!isLocked) {
                        gui.openTopMenu(sender)
                    } else {
                        sender.sendMessage(prefix + "ブックメーカーは現在OFFになっています。")
                    }
                } else {
                    sender.sendMessage("権限がありません。")
                }
            } else {
                if (args[0] == "view") {
                    if (gameManager.runningGames[args[1]] == null) {
                        sender.sendMessage(prefix + "ゲームが存在しません。")
                        return true
                    } else {
                        gameManager.viewTeleport(args[1], sender)
                        return true
                    }
                }
                if (args[0] == "return") {
                    if (sender.world.name == "bookmaker") {
                        if (sender.hasPermission("mb.view")) {
                            sender.performCommand("spawn")
                            return true
                        } else {
                            sender.sendMessage(prefix + "権限がありません。")
                            return true
                        }
                    } else {
                        sender.sendMessage(prefix + "あなたは観戦していません。")
                        return true
                    }
                }

                if (args[0] == "open" ){
                    if (!isLocked) {
                        if (args.size == 2) {
                            gameManager.openNewGame(args[1], sender)
                            return true
                        } else {
                            sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                            return true
                        }
                    } else {
                        sender.sendMessage(prefix + "ブックメーカーは現在OFFになっています。")
                    }
                }
                if (args[0] == "help") {
                    showHelp(sender)
                    return true
                }
                if (sender.hasPermission("mb.op")) {
                    when (args[0]) {
                    //OPコマンド
                        "reload" -> {
                            configManager.loadConfig(sender)
                        }
                        "list" -> {
                            sender.sendMessage(prefix + "§6現在" + gameManager.loadedGames.size + "個のゲームがロードされています。")
                            for (item in gameManager.loadedGames) {
                                sender.sendMessage(prefix + item.key + " (" + item.value.gameName + ")")
                            }
                        }
                        "info" -> {
                            if (args.size == 2) {
                                if (gameManager.loadedGames[args[1]] == null) {
                                    sender.sendMessage(prefix + "指定されたゲームは存在しません。")
                                } else {
                                    //FLAG
                                    val checkingGame: Game = gameManager.loadedGames[args[1]]!!
                                    sender.sendMessage(prefix + "§6" + args[1] + "のデーター")
                                    sender.sendMessage(prefix + "§7ゲーム名: " + checkingGame.gameName)
                                    sender.sendMessage(prefix + "§7GUI表示アイテム: " + checkingGame.item.toString())
                                    sender.sendMessage(prefix + "§7プレイヤー人数: " + checkingGame.playerNumber)
                                    sender.sendMessage(prefix + "§7参加費: " + checkingGame.joinFee)
                                    sender.sendMessage(prefix + "§7税率: " + checkingGame.tax.toString())
                                    sender.sendMessage(prefix + "§7賞金率: " + checkingGame.prize.toString())
                                    sender.sendMessage(prefix + "§7ステータス: " + checkingGame.status.toString())
                                    sender.sendMessage(prefix + "§7参加登録者一覧: " + checkingGame.candidates.toString())
                                    sender.sendMessage(prefix + "§7ベット一覧: " + checkingGame.players.toString())
                                }
                            } else {
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                            }
                        }
                        "push" -> {
                            if (args.size == 2) {
                                gameManager.pushPhase(args[1], sender)
                            } else {
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                            }
                        }
                        "end" -> {
                            if (args.size == 3) {
                                if (gameManager.runningGames[args[1]] != null) {
                                    if (Bukkit.getPlayer(args[2]) != null) {
                                        Bukkit.getPlayer(args[2])?.let { gameManager.endGame(args[1], it.uniqueId) }
                                    } else {
                                        if (gameManager.UUIDMap.keys.contains(gameManager.runningGames[args[1]]!!.players.keys.toList()[0])) {
                                            if (args[2].toIntOrNull() == null) {
                                                sender.sendMessage("$prefix§4§lERROR: §f§l選択肢の番号を入力してください。")
                                            } else {
                                                if (args[2].toInt() == 1 || args[2].toInt() == 2) {
                                                    gameManager.endGame(args[1], gameManager.runningGames[args[1]]!!.players.keys.toList()[args[2].toInt() - 1])
                                                }
                                            }
                                        } else {
                                            sender.sendMessage("$prefix§4§lERROR: §f§lプレイヤーが存在しません。")
                                        }
                                    }
                                } else {
                                    sender.sendMessage("$prefix§4§lERROR: §f§l指定されたゲームが存在しません。")
                                }
                            } else {
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                            }
                        }

                        "forcestop" -> {
                            if (args.size == 2) {
                                if (gameManager.runningGames[args[1]] != null) {
                                    Bukkit.broadcastMessage(prefix + "§l運営によって§6§l「" + gameManager.runningGames[args[1]]!!.gameName + "」§f§lが停止されました。")
                                    gameManager.stopGame(args[1])
                                } else {
                                    sender.sendMessage("$prefix§4§lERROR: §f§l指定されたゲームは存在しません")
                                }
                            } else {
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                            }
                        }
                        "setfighterspawn" -> {
                            if (args.size == 2) {
                                if (gameManager.loadedGames[args[1]] != null) {
                                    gameManager.setFighterSpawnPoint(args[1], sender)
                                }
                            }
                        }
                        "setviewerspawn" -> {
                            if (args.size == 2) {
                                if (gameManager.loadedGames[args[1]] != null) {
                                    gameManager.setViewerSpawnPoint(args[1], sender)
                                }
                            }
                        }
                        "off" -> {
                            isLocked = true
                            sender.sendMessage(prefix + "OFFにしました。")
                        }
                        "on" -> {
                            isLocked = false
                            sender.sendMessage(prefix + "ONにしました。")
                        }

                        "ask" -> {
                            if (args.size == 5) {
                                if (gameManager.loadedGames[args[1]] == null && gameManager.runningGames[args[1]] == null) {
                                    gameManager.openNewQ(args[1], args[2], args[3], args[4])
                                } else {
                                    sender.sendMessage(prefix + "ゲームがすでに存在します。")
                                }
                            } else {
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                            }
                        }
                        else -> {
                            sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                        }
                    }
                } else {
                    sender.sendMessage(prefix + "権限がありません。")
                }
            }
        } else {
            sender.sendMessage(prefix + "コンソールからコマンドは実行できません。")
        }
        return true
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§f§l=====( §a§lm§6§lBookMaker§f§l )=====")
        sender.sendMessage("§6《データー管理系》")
        sender.sendMessage("§a/mb reload §7config.ymlとregionをリロードする")
        sender.sendMessage("§a/mb list §7登録中のゲームを表示する")
        sender.sendMessage("§a/mb info <ゲームid> §7指定したゲームの情報を表示する")
        sender.sendMessage(" ")
        sender.sendMessage("§6《ゲーム管理系》")
        sender.sendMessage("§a/mb open <ゲームid> §7指定したゲームを開く (一般人使用可能)")
        sender.sendMessage("§a/mb ask <新ゲームid> <質問> <選択肢1> <選択肢2> §7ASKモードの試合をオープンする。")
        sender.sendMessage("§a/mb forcestop <ゲームid> §7指定したゲームを強制終了する")
        sender.sendMessage("§a/mb push <ゲームid> §7指定したゲームを次のフェーズに進ませる")
        sender.sendMessage("§a/mb end <ゲームid> <勝者> §7試合中のゲームを終了させる")
        sender.sendMessage("§a/mb off §7ブックメーカーをロックする。")
        sender.sendMessage("§a/mb on §7ブックメーカーをアンロックする。")
        sender.sendMessage("§a/mb view <ゲームid> §7観戦場所にtpする (一般人使用可能)")
        sender.sendMessage("§a/mb view <ゲームid> §7ブックメーカーロビーにtpする (一般人使用可能)")
        sender.sendMessage("")
        sender.sendMessage("§6《ポイント管理系》")
        sender.sendMessage("§a/mb setfighterspawn <ゲームid> §7立っているところを選手のスポーンポイントにする")
        sender.sendMessage("§a/mb setviewerspawn <ゲームid> §7立っているところを選手のスポーンポイントにする")
        sender.sendMessage(" ")
        sender.sendMessage("§6Ver 1.0  Made by Shupro")
        sender.sendMessage("§f§l=====================")
    }

//    fun fixTpBug(tpedPlayer: Player) {
//        for (player in Bukkit.getWorld(worldName).getPlayers()) {
//            tpedPlayer.hidePlayer(player)
//            player.hidePlayer(tpedPlayer)
//        }
//        for (player in Bukkit.getWorld(worldName).getPlayers()) {
//            tpedPlayer.showPlayer(player)
//            player.showPlayer(tpedPlayer)
//        }
//    }
}
