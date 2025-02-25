package red.man10

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.*
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.util.*
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.math.RoundingMode
import kotlin.math.floor


//FLAG
data class Game(var gameName: String,
                var item: Material,
                var playerNumber: Int,
                var joinFee: Double,
                var tax: Double,
                var prize: Double,
                var startingLocation: Location?,
                var viewingLocation: Location?,
                var duration: MutableList<Int>,
                var status: GameStatus,
                var players:  MutableMap<UUID, MutableList<Bet>>,
                var candidates: MutableList<UUID>,
                var gameTimer: Double,
                var virtualBet : Int,
                var commands : MutableList<String>){
    constructor() : this("",Material.STONE,0,0.0,0.0,0.0,null,null, mutableListOf(),GameStatus.OFF, mutableMapOf(), mutableListOf(),0.0,0,
        mutableListOf())
}

data class Bet(val playerUUID: UUID,
               val price: Double)

class BookMakerGameManager {

    var loadedGames = mutableMapOf<String, Game>()

    var runningGames = mutableMapOf<String, Game>()

    var uuidMap = mutableMapOf<UUID, String>()

    val console = Bukkit.getServer().consoleSender

    val xDifferenences = mutableListOf(-1, 0, 1, -1, 0, 1, -1, 0, 1, -1, 0, 1)
    val zDifferenences = mutableListOf(0, 0, 0, 1, -1, 1, 0, 0, 0, 1, -1, 1)

    companion object {
        lateinit var pl: BookMakerPlugin
    }

    //クラス接続
    fun returnGameManager(plugin: BookMakerPlugin) : BookMakerGameManager{
        pl = plugin
        return BookMakerGameManager()
    }

    //コンフィグロード
    fun setUpGames(config: FileConfiguration, p: Player?) {
        val newGames = mutableMapOf<String, Game>()
        val keys = config.getKeys(false)

        pl.listener.hasBMWorld = (Bukkit.getWorld("bookmaker") != null)

        for (gameKey in keys) {
            if (!listOf("mysql","lobbyLocation","enable","whitelist","limitGame","blacklist").contains(gameKey)) {
                //gameに不備がないか確認
                //FLAG
                if (config.getKeys(true).containsAll(listOf("$gameKey.name", "$gameKey.playerNumber", "$gameKey.item", "$gameKey.joinFee", "$gameKey.tax", "$gameKey.prize","$gameKey.virtualBet"))) {

                    //itemが存在するアイテムかの確認
                    var gameItem: Material = Material.STONE
                    try {
                        gameItem = config.getString("$gameKey.item")?.let { Material.valueOf(it) }!!
                    } catch (e: Exception) {
                        //コンソール・プレイヤー分岐
                        if (p == null) {
                            print(pl.prefix + "§e§lWARNING: §r§fゲーム「" + gameKey + "」のitemが存在しないアイテムです。")
                        } else {
                            p.sendMessage(pl.prefix + "§e§lWARNING: §r§fゲーム「" + gameKey + "」のitemが存在しないアイテムです。")
                        }
                    }



                    var startLocation: Location? = null
                    if (config.getKeys(true).containsAll(listOf("$gameKey.x", "$gameKey.y", "$gameKey.x","$gameKey.yaw","$gameKey.pitch"))) {
                        if (pl.listener.hasBMWorld) {
                            startLocation = Location(
                                Bukkit.getWorld("bookmaker"),
                                config.getDouble("$gameKey.x"),
                                config.getDouble("$gameKey.y"),
                                config.getDouble("$gameKey.z"),
                                config.getInt("$gameKey.yaw").toFloat(),
                                config.getInt("$gameKey.pitch").toFloat())
                        }
                    }

                    var durationList = mutableListOf<Int>()
                    if (config.getKeys(true).containsAll(listOf("$gameKey.time1", "$gameKey.time2", "$gameKey.time3"))) {
                        durationList.add(config.getInt("$gameKey.time1"))
                        durationList.add(config.getInt("$gameKey.time2"))
                        durationList.add(config.getInt("$gameKey.time3"))
                    } else {
                        durationList = mutableListOf(120, 120, 120)
                    }


                    var viewLocation: Location? = null
                    if (config.getKeys(true).containsAll(listOf("$gameKey.vx", "$gameKey.vy", "$gameKey.vx"))) {
                        if (pl.listener.hasBMWorld) {
                            viewLocation = Location(
                                Bukkit.getWorld("bookmaker"),
                                config.getDouble("$gameKey.vx"),
                                config.getDouble("$gameKey.vy"),
                                config.getDouble("$gameKey.vz"),
                                config.getInt("$gameKey.vyaw").toFloat(),
                                config.getInt("$gameKey.vpitch").toFloat())
                        }
                    }

                    //仮配列にゲームを追加
                    if (loadedGames[gameKey] == null) {
                        //新しいゲーム
                        config.getString("$gameKey.name")?.let {
                            Game(
                                it,
                                gameItem,
                                config.getInt("$gameKey.playerNumber"),
                                config.getInt("$gameKey.joinFee").toDouble(),
                                config.getDouble("$gameKey.tax"),
                                config.getDouble("$gameKey.prize"),
                                startLocation,
                                viewLocation,
                                durationList,
                                GameStatus.OFF,
                                mutableMapOf(),
                                mutableListOf(),
                                0.0,
                                config.getInt("$gameKey.virtualBet"),
                                config.getStringList("$gameKey.commands")
                            )
                        }?.let {
                            newGames.put(
                                gameKey,
                                //FLAG
                                it
                            )
                        }
                    } else {
                        //元々あったゲームの改変
                        //FLAG
                        val changingGame = loadedGames[gameKey]!!
                        changingGame.gameName = config.getString("$gameKey.name").toString()
                        changingGame.item = gameItem
                        changingGame.playerNumber = config.getInt("$gameKey.playerNumber")
                        changingGame.joinFee = config.getInt("$gameKey.joinFee").toDouble()
                        changingGame.tax = config.getDouble("$gameKey.tax")
                        changingGame.prize = config.getDouble("$gameKey.prize")
                        changingGame.startingLocation = startLocation
                        changingGame.viewingLocation = viewLocation
                        changingGame.duration = durationList
                        changingGame.virtualBet = config.getInt("$gameKey.virtualBet")
                        changingGame.commands = config.getStringList("$gameKey.commands")
                        newGames[gameKey] = changingGame
                        print(changingGame)
                    }
                } else {
                    //configに不備がある
                    if (p == null) {
                        print(pl.prefix + "§4§lERROR: §f§lゲーム「" + gameKey + "」のconfigに不備があります。")
                    } else {
                        p.sendMessage(pl.prefix + "§4§lERROR: §f§lゲーム「" + gameKey + "」のconfigに不備があります。")
                    }
                }
            }
        }
        loadedGames = newGames

        pl.listener.gameIDs = mutableListOf()
        for (loadedGameID in loadedGames.keys) {
            pl.listener.gameIDs.add(loadedGameID)
        }
    }

    fun openNewGame(id: String, p: Player) {
        if (p.hasPermission("mb.open")) {
            val openingGame: Game? = loadedGames[id]
            if (openingGame == null) {
                p.sendMessage(pl.prefix + "§4§lERROR: §f§l指定されたゲームは存在しません。")
            } else {
                if (runningGames.keys.contains(id)) {
                    p.sendMessage(pl.prefix + "§4§lERROR: §f§l指定されたゲームはすでに開いています。")
                } else {
                    if (runningGames.keys.size >= pl.limitGame){
                        p.sendMessage(pl.prefix + "§4§lERROR: §f§lゲームは同時に${pl.limitGame}個までしか開けません。")
                        return
                    }
                    openingGame.candidates = mutableListOf()
                    openingGame.players = mutableMapOf()
                    openingGame.status = GameStatus.JOIN
                    runningGames[id] = openingGame
                    p.sendMessage(pl.prefix + "ゲーム「" + id + "」を開きました")
                    pl.worldMsg(pl.prefix + "§6§l" + openingGame.gameName + "§f§lがオープンしました。")
                    pl.worldMsg(pl.prefix + "§6§l/mb§f§lで参加しよう！")
                    pl.sidebar.showCandidates(openingGame, id)
                    var timer = openingGame.duration[0]
                    object : BukkitRunnable() {
                        override fun run() {
                            timer--
                            if (timer == 0) {
                                if (openingGame.status == GameStatus.JOIN) {
                                    if (openingGame.candidates.size >= openingGame.playerNumber) {
                                        pushPhase(id, p)
                                    } else {
                                        pl.worldMsg(pl.prefix + "§l時間切れで§a§l" + openingGame.gameName + "§f§lが終了されました。")
                                        stopGame(id)
                                    }
                                }
                                cancel()
                            } else {
                                if (openingGame.status == GameStatus.JOIN) {
                                    when (timer) {
                                        100 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 100秒")
                                        60 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 60秒")
                                        45 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 45秒")
                                        30 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 30秒")
                                        15 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 15秒")
                                        10 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 10秒")
                                        5 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 5秒")
                                        3 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 3秒")
                                        2 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 2秒")
                                        1 -> pl.worldMsg(pl.prefix + "§a" + openingGame.gameName + " §f参加応募終了まで: 1秒")
                                    }
                                } else {
                                    cancel()
                                }
                            }
                        }
                    }.runTaskTimer(pl, 0, 20)//タイム計測
//                var timer = openingGame.duration[0]
//                object : BukkitRunnable() {
//                    override fun run() {
//                        timer--
//                        if (timer == 0) {
//                            cancel()
//                            stopGame(id)
//                            Bukkit.broadcastMessage(pl.prefix + "§lタイムアウト: " + openingGame.gameName + "が停止されました。")
//                        }
//                    }
//                }.runTaskTimer(pl, 0, 20)
                }
            }
        }
    }

    fun addCandidate(p: Player, gameID: String){
        if (p.hasPermission("mb.join")) {
            if (runningGames[gameID] == null) {
                p.sendMessage(pl.prefix + "§4§lERROR: §f§lゲームが存在しません。")
            } else {
                if (runningGames[gameID]!!.status == GameStatus.JOIN) {
                    if (runningGames[gameID]!!.candidates.contains(p.uniqueId)) {
                        p.sendMessage(pl.prefix + "§4§lERROR: §f§lあなたはすでにこのゲームに参加しています！")
                    } else {
                        if (pl.vault.getBalance(p.uniqueId) < runningGames[gameID]!!.joinFee && pl.mode != MBGameMode.FREE) {
                            p.sendMessage(pl.prefix + "§4§lERROR: §f§l参加費が足りません！")
                        } else {
                            if (pl.mode == MBGameMode.WHITELIST){
                                if (!pl.whitelist.contains(p.uniqueId) && !p.isOp){
                                    p.sendMessage(pl.prefix + "§4§lERROR: §f§l現在ホワイトリスト制です！")
                                    return
                                }
                            }
                            var isFighter = false
                            for (game in runningGames.values) {
                                if (game.players.keys.contains(p.uniqueId)) {
                                    isFighter = true
                                }
                            }
                            if (!isFighter) {
                                p.playSound(p.location,Sound.ENTITY_PLAYER_LEVELUP,1f,2f)
                                runningGames[gameID]!!.candidates.add(p.uniqueId)
                                p.sendMessage(pl.prefix + "§6§l" + runningGames[gameID]!!.gameName + "に参加登録されました。")
                                p.sendMessage(pl.prefix + "§l抽選で選ばれると試合に参加できます")
                                if (pl.mode != MBGameMode.FREE) pl.vault.withdraw(p.uniqueId, runningGames[gameID]!!.joinFee)
                                pl.sidebar.showCandidates(runningGames[gameID]!!, gameID)
                            } else {
                                p.sendMessage(pl.prefix + "§4§lERROR: §f§lあなたは別のゲームで選手なので、参加できません。")
                            }
                        }
                    }
                } else {
                    p.sendMessage(pl.prefix + "§l今は参加フェーズではありません。")
                }
            }
        } else {
            p.sendMessage(pl.prefix + "§l権限がありません。")
        }
    }

    fun removeCandidate(p : Player, gameID: String){
        if (p.hasPermission("mb.join")) {
            if (runningGames[gameID] == null) {
                p.sendMessage(pl.prefix + "§4§lERROR: §f§lゲームが存在しません。")
            } else {
                if (runningGames[gameID]!!.status == GameStatus.JOIN) {
                    if (!runningGames[gameID]!!.candidates.contains(p.uniqueId)) {
                        p.sendMessage(pl.prefix + "§4§lERROR: §f§lあなたはこのゲームに参加していません！")
                    } else {
                        p.playSound(p.location,Sound.UI_BUTTON_CLICK,1f,1f)
                        runningGames[gameID]!!.candidates.remove(p.uniqueId)
                        p.sendMessage(pl.prefix + "§6§l" + runningGames[gameID]!!.gameName + "の参加登録を解除しました")
                        pl.sidebar.showCandidates(runningGames[gameID]!!, gameID)
                    }
                } else {
                    p.sendMessage(pl.prefix + "§l今は参加フェーズではありません。")
                }
            }
        } else {
            p.sendMessage(pl.prefix + "§l権限がありません。")
        }
    }

    //次のフェーズにゲームを進める
    fun pushPhase(gameID: String, player: Player) {
        if (runningGames[gameID] != null) {
            val pushingGame = runningGames[gameID]!!
            when (pushingGame.status) {
                GameStatus.JOIN -> { //BETに進む
                    if (pushingGame.players.keys.size == 0) {
                        if (pushingGame.candidates.size >= pushingGame.playerNumber) {
                            pl.worldMsg(pl.prefix + "§6§l" + pushingGame.gameName + "§f§lのプレイヤーが決定しました！")
                            pushingGame.candidates.shuffle()
                            var i = 1
                            for (uuid in pushingGame.candidates.take(pushingGame.playerNumber)) {
                                Bukkit.getPlayer(uuid)?.closeInventory()
                                if (Bukkit.getPlayer(uuid)?.world?.name != "bookmaker"){
                                    val lobby = pl.lobbyLocation
                                    if (lobby == null){
                                        Bukkit.getLogger().warning("lobbylocが設定されていません。増殖の恐れがあります。")
                                    }else{
                                        Bukkit.getPlayer(uuid)?.teleport(lobby)
                                    }

                                }
                                Bukkit.getPlayer(uuid)?.location?.let { Bukkit.getPlayer(uuid)?.playSound(it,Sound.BLOCK_ANVIL_PLACE,1f,1f) }
                                pushingGame.players[uuid] = mutableListOf()
                                if (pl.data.getBestRecord(gameID, uuid) != null) {
                                    pl.worldMsg(pl.prefix + "§c§lP" + i + ": §f§l" + Bukkit.getPlayer(uuid)?.name + " §e§l最高記録: " + pl.data.getBestRecord(gameID, uuid) + "秒")
                                } else {
                                    pl.worldMsg(pl.prefix + "§c§lP" + i + ": §f§l" + Bukkit.getPlayer(uuid)?.name + " §e§l記録無し")
                                }

                                for (otherGame in runningGames.values) {
                                    if (otherGame.candidates.contains(uuid)) {
                                        otherGame.candidates.remove(uuid)
                                    }
                                }
                                pushingGame.candidates.remove(uuid)

                                i++
                            }
                            pushingGame.status = GameStatus.BET
                            if (pl.mode == MBGameMode.FREE){
                                pushPhase(gameID, player)
                                return
                            }
                            for (p in pushingGame.candidates){
                                pl.vault.deposit(p,pushingGame.joinFee)
                                Bukkit.getPlayer(p)?.sendMessage(pl.prefix + "§b選手に選ばれなかったので参加費が返却されました")
                            }
                            pushingGame.candidates.clear()
                            pl.worldMsg(pl.prefix + "§f§l勝者を予想し、§6§l/mb§f§lでベットしましょう！")
                            for (fighter in pushingGame.players) {
                                val virtualBet = Bet(UUID.randomUUID(), pushingGame.virtualBet.toDouble())
                                fighter.value.add(virtualBet)
                                Bukkit.dispatchCommand(console, ("man10kit:mkit push " + Bukkit.getPlayer(fighter.key)?.name))
                            }
                            pl.sidebar.showOdds(pushingGame)
                            var timer = pushingGame.duration[1]
                            object : BukkitRunnable() {
                                override fun run() {
                                    timer--
                                    if (timer == 0) {
                                        if (pushingGame.status == GameStatus.BET) {
                                            pushPhase(gameID, player)
                                        }
                                        cancel()
                                    } else {
                                        if (pushingGame.status == GameStatus.BET) {
                                            when (timer) {
                                                100 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 100秒")
                                                60 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 60秒")
                                                45 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 45秒")
                                                30 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 30秒")
                                                15 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 15秒")
                                                10 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 10秒")
                                                5 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 5秒")
                                                3 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 3秒")
                                                2 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 2秒")
                                                1 -> pl.worldMsg(pl.prefix + "§a" + pushingGame.gameName + " §fベット終了まで: 1秒")
                                            }
                                        } else {
                                            cancel()
                                        }
                                    }
                                }
                            }.runTaskTimer(pl, 0, 20)//タイム計測
                        } else {
                            player.sendMessage(pl.prefix + "§4§lERROR: §f§l応募登録人数が足りません。")
                        }
                    } else {
                        player.sendMessage(pl.prefix + "§4§lERROR: §f§lこのゲームはask形式です。")
                    }
                }

                GameStatus.BET -> {//試合に進む
                    if (!uuidMap.keys.contains(pushingGame.players.keys.toList()[0])) {
                        pushingGame.status = GameStatus.FIGHT
                        pl.worldMsg(pl.prefix + "§6§l" + pushingGame.gameName + " §f§lベット終了!")
                        pl.worldMsg(pl.prefix + "§e§lまもなく試合が始まります...")

                        var timer = 8

                        (Bukkit.getWorld("bookmaker")?:return).players.forEach {
                            it.spigot().sendMessage(*ComponentBuilder("§2§l<<< §e§lクリックで観戦場所にテレポート§2§l >>>").event(
                                ClickEvent(ClickEvent.Action.RUN_COMMAND,"/mb view $gameID")
                            ).create())
                        }
                        pl.sidebar.showWhileFight(pushingGame)

                        object : BukkitRunnable() {
                            override fun run() {

                                when (timer) {
                                    3 -> {


                                        var i = 1
                                        //ファイター処理
                                        for (fighter in pushingGame.players) {
                                            val p = Bukkit.getPlayer(fighter.key)!!
                                            pl.worldMsg(pl.prefix +
                                                    "§c§lP" + i.toString() + ": §f§l" +
                                                    p.name +
                                                    " §r§e(オッズ: " + getOdds(pushingGame.players, fighter.key, pushingGame.tax, pushingGame.prize).roundTo2DecimalPlaces().toString() + "倍)")
                                            if (pushingGame.startingLocation != null) {
                                                val location = pushingGame.startingLocation!!
                                                val tpLocation = Location(location.world, location.x, location.y, location.z,location.yaw,location.pitch)
                                                tpLocation.add(xDifferenences[i - 1].toDouble(), 0.0, zDifferenences[i - 1].toDouble())
                                                p.teleport(tpLocation)
                                            }
                                            resetPlayerStatus(p)
                                            Bukkit.dispatchCommand(console, ("man10kit:mkit set " + p.name + " mb_" + gameID))
                                            for (command in pl.config.getStringList("${gameID}.commands")){
                                                Bukkit.dispatchCommand(console, command.replace("<player>",p.name))
                                            }
                                            pl.freezedPlayer.add(fighter.key)
                                            i++
                                        }

                                        for (p in (Bukkit.getWorld("bookmaker")?:return).players){
                                            p.playSound(p.location,Sound.ENTITY_WITHER_SPAWN,1f,1f)
                                        }
                                        pl.worldMsg(pl.prefix + "§aスタートまで: §e§l3秒")
                                    }
                                    2 -> {
                                        pl.worldMsg(pl.prefix + "§aスタートまで: §e§l2秒")
                                    }
                                    1 -> {
                                        pl.worldMsg(pl.prefix + "§aスタートまで: §e§l1秒")
                                    }
                                    0 -> {
                                        pl.worldMsg(pl.prefix + "§e§l<< START >>")
                                        for (p in (Bukkit.getWorld("bookmaker")?:return).players){
                                            if (pl.gameManager.runningGames.none { it.value.players.containsKey(p.uniqueId) }){
                                                p.sendTitle("§e§lSTART","§f§lGAME: §a§l${pushingGame.gameName}",0,40,0)
                                                p.playSound(p.location,Sound.ENTITY_WITHER_SPAWN,1f,1f)
                                            }
                                        }
                                        for (fighter in pushingGame.players.keys) {
                                            pl.freezedPlayer.remove(fighter)
                                        }

                                        cancel()
                                    }

                                }
                                timer--
                            }
                        }.runTaskTimer(pl, 0, 20)//カウントダウン

                        pushingGame.gameTimer = 0.0
                        object : BukkitRunnable() {
                            override fun run() {
                                when {
                                    pushingGame.gameTimer == -1.0 -> {
                                        cancel()
                                        return
                                    }
                                    pushingGame.gameTimer >= pushingGame.duration[2].toDouble() + 3 -> {
                                        if (pushingGame.status == GameStatus.FIGHT) {
                                            stopGame(gameID)
                                            Bukkit.broadcastMessage(pl.prefix + "§l時間切れで§a§l" + pushingGame.gameName + "§f§lが終了されました。")
                                        }
                                        cancel()
                                    }
                                    else -> {
                                        pushingGame.gameTimer += 0.05
                                    }
                                }
                            }
                        }.runTaskTimer(pl, 180, 1)//タイム計測


                    } else {
                        player.sendMessage(pl.prefix + "§4§lERROR: §f§lこのゲームはask形式です。")
                    }
                }

                GameStatus.FIGHT -> {
                    player.sendMessage(pl.prefix + "§4§lERROR: §f§l次のフェーズがありません。")
                    player.sendMessage(pl.prefix + "試合終了まで待つか、/mb forcestopで強制終了してください。")
                }

                else -> {

                }
            }
        } else {
            player.sendMessage(pl.prefix + "§4§lERROR: §f§l指定されたゲームが存在しません。")
        }
    }

    fun bet(better: Player, fighterUUID: UUID, gameId: String, price: Double) {
        val bettingGame = runningGames[gameId]!!
        if (pl.vault.getBalance(better.uniqueId) >= price) {
            if (bettingGame.players.keys.contains(better.uniqueId)) {
                better.sendMessage(pl.prefix + "§4§lERROR: §f§l選手はベットできません。")
            } else {
                if (!uuidMap.keys.contains(bettingGame.players.keys.toList()[0])) {
                    if (bettingGame.status == GameStatus.BET) {
                        val newBet = Bet(better.uniqueId, price)
                        bettingGame.players[fighterUUID]!!.add(newBet)
                        pl.vault.withdraw(better.uniqueId, price)
                        pl.worldMsg(
                            pl.prefix +
                                    "§6§l" + bettingGame.gameName +
                                    "§f§lで§e§l" + better.name +
                                    "§f§lが§d§l" + Bukkit.getPlayer(fighterUUID)?.name +
                                    "§f§lに§a§l" + price +
                                    "円§f§lベットしました! " +
                                    "§e§l(オッズ: " + getOdds(
                                bettingGame.players,
                                fighterUUID,
                                bettingGame.tax,
                                bettingGame.prize
                            ).roundTo2DecimalPlaces().toString() + "倍)"
                        )
                        pl.sidebar.showOdds(bettingGame)
                    } else {
                        better.sendMessage(pl.prefix + "§4§lERROR: §f§l今はベットできません。")
                    }
                } else {
                    if (bettingGame.status == GameStatus.BET) {
                        val newBet = Bet(better.uniqueId, price)
                        bettingGame.players[fighterUUID]!!.add(newBet)
                        pl.vault.withdraw(better.uniqueId, price)
                        pl.worldMsg(
                            pl.prefix +
                                    "§6§l" + bettingGame.gameName +
                                    "§f§lで§e§l" + better.name +
                                    "§f§lが§d§l" + uuidMap[fighterUUID] +
                                    "§f§lに§a§l" + price +
                                    "円§f§lベットしました! " +
                                    "§e§l(オッズ: " + getOdds(
                                bettingGame.players,
                                fighterUUID,
                                bettingGame.tax,
                                bettingGame.prize
                            ).roundTo2DecimalPlaces().toString() + "倍)"
                        )
                    } else {
                        better.sendMessage(pl.prefix + "§4§lERROR: §f§l今はベットできません。")
                    }
                }
            }
        } else {
            better.sendMessage(pl.prefix + "§4§lERROR: §f§l所持金が足りません")
        }
    }

    fun endGame(gameID: String, winner: UUID) {
        if (runningGames[gameID] != null) {
            val endingGame = runningGames[gameID]!!
            if (endingGame.status == GameStatus.FIGHT || uuidMap.keys.contains(endingGame.players.keys.toList()[0])) {
                val winnerBetters = endingGame.players[winner]
                val odds = getOdds(endingGame.players, winner, endingGame.tax, endingGame.prize)
                val prize = getPrize(endingGame.players,endingGame.prize,endingGame.virtualBet.toDouble(),endingGame.joinFee,endingGame.playerNumber)
                if (uuidMap.keys.contains(endingGame.players.keys.toList()[0])) {
                    Bukkit.broadcastMessage(pl.prefix +
                            "§6§l" + endingGame.gameName + " §f§l結果発表!" + "  §e§lオッズ: " + getOdds(endingGame.players, winner, endingGame.tax, endingGame.prize).roundTo2DecimalPlaces() + "倍")
                    Bukkit.broadcastMessage(pl.prefix +
                            "§c§l正解: " + uuidMap[winner])
                } else {
                    pl.worldMsg(pl.prefix +
                            "§6§l" + endingGame.gameName + " §f§l試合終了!" + "  §e§lオッズ: " + getOdds(endingGame.players, winner, endingGame.tax, endingGame.prize).roundTo2DecimalPlaces() + "倍")
                    pl.worldMsg(pl.prefix +
                            "§c§l勝者: " + Bukkit.getPlayer(winner)?.name +
                            " §a§l賞金: " + prize.toInt() + "円")
                    pl.worldMsg(pl.prefix + "§f§l記録: §e§l" + endingGame.gameTimer.roundTo2DecimalPlaces() + "秒")
                    pl.data.saveFight(gameID, endingGame, winner) //SQL
                    endingGame.gameTimer = -1.0
                    pl.vault.deposit(winner, prize)
                    val console = Bukkit.getServer().consoleSender
                    for (fighter in endingGame.players.keys) {
                        val command = "man10kit:mkit pop " + Bukkit.getPlayer(fighter)?.name
                        Bukkit.dispatchCommand(console, command)
                        val p = Bukkit.getPlayer(fighter)
                        pl.lobbyLocation?.let { p?.teleport(it) }
                    }
                    (Bukkit.getWorld("bookmaker")?:return).players.forEach {
                        it.spigot().sendMessage(*ComponentBuilder("§2§l<<< §e§lクリックでブックメーカーロビーに戻る§2§l >>>").event(
                            ClickEvent(ClickEvent.Action.RUN_COMMAND,"/mb return")
                        ).create())
                    }
                    pl.sidebar.removeAll()
                }
                for (bet in winnerBetters!!) {
                    if (Bukkit.getPlayer(bet.playerUUID) != null) {
                        val givingPrice: Double = floor((bet.price * odds))
                        pl.worldMsg(pl.prefix +
                                "§b" + Bukkit.getPlayer(bet.playerUUID)?.name +
                                "§fが正解にベットし、§e" + givingPrice.roundTo2DecimalPlaces().toString() + "円§f獲得しました。")
                        pl.vault.deposit(bet.playerUUID, givingPrice)
                    }
                }

                endingGame.status = GameStatus.OFF
                endingGame.candidates = mutableListOf()
                endingGame.players = mutableMapOf()
                runningGames.remove(gameID)
            }
        }
    }

    fun getOdds(list: MutableMap<UUID, MutableList<Bet>>, fighterUUID: UUID, tax: Double, prize: Double): Double {

        var fighterTotalPrice = 0.0
        var otherTotalPrice = 0.0
        for (fighter in list) {
            for (bet in fighter.value) {
                if (fighter.key == fighterUUID) {
                    fighterTotalPrice += bet.price
                } else {
                    otherTotalPrice += bet.price
                }
            }
        }

        val total = fighterTotalPrice + otherTotalPrice

        val odds = ((1 - (tax + prize)) / (fighterTotalPrice / total))
        if (odds < 1)return 1.00
        return odds

    }

    fun getTotalPrice(list: MutableMap<UUID, MutableList<Bet>>): Double {
        var totalPrice = 0.0
        for (fighter in list) {
            for (bet in fighter.value) {
                totalPrice += bet.price
            }
        }
        return totalPrice.roundTo2DecimalPlaces()
    }

    fun getFighterBet(list: MutableMap<UUID, MutableList<Bet>>, fighterUUID: UUID): Double {
        var total = 0.0
        for (bet in list[fighterUUID]!!){
            total += bet.price
        }
        return total
    }

    fun getPrize(list: MutableMap<UUID, MutableList<Bet>>, prize: Double, virtualBet: Double, joinFee: Double, playerNumber: Int): Double {
        var total = getTotalPrice(list) * prize

        for (bet in list){
            val fighterBet = getFighterBet(list,bet.key) - virtualBet + joinFee * playerNumber.toDouble()
            if (total > fighterBet){
                total = fighterBet
            }
        }
        return total.roundTo2DecimalPlaces()

    }

    fun stopGame(gameID: String) {
        //強制終了
        val game = runningGames[gameID]!!
        when (game.status) {
            GameStatus.JOIN -> {
                game.candidates = mutableListOf()
            }
            GameStatus.OFF -> {

            }
            else -> {
                for (fighter in game.players) {
                    Bukkit.getPlayer(fighter.key)?.let { resetPlayerStatus(it) }
                    pl.vault.deposit(fighter.key,game.joinFee)
                    for (bet in fighter.value) {
                        if (Bukkit.getPlayer(bet.playerUUID) != null) {
                            pl.vault.deposit(bet.playerUUID, bet.price)
                            Bukkit.getPlayer(bet.playerUUID)?.sendMessage(pl.prefix + "§lベット金額が返金されました。")
                        }
                    }
                }

                if (game.status == GameStatus.FIGHT) {
                    val console = Bukkit.getServer().consoleSender
                    for (fighter in game.players.keys) {
                        val p = Bukkit.getPlayer(fighter)
                        Bukkit.getPlayer(fighter)?.let { resetPlayerStatus(it) }
                        val command = "man10kit:mkit pop " + p?.name
                        Bukkit.dispatchCommand(console, command)
                        pl.lobbyLocation?.let { p?.teleport(it) }
                    }
                }
                (Bukkit.getWorld("bookmaker")?:return).players.forEach {
                    it.spigot().sendMessage(*ComponentBuilder("§2§l<<< §e§lクリックでブックメーカーロビーに戻る§2§l >>>").event(
                        ClickEvent(ClickEvent.Action.RUN_COMMAND,"/mb return")
                    ).create())
                }
                pl.sidebar.removeAll()
            }
        }
        game.gameTimer = -1.0
        game.status = GameStatus.OFF
        game.candidates = mutableListOf()
        game.players = mutableMapOf()
        runningGames.remove(gameID)
    }

    fun setFighterSpawnPoint(gameID: String, p: Player) {
        val game = loadedGames[gameID]!!
        if (p.location.world?.name == "bookmaker") {
            game.startingLocation = p.location
            pl.config.set("$gameID.x", p.location.x)
            pl.config.set("$gameID.y", p.location.y)
            pl.config.set("$gameID.z", p.location.z)
            pl.config.set("$gameID.yaw", p.location.yaw)
            pl.config.set("$gameID.pitch",p.location.pitch)
            pl.saveConfig()
            p.sendMessage("§l座標を設定しました。")
        } else {
            p.sendMessage("§lbookmakerワールドで実行してください。")
        }

    }

    fun setViewerSpawnPoint(gameID: String, p: Player) {
        val game = loadedGames[gameID]!!
        if (p.location.world?.name == "bookmaker") {
            game.viewingLocation = p.location
            pl.config.set("$gameID.vx", p.location.x)
            pl.config.set("$gameID.vy", p.location.y)
            pl.config.set("$gameID.vz", p.location.z)
            pl.config.set("$gameID.vyaw", p.location.yaw)
            pl.config.set("$gameID.vpitch",p.location.pitch)
            pl.saveConfig()
            p.sendMessage(pl.prefix + "§l座標を設定しました。")
        } else {
            p.sendMessage(pl.prefix + "§lbookmakerワールドで実行してください。")
        }
    }

    fun viewTeleport(gameID: String, p: Player) {
        if (p.hasPermission("mb.view")) {
            if (runningGames[gameID] != null) {
                if (runningGames[gameID]!!.viewingLocation != null) {
                    if (runningGames[gameID]!!.status != GameStatus.FIGHT) {
                        p.sendMessage(pl.prefix + "§lこのゲームは試合中ではありません。")
                    } else {
                        if (!runningGames[gameID]!!.players.keys.contains(p.uniqueId)) {
                            runningGames[gameID]!!.viewingLocation?.let { p.teleport(it) }
                        } else {
                            p.sendMessage(pl.prefix + "§l選手は観戦できません。")
                        }
                    }
                } else {
                    p.sendMessage(pl.prefix + "§lこのゲームにはテレポート先が存在しません。")
                }
            } else {
                p.sendMessage(pl.prefix + "§l指定したゲームが存在しません。")
            }
        } else {
            p.sendMessage(pl.prefix + "§l権限がありません。")
        }
    }

    fun openNewQ(gameID: String, question: String, s1: String, s2: String) {
        val s1UUID = UUID.randomUUID()
        val s2UUID = UUID.randomUUID()
        uuidMap[s1UUID] = s1
        uuidMap[s2UUID] = s2
        //FLAG
        val openingGame = Game(
            "Q: $question",
                Material.OAK_SIGN,
                2,
                0.0,
                0.1,
                0.0,
                null,
                null,
                mutableListOf(0, 0, 0),
                GameStatus.BET,
                mutableMapOf(s1UUID to mutableListOf(), s2UUID to mutableListOf()),
                mutableListOf(),
                0.0,
                10000,
                mutableListOf()
        )
        runningGames[gameID] = openingGame
        Bukkit.broadcastMessage(pl.prefix + "§f§l質問§6§l「" + openingGame.gameName + "」")
        Bukkit.broadcastMessage(pl.prefix + "§c§l選択肢1: §f§l" + s1)
        Bukkit.broadcastMessage(pl.prefix + "§c§l選択肢2: §f§l" + s2)
        Bukkit.broadcastMessage(pl.prefix + "§6§l/mb§f§lでベットしよう！")
        for (selection in openingGame.players) {
            val virtualBet = Bet(UUID.randomUUID(), openingGame.virtualBet.toDouble())
            selection.value.add(virtualBet)
        }
    }

    fun Double.roundTo2DecimalPlaces() = BigDecimal(this).setScale(2, RoundingMode.HALF_UP).toDouble()

    fun resetPlayerStatus(p: Player) {
        p.fireTicks = 0
        p.gameMode = GameMode.SURVIVAL
        if (p.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            p.removePotionEffect(PotionEffectType.BLINDNESS)
        }
        if (p.hasPotionEffect(PotionEffectType.CONFUSION)) {
            p.removePotionEffect(PotionEffectType.CONFUSION)
        }
        if (p.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
            p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE)
        }
        if (p.hasPotionEffect(PotionEffectType.FAST_DIGGING)) {
            p.removePotionEffect(PotionEffectType.FAST_DIGGING)
        }
        if (p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            p.removePotionEffect(PotionEffectType.FIRE_RESISTANCE)
        }
        if (p.hasPotionEffect(PotionEffectType.HEAL)) {
            p.removePotionEffect(PotionEffectType.HEAL)
        }
        if (p.hasPotionEffect(PotionEffectType.HUNGER)) {
            p.removePotionEffect(PotionEffectType.HUNGER)
        }
        if (p.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
            p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE)
        }
        if (p.hasPotionEffect(PotionEffectType.JUMP)) {
            p.removePotionEffect(PotionEffectType.JUMP)
        }
        if (p.hasPotionEffect(PotionEffectType.POISON)) {
            p.removePotionEffect(PotionEffectType.POISON)
        }
        if (p.hasPotionEffect(PotionEffectType.REGENERATION)) {
            p.removePotionEffect(PotionEffectType.REGENERATION)
        }
        if (p.hasPotionEffect(PotionEffectType.SLOW)) {
            p.removePotionEffect(PotionEffectType.SLOW)
        }
        if (p.hasPotionEffect(PotionEffectType.SLOW_DIGGING)) {
            p.removePotionEffect(PotionEffectType.SLOW_DIGGING)
        }
        if (p.hasPotionEffect(PotionEffectType.SPEED)) {
            p.removePotionEffect(PotionEffectType.SPEED)
        }
        if (p.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
            p.removePotionEffect(PotionEffectType.WATER_BREATHING)
        }
        if (p.hasPotionEffect(PotionEffectType.WEAKNESS)) {
            p.removePotionEffect(PotionEffectType.WEAKNESS)
        }
        if (p.hasPotionEffect(PotionEffectType.ABSORPTION)) {
            p.removePotionEffect(PotionEffectType.ABSORPTION)
        }
        if (p.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
            p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE)
        }
        if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            p.removePotionEffect(PotionEffectType.NIGHT_VISION)
        }
        p.foodLevel = 20
        p.exhaustion = 0f
        p.health = p.healthScale
    }
}