package red.man10

import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.RegionContainer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.floor

enum class GameStatus {
    OFF,
    JOIN,
    BET,
    FIGHT
}

enum class MBGameMode {
    NORMAL,
    WHITELIST,
    PRO,
    FREE
}

class BookMakerPlugin: JavaPlugin() {

    val gui = BookMakerGUI().returnGUI(this)
    val gameManager = BookMakerGameManager().returnGameManager(this)
    val listener = BookMakerListener().returnListener(this)
    private val configManager = BookMakerConfigManager().returnConfigManager(this)
    val createGameManager = HashMap<UUID,Int>()
    val createGameManagerData = HashMap<UUID,Pair<String,Game>>()
    lateinit var sidebar: BookMakerSidebar
    lateinit var data: BookMakerData // = BookMakerData().returnData(this)

    var isLocked = true

    var mode = MBGameMode.NORMAL

    val whitelist = ArrayList<UUID>()

    lateinit var vault : VaultManager

    fun worldMsg(message : String){
        (Bukkit.getWorld("bookmaker")?:return).players.forEach {
            it.sendMessage(message)
        }
    }

    var lobbyLocation : Location? = null

    var limitGame = 99

    val prefix = "§l[§a§lm§6§lBookMaker§f§l]§r "

    lateinit var worldguard: RegionContainer

    var freezedPlayer = mutableListOf<UUID>()

    override fun onEnable() {
        logger.info("Man10BookMaker Enabled")
        server.pluginManager.registerEvents(listener, this)

        vault = VaultManager()

        worldguard = WorldGuard.getInstance().platform.regionContainer

        configManager.loadConfig(null)

        isLocked = !config.getBoolean("enable")

        lobbyLocation = config.getLocation("lobbyLocation")

        limitGame = config.getInt("limitGame",99)

        config.getStringList("whitelist").forEach {
            whitelist.add(UUID.fromString(it))
        }

        sidebar = BookMakerSidebar().returnSidebar(this)
        data = BookMakerData().returnData(this)
    }

    override fun onDisable() {
        sidebar.removeAll()
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
                    if (args.size == 1){
                        sender.sendMessage("$prefix/mb view <ゲーム名>")
                    }
                    if (gameManager.runningGames[args[1]] == null) {
                        sender.sendMessage(prefix + "ゲームが存在しません。")
                    } else {
                        gameManager.viewTeleport(args[1], sender)
                    }
                    return true
                }
                if (args[0] == "return") {
                    if (sender.world.name == "bookmaker") {
                        if (sender.hasPermission("mb.view")) {
                            sender.teleport(lobbyLocation?:return true)
                        } else {
                            sender.sendMessage(prefix + "権限がありません。")
                        }
                    } else {
                        sender.sendMessage(prefix + "あなたは観戦していません。")
                    }
                    return true
                }

                if (args[0] == "open"){
                    if (!isLocked) {
                        if (args.size == 2) {
                            if (mode == MBGameMode.WHITELIST){
                                if (whitelist.contains(sender.uniqueId) || sender.isOp){
                                    gameManager.openNewGame(args[1], sender)
                                } else {
                                    sender.sendMessage(prefix + "ホワイトリストに入っていません。")
                                    return true
                                }
                            } else {
                                gameManager.openNewGame(args[1], sender)
                            }
                        } else {
                            sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                        }
                    } else {
                        sender.sendMessage(prefix + "ブックメーカーは現在OFFになっています。")
                    }
                    return true
                }
                if (args[0] == "help") {
                    showHelp(sender)
                    return true
                }

                if (args[0] == "log"){
                    Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
                        val log = data.getLog(sender.uniqueId)
                        if (log == null){
                            sender.sendMessage(prefix + "データの取得に失敗しました。")
                            return@Runnable
                        }

                        sender.sendMessage(prefix + "§e試合数：${log.first}")
                        sender.sendMessage(prefix + "§a勝利数：${log.second}")
                        sender.sendMessage(prefix + "§b敗北数：${log.first-log.second}")
                        sender.sendMessage(prefix + "§d勝率：${floor((log.second.toDouble()/log.first.toDouble()) * 100)}%")
                        return@Runnable
                    })
                    return true
                }

                if (args[0] == "ranking"){
                    if (args.size != 2){
                        sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                        return true
                    }
                    Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
                        val ranking = data.getBestRecordRanking(args[1])
                        if (ranking.isEmpty()){
                            sender.sendMessage(prefix + "データの取得に失敗しました。")
                            return@Runnable
                        }

                        sender.sendMessage("§a${gameManager.loadedGames[args[1]]?.gameName}のレコードランキング")
                        for ((i,rank) in ranking.withIndex()){
                            sender.sendMessage("§7§l${i+1}.§b${rank.key}：§e${rank.value}秒")
                        }
                    })
                    return true


                }

                if (sender.hasPermission("mb.op")) {
                    when (args[0]) {
                    //OPコマンド
                        "creategame"->{
                            createGameManager[sender.uniqueId] = 0
                            createGameManagerData[sender.uniqueId] = Pair("", Game())
                            sender.sendMessage(prefix + "ゲーム名を入力してください。")
                            sender.sendMessage(prefix + "取り消す場合は「cancel」と入力してください。")
                        }

                        "removegame"->{
                            if (args.size != 2){
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                                return true
                            }
                            if (!config.isSet(args[1])){
                                sender.sendMessage(prefix + "ゲームが存在しません。")
                                return true
                            }

                            config.set(args[1],null)
                            saveConfig()
                            sender.sendMessage(prefix + "ゲームを削除しました。")
                            return true
                        }

                        "addwhitelist","awl","addlist"->{
                            if (args.size != 2){
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                                return true
                            }

                            val player = Bukkit.getPlayer(args[1])
                            if (player == null){
                                sender.sendMessage(prefix + "プレイヤーが存在しません オンラインのプレイヤーを指定してください")
                                return true
                            }

                            if (whitelist.contains(player.uniqueId)){
                                sender.sendMessage(prefix + "既に追加済みです")
                                return true
                            }

                            whitelist.add(player.uniqueId)
                            val list = config.getStringList("whitelist")
                            list.add(player.uniqueId.toString())
                            config.set("whitelist",list)
                            saveConfig()
                            sender.sendMessage(prefix + "${args[1]}を追加しました")

                        }

                        "removewhitelist","rwl","removelist"->{
                            if (args.size != 2){
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                                return true
                            }

                            val player = Bukkit.getOfflinePlayer(args[1])
                            if (player.name == null){
                                sender.sendMessage(prefix + "プレイヤーが存在しません")
                                return true
                            }

                            if (!whitelist.contains(player.uniqueId)){
                                sender.sendMessage(prefix + "ホワイトリストに追加されていません！")
                                return true
                            }

                            whitelist.remove(player.uniqueId)
                            val list = config.getStringList("whitelist")
                            list.remove(player.uniqueId.toString())
                            config.set("whitelist",list)
                            saveConfig()
                            sender.sendMessage(prefix + "${args[1]}を削除しました")
                        }

                        "whitelist","wl"->{
                            sender.sendMessage(prefix + "ホワイトリストに入っているプレイヤー")
                            whitelist.forEach {
                                sender.sendMessage(Bukkit.getOfflinePlayer(it).name)
                            }
                        }

                        "limit"->{
                            if (args.size != 2){
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                                return true
                            }

                            val limit = args[0].toIntOrNull()

                            if (limit == null){
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                                return true
                            }

                            config.set("limitGame",limit)
                            saveConfig()

                            limitGame = limit

                            sender.sendMessage(prefix + "§b最大ゲーム開催数を${limit}にしました")
                            return true

                        }

                        "mode"->{
                            if (args.size != 2){
                                sender.sendMessage(prefix + "コマンドの使用方法が間違っています。/mb help")
                                return true
                            }
                            if (!listOf("normal","whitelist").contains(args[1])){
                                sender.sendMessage(prefix + "モードが存在しません")
                            }
                            val mode = when(args[1]){
                                "normal"-> MBGameMode.NORMAL
                                "whitelist"-> MBGameMode.WHITELIST
                                else -> MBGameMode.NORMAL
                            }

                            this.mode = mode
                            for (game in gameManager.runningGames){
                                gameManager.stopGame(game.key)
                            }
                            sender.sendMessage(prefix + "モードを${args[1]}にしました")
                        }

                        "reload" -> {
                            configManager.loadConfig(sender)
                        }
                        "setlobby"->{
                            config.set("lobbyLocation",sender.location)
                            lobbyLocation = sender.location
                            saveConfig()
                            sender.sendMessage("$prefix§aロビーを設定しました")
                            return true
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
                                        if (gameManager.uuidMap.keys.contains(gameManager.runningGames[args[1]]!!.players.keys.toList()[0])) {
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
                            config.set("enable",false)
                            saveConfig()
                            sender.sendMessage(prefix + "OFFにしました。")
                        }
                        "on" -> {
                            isLocked = false
                            config.set("enable",true)
                            saveConfig()
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
        sender.sendMessage("§a/mb return §7ブックメーカーロビーにtpする (一般人使用可能)")
        sender.sendMessage("§a/mb mode (normal or whitelist) §7ブックメーカーのモードを変える")
        sender.sendMessage("§a/mb awl <プレイヤー名> §7ホワイトリストにプレイヤーを追加する")
        sender.sendMessage("§a/mb rwl <プレイヤー名> §7ホワイトリストからプレイヤーを削除する")
        sender.sendMessage("§a/mb whitelist §7ホワイトリストにいるプレイヤーを確認する")
        sender.sendMessage("§a/mb limit <数> §7ゲーム同時開催数を制限する")
        sender.sendMessage("§a/mb creategame §7ゲームを作成する")
        sender.sendMessage("§a/mb removegame <ゲームid> §7ゲームを削除する")
        sender.sendMessage("")
        sender.sendMessage("§6《ポイント管理系》")
        sender.sendMessage("§a/mb setfighterspawn <ゲームid> §7立っているところを選手のスポーンポイントにする")
        sender.sendMessage("§a/mb setviewerspawn <ゲームid> §7立っているところを観戦者のスポーンポイントにする")
        sender.sendMessage("§a/mb setlobby §7立っているところをロビーのスポーン位置にする")
        sender.sendMessage(" ")
        sender.sendMessage("§6《その他》")
        sender.sendMessage("§a/mb log §7試合数、勝利数、敗北数、勝率を見る")
        sender.sendMessage("§a/mb ranking <ゲームid> レコードのランキングを見る")
        sender.sendMessage(" ")
        sender.sendMessage("§6Ver 2.1  Made by Shupro (Refactor tororo_1066)")
        sender.sendMessage("§f§l=====================")
    }

}
