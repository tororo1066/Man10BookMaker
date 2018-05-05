package red.man10

import org.apache.commons.lang.mutable.Mutable
import org.bukkit.*
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.util.*
import org.bukkit.potion.PotionEffectType
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitScheduler



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
                var players:  MutableMap<UUID, MutableList<Bet>>,//AnyはUUIDかStringが入る
                var candidates: MutableList<UUID>,
                var gameTimer: Double)

data class Bet(val playerUUID: UUID,
               val price: Double)

class BookMakerGameManager {

    var loadedGames = mutableMapOf<String, Game>()

    var runningGames = mutableMapOf<String, Game>()

    var UUIDMap = mutableMapOf<UUID, String>()

    val console = Bukkit.getServer().consoleSender
    val scheduler = Bukkit.getServer().scheduler

    val xDifferenences = mutableListOf<Int>(-1, 0, 1, -1, 0, 1, -1, 0, 1, -1, 0, 1)
    val zDifferenences = mutableListOf<Int>(0, 0, 0, 1, -1, 1, 0, 0, 0, 1, -1, 1)

    companion object {
        var pl: BookMakerPlugin? = null
    }

    //クラス接続
    fun returnGameManager(plugin: BookMakerPlugin) : BookMakerGameManager{
        pl = plugin
        return BookMakerGameManager()
    }

    //コンフィグロード
    fun setUpGames(config: FileConfiguration, p: Player?) {
        var newGames = mutableMapOf<String, Game>()
        val keys = config.getKeys(false)

        pl!!.listener.hasBMWorld = (Bukkit.getWorld("bookmaker") != null)

        for (gameKey in keys) {
            //gameに不備がないか確認
            //FLAG
            if (config.getKeys(true).containsAll(listOf(gameKey + ".name", gameKey + ".playerNumber", gameKey + ".item", gameKey + ".joinFee", gameKey + ".tax", gameKey + ".prize"))) {

                //itemが存在するアイテムかの確認
                var gameItem: Material = Material.STONE
                try {
                    gameItem = Material.valueOf(config.getString(gameKey + ".item"))
                } catch (e: Exception) {
                    //コンソール・プレイヤー分岐
                    if (p == null) {
                        print(pl!!.prefix + "§e§lWARNING: §r§fゲーム「" + gameKey + "」のitemが存在しないアイテムです。")
                    } else {
                        p.sendMessage(pl!!.prefix + "§e§lWARNING: §r§fゲーム「" + gameKey + "」のitemが存在しないアイテムです。")
                    }
                }

                var startLocation: Location? = null
                if (config.getKeys(true).containsAll(listOf(gameKey + ".x", gameKey + ".y", gameKey + ".x"))) {
                    if (pl!!.listener.hasBMWorld) {
                        startLocation = Location(
                                Bukkit.getWorld("bookmaker"),
                                config.getDouble(gameKey + ".x"),
                                config.getDouble(gameKey + ".y"),
                                config.getDouble(gameKey + ".z"))
                    }
                } else {
                }

                var durationList = mutableListOf<Int>()
                if (config.getKeys(true).containsAll(listOf(gameKey + ".time1", gameKey + ".time2", gameKey + ".time3"))) {
                    durationList.add(config.getInt(gameKey + ".time1"))
                    durationList.add(config.getInt(gameKey + ".time2"))
                    durationList.add(config.getInt(gameKey + ".time3"))
                } else {
                    durationList = mutableListOf(120, 120, 120)
                }


                var viewLocation: Location? = null
                if (config.getKeys(true).containsAll(listOf(gameKey + ".vx", gameKey + ".vy", gameKey + ".vx"))) {
                    if (pl!!.listener.hasBMWorld) {
                        viewLocation = Location(
                                Bukkit.getWorld("bookmaker"),
                                config.getDouble(gameKey + ".vx"),
                                config.getDouble(gameKey + ".vy"),
                                config.getDouble(gameKey + ".vz"))
                    }
                } else {
                }

                //仮配列にゲームを追加
                if (loadedGames[gameKey] == null) {
                    //新しいゲーム
                    newGames.put(
                            gameKey,
                            //FLAG
                            Game(
                                    config.getString(gameKey + ".name"),
                                    gameItem,
                                    config.getInt(gameKey + ".playerNumber"),
                                    config.getInt(gameKey + ".joinFee").toDouble(),
                                    config.getDouble(gameKey + ".tax"),
                                    config.getDouble(gameKey + ".prize"),
                                    startLocation,
                                    viewLocation,
                                    durationList,
                                    GameStatus.OFF,
                                    mutableMapOf(),
                                    mutableListOf(),
                                    0.0
                            )
                    )
                } else {
                    //元々あったゲームの改変
                    //FLAG
                    var changingGame = loadedGames[gameKey]!!
                    changingGame.gameName = config.getString(gameKey + ".name")
                    changingGame.item = gameItem
                    changingGame.playerNumber = config.getInt(gameKey + ".playerNumber")
                    changingGame.joinFee = config.getInt(gameKey + ".joinFee").toDouble()
                    changingGame.tax = config.getDouble(gameKey + ".tax")
                    changingGame.prize = config.getDouble(gameKey + ".prize")
                    changingGame.startingLocation = startLocation
                    changingGame.viewingLocation = viewLocation
                    changingGame.duration = durationList
                    newGames.put(gameKey, changingGame)
                }
            } else {
                //configに不備がある
                if (p == null) {
                    print(pl!!.prefix + "§4§lERROR: §f§lゲーム「" + gameKey + "」のconfigに不備があります。")
                } else {
                    p.sendMessage(pl!!.prefix + "§4§lERROR: §f§lゲーム「" + gameKey + "」のconfigに不備があります。")
                }
            }
        }
        loadedGames = newGames

        pl!!.listener.gameIDs = mutableListOf()
        for (loadedGameID in loadedGames.keys) {
            pl!!.listener.gameIDs.add(loadedGameID)
        }
    }

    fun openNewGame(id: String, p: Player) {
        var openingGame: Game? = loadedGames[id]
        if (openingGame == null) {
            p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l指定されたゲームは存在しません。")
        } else {
            if (runningGames.keys.contains(id)) {
                p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l指定されたゲームはすでに開いています。")
            } else {
                openingGame!!.candidates = mutableListOf()
                openingGame!!.players = mutableMapOf()
                openingGame.status = GameStatus.JOIN
                runningGames.put(id, openingGame)
                p.sendMessage(pl!!.prefix + "ゲーム「" + id + "」を開きました")
                Bukkit.broadcastMessage(pl!!.prefix + "§6§l" + openingGame.gameName + "§f§lがオープンしました。")
                Bukkit.broadcastMessage(pl!!.prefix + "§6§l/mb§f§lで参加しよう！")
                var timer = openingGame.duration[0]
                object : BukkitRunnable() {
                    override fun run() {
                        timer--
                        if (timer == 0) {
                            if (openingGame.status == GameStatus.JOIN) {
                                if (openingGame.candidates.size >= openingGame.playerNumber) {
                                    pushPhase(id, p)
                                } else {
                                    Bukkit.broadcastMessage(pl!!.prefix + "§l時間切れで§a§l" + openingGame.gameName + "§f§lが終了されました。")
                                    stopGame(id)
                                }
                            }
                            cancel()
                        }
                    }
                }.runTaskTimer(pl!!, 0, 20)//タイム計測
//                var timer = openingGame.duration[0]
//                object : BukkitRunnable() {
//                    override fun run() {
//                        timer--
//                        if (timer == 0) {
//                            cancel()
//                            stopGame(id)
//                            Bukkit.broadcastMessage(pl!!.prefix + "§lタイムアウト: " + openingGame.gameName + "が停止されました。")
//                        }
//                    }
//                }.runTaskTimer(pl!!, 0, 20)
            }
        }
    }

    fun addCandidate(p: Player, gameID: String){
        if (runningGames[gameID] == null) {
            p.sendMessage(pl!!.prefix + "§4§lERROR: §f§lゲームが存在しません。")
        } else {
            if (runningGames[gameID]!!.status == GameStatus.JOIN) {
                if (runningGames[gameID]!!.candidates.contains(p.uniqueId)) {
                    p.sendMessage(pl!!.prefix + "§4§lERROR: §f§lあなたはすでにこのゲームに参加しています！")
                } else {
                    if (pl!!.vault!!.getBalance(p.uniqueId) < runningGames[gameID]!!.joinFee) {
                        p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l参加費が足りません！")
                    } else {
                        var isFighter = false
                        for (game in runningGames.values) {
                            if (game.players.keys.contains(p.uniqueId)) {
                                isFighter = true
                            }
                        }
                        if (!isFighter) {
                            runningGames[gameID]!!.candidates.add(p.uniqueId)
                            p.sendMessage(pl!!.prefix + "§6§l" + runningGames[gameID]!!.gameName + "に参加登録されました。")
                            p.sendMessage(pl!!.prefix + "§l抽選で選ばれると試合に参加できます")
                            pl!!.vault!!.withdraw(p.uniqueId, runningGames[gameID]!!.joinFee)
                        } else {
                            p.sendMessage(pl!!.prefix + "§4§lERROR: §f§lあなたは別のゲームで選手なので、参加できません。")
                        }
                    }
                }
            }
        }
    }

    //次のフェーズにゲームを進める
    fun pushPhase(gameID: String, p: Player) {
        if (runningGames[gameID] != null) {
            var pushingGame = runningGames[gameID]!!
            when (pushingGame.status) {
                GameStatus.JOIN -> { //BETに進む
                    if (pushingGame.players.keys.size == 0) {
                        if (pushingGame.candidates.size >= pushingGame.playerNumber) {
                            Bukkit.broadcastMessage(pl!!.prefix + "§6§l" + pushingGame.gameName + "§f§lのプレイヤーが決定しました！")
                            Collections.shuffle(pushingGame.candidates)
                            var i = 1
                            for (uuid in pushingGame.candidates.take(pushingGame.playerNumber)) {
                                pushingGame.players.put(uuid, mutableListOf())
                                Bukkit.broadcastMessage(pl!!.prefix + "§c§lP" + i + ": §f§l" + Bukkit.getOfflinePlayer(uuid).name)

                                for (otherGame in runningGames.values) {
                                    if (otherGame.candidates.contains(uuid)) {
                                        otherGame.candidates.remove(uuid)
                                    }
                                }

                                i++
                            }
                            Bukkit.broadcastMessage(pl!!.prefix + "§f§l勝者を予想し、§6§l/mb§f§lでベットしましょう！")
                            pushingGame.status = GameStatus.BET
                            for (fighter in pushingGame.players) {
                                var virtualBet = Bet(UUID.randomUUID(), 100000.0)
                                fighter.value.add(virtualBet)
                            }
                            pl!!.sidebar!!.showOdds(pushingGame)
                            var timer = pushingGame.duration[1]
                            object : BukkitRunnable() {
                                override fun run() {
                                    timer--
                                    if (timer == 0) {
                                        if (pushingGame.status == GameStatus.BET) {
                                            pushPhase(gameID, p)
                                        }
                                        cancel()
                                    }
                                }
                            }.runTaskTimer(pl!!, 0, 20)//タイム計測
                        } else {
                            p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l応募登録人数が足りません。")
                        }
                    } else {
                        p.sendMessage(pl!!.prefix + "§4§lERROR: §f§lこのゲームはask形式です。")
                    }
                }

                GameStatus.BET -> {//試合に進む
                    if (UUIDMap.keys.contains(pushingGame.players.keys.toList()[0]) == false) {
                        pl!!.sidebar!!.removeAll()
                        pushingGame.status = GameStatus.FIGHT
                        Bukkit.broadcastMessage(pl!!.prefix + "§6§l" + pushingGame.gameName + " §f§lベット終了!")
                        Bukkit.broadcastMessage(pl!!.prefix + "§e§lまもなく試合が始まります...")
                        object : BukkitRunnable() {
                            override fun run() {
                                cancel()
                            }
                        }.runTaskTimer(pl!!, 120, 0) //試合前の時間

                        var timer = 3
                        for (p in Bukkit.getOnlinePlayers()) {
                            p.playSound(p.location, Sound.ENTITY_WITHER_SPAWN, 1.toFloat(), 1.toFloat())
                        }

                        var i = 1
                        //ファイター処理
                        for (fighter in pushingGame.players) {
                            var p = Bukkit.getPlayer(fighter.key)
                            Bukkit.broadcastMessage(pl!!.prefix +
                                    "§c§lP" + i.toString() + ": §f§l" +
                                    p.name +
                                    " §r§e(オッズ: " + getOdds(pushingGame.players, fighter.key, pushingGame.tax, pushingGame.prize).toString() + "倍)")
                            if (pushingGame.startingLocation != null) {
                                var location = pushingGame.startingLocation!!
                                var tpLocation = Location(location.world, location.x, location.y, location.z)
                                tpLocation.add(xDifferenences[i - 1]!!.toDouble(), 0.0, zDifferenences[i - 1]!!.toDouble())
                                p.teleport(tpLocation)
                            }
                            Bukkit.dispatchCommand(console, ("mkit push " + p.name))
                            resetPlayerStatus(p)
                            p.gameMode = GameMode.SURVIVAL
                            Bukkit.dispatchCommand(console, ("mkit set " + p.name + " mb_" + gameID))
                            Bukkit.dispatchCommand(console, ("mkit set " + p.name + " mb_" + gameID))
                            pl!!.freezedPlayer.add(fighter.key)
                            i++
                        }

                        Bukkit.dispatchCommand(console, "minecraft:tellraw @a {\"text\":\"§2§l<<< §e§lクリックで観戦場所にテレポート§2§l >>>\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/mb view " + gameID + "\"}}")

                        object : BukkitRunnable() {
                            override fun run() {

                                when (timer) {
                                    3 -> {
                                        Bukkit.broadcastMessage(pl!!.prefix + "§aスタートまで: §e§l3秒")
                                    }
                                    2 -> {
                                        Bukkit.broadcastMessage(pl!!.prefix + "§aスタートまで: §e§l2秒")
                                    }
                                    1 -> {
                                        Bukkit.broadcastMessage(pl!!.prefix + "§aスタートまで: §e§l1秒")
                                    }
                                    0 -> {
                                        Bukkit.broadcastMessage(pl!!.prefix + "§e§l<< START >>")
                                        Bukkit.dispatchCommand(console, ("mtitle &e&lSTART | &f&lGAME: &a&l" + pushingGame.gameName + " | 2"))
                                        for (fighter in pushingGame.players.keys) {
                                            pl!!.freezedPlayer.remove(fighter)
                                        }
                                        cancel()
                                    }

                                }
                                timer--
                            }
                        }.runTaskTimer(pl!!, 40, 20)//カウントダウン

                        pushingGame.gameTimer = 0.0
                        object : BukkitRunnable() {
                            override fun run() {
                                print(pushingGame.gameTimer)
                                print(pushingGame.duration[2])
                                if (pushingGame.gameTimer == -1.0) {
                                    cancel()
                                    return
                                } else if (pushingGame.gameTimer >= pushingGame.duration[2].toDouble() + 3) {
                                    if (pushingGame.status == GameStatus.FIGHT) {
                                        stopGame(gameID)
                                        Bukkit.broadcastMessage(pl!!.prefix + "§l時間切れで§a§l" + pushingGame.gameName + "§f§lが終了されました。")
                                    }
                                    cancel()
                                } else {
                                    pushingGame.gameTimer += 0.05
                                }
                            }
                        }.runTaskTimer(pl!!, 60, 1)//タイム計測

                    } else {
                        p.sendMessage(pl!!.prefix + "§4§lERROR: §f§lこのゲームはask形式です。")
                    }
                }

                GameStatus.FIGHT -> {
                    p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l次のフェーズがありません。")
                    p.sendMessage(pl!!.prefix + "試合終了まで待つか、/mb stopで強制終了してください。")
                }

                else -> {

                }
            }
        } else {
            p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l指定されたゲームが存在しません。")
        }
    }

    fun bet(better: Player, fighterUUID: UUID, gameId: String, price: Double) {
        var bettingGame = runningGames[gameId]!!
        var betPrice = price
        if (pl!!.vault!!.getBalance(better.uniqueId) >= betPrice) {
            if (bettingGame.players.keys.contains(better.uniqueId)) {
                better.sendMessage(pl!!.prefix + "§4§lERROR: §f§l選手はベットできません。")
            } else {
                if (UUIDMap.keys.contains(bettingGame.players.keys.toList()[0]) == false) {
                    if (bettingGame.status == GameStatus.BET) {
                        var newBet = Bet(better.uniqueId, betPrice)
                        bettingGame.players[fighterUUID]!!.add(newBet)
                        pl!!.vault!!.withdraw(better.uniqueId, betPrice)
                        Bukkit.broadcastMessage(pl!!.prefix +
                                "§6§l" + bettingGame.gameName +
                                "§f§lで§e§l" + better.name +
                                "§f§lが§d§l" + Bukkit.getOfflinePlayer(fighterUUID).name +
                                "§f§lに§a§l" + price +
                                "円§f§lベットしました! " +
                                "§e§l(オッズ: " + getOdds(bettingGame.players, fighterUUID, bettingGame.tax, bettingGame.prize).toString() + "倍)"
                        )
                        pl!!.sidebar!!.showOdds(bettingGame)
                    } else {
                        better.sendMessage(pl!!.prefix + "§4§lERROR: §f§l今はベットできません。")
                    }
                } else {
                    if (bettingGame.status == GameStatus.BET) {
                        var newBet = Bet(better.uniqueId, betPrice)
                        bettingGame.players[fighterUUID]!!.add(newBet)
                        pl!!.vault!!.withdraw(better.uniqueId, betPrice)
                        Bukkit.broadcastMessage(pl!!.prefix +
                                "§6§l" + bettingGame.gameName +
                                "§f§lで§e§l" + better.name +
                                "§f§lが§d§l" + UUIDMap[fighterUUID] +
                                "§f§lに§a§l" + price +
                                "円§f§lベットしました! " +
                                "§e§l(オッズ: " + getOdds(bettingGame.players, fighterUUID, bettingGame.tax, bettingGame.prize).toString() + "倍)"
                        )
                    } else {
                        better.sendMessage(pl!!.prefix + "§4§lERROR: §f§l今はベットできません。")
                    }
                }
            }
        } else {
            better.sendMessage(pl!!.prefix + "§4§lERROR: §f§l所持金が足りません")
        }
    }

    fun endGame(gameID: String, winner: UUID) {
        if (runningGames[gameID] != null) {
            var endingGame = runningGames[gameID]!!
            if (endingGame.status == GameStatus.FIGHT || UUIDMap.keys.contains(endingGame.players.keys.toList()[0])) {
                var winnerBetters = endingGame.players[winner]
                var odds = getOdds(endingGame.players, winner, endingGame.tax, endingGame.prize)
                var prize = getTotalPrice(endingGame.players) * endingGame.prize
                if (UUIDMap.keys.contains(endingGame.players.keys.toList()[0])) {
                    Bukkit.broadcastMessage(pl!!.prefix +
                            "§6§l" + endingGame.gameName + " §f§l結果発表!" + "  §e§lオッズ: " + getOdds(endingGame.players, winner, endingGame.tax, endingGame.prize) + "倍")
                    Bukkit.broadcastMessage(pl!!.prefix +
                            "§c§l正解: " + UUIDMap[winner])
                } else {
                    Bukkit.broadcastMessage(pl!!.prefix +
                            "§6§l" + endingGame.gameName + " §f§l試合終了!" + "  §e§lオッズ: " + getOdds(endingGame.players, winner, endingGame.tax, endingGame.prize) + "倍")
                    Bukkit.broadcastMessage(pl!!.prefix +
                            "§c§l勝者: " + Bukkit.getOfflinePlayer(winner).name +
                            " §a§l賞金: " + prize.toInt() + "円")
                    Bukkit.broadcastMessage(pl!!.prefix + "§f§l記録: §e§l" + endingGame.gameTimer.roundTo2DecimalPlaces() + "秒")
                    endingGame.gameTimer = -1.0
                    pl!!.vault!!.deposit(winner, prize)
                    val console = Bukkit.getServer().consoleSender
                    for (fighter in endingGame.players.keys) {
                        val command = "mkit pop " + Bukkit.getOfflinePlayer(fighter).name
                        Bukkit.dispatchCommand(console, command)
                        var p = Bukkit.getPlayer(fighter)
                        p.performCommand("spawn")
                    }
                }
                for (bet in winnerBetters!!) {
                    if (Bukkit.getOfflinePlayer(bet.playerUUID) != null) {
                        var givingPrice = (bet.price * odds).toInt()
                        Bukkit.broadcastMessage(pl!!.prefix +
                                "§b" + Bukkit.getOfflinePlayer(bet.playerUUID).name +
                                "§fが正解にベットし、§e" + givingPrice.toString() + "円§f獲得しました。")
                    } else {
                    }
                }
                endingGame.status = GameStatus.OFF
                endingGame.candidates = mutableListOf()
                endingGame.players = mutableMapOf()
                runningGames.remove(gameID)
            } else {

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

        otherTotalPrice *= (1 - (tax + prize))
        return ((fighterTotalPrice + otherTotalPrice) / fighterTotalPrice).roundTo2DecimalPlaces()

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

    fun stopGame(gameID: String) {
        //強制終了
        var game = runningGames[gameID]!!
        when (game.status) {
            GameStatus.JOIN -> {
                game.candidates = mutableListOf()
            }
            GameStatus.OFF -> {

            }
            else -> {
                for (fighter in game.players) {
                    for (bet in fighter.value) {
                        if (Bukkit.getOfflinePlayer(bet.playerUUID) != null) {
                            pl!!.vault!!.deposit(bet.playerUUID, bet.price)
                            Bukkit.getPlayer(bet.playerUUID).sendMessage(pl!!.prefix + "§lベット金額が返金されました。")
                        }
                    }
                }
                if (game.status == GameStatus.FIGHT) {
                    val console = Bukkit.getServer().consoleSender
                    for (fighter in game.players.keys) {
                        var p = Bukkit.getPlayer(fighter)
                        val command = "mkit pop " + p.name
                        Bukkit.dispatchCommand(console, command)
                        p.performCommand("spawn")
                    }
                }
                pl!!.sidebar!!.removeAll()
            }
        }
        game.gameTimer = -1.0
        game.status = GameStatus.OFF
        game.candidates = mutableListOf()
        game.players = mutableMapOf()
        runningGames.remove(gameID)
    }

    fun setFighterSpawnPoint(gameID: String, p: Player) {
        var game = loadedGames[gameID]!!
        if (p.location.world.name == "bookmaker") {
            game.startingLocation = p.location
            pl!!.config.set(gameID + ".x", p.location.x)
            pl!!.config.set(gameID + ".y", p.location.y)
            pl!!.config.set(gameID + ".z", p.location.z)
            pl!!.saveConfig()
            p.sendMessage("§l座標を設定しました。")
        } else {
            p.sendMessage("§lbookmakerワールドで実行してください。")
        }

    }

    fun setViewerSpawnPoint(gameID: String, p: Player) {
        var game = loadedGames[gameID]!!
        if (p.location.world.name == "bookmaker") {
            game.viewingLocation = p.location
            pl!!.config.set(gameID + ".vx", p.location.x)
            pl!!.config.set(gameID + ".vy", p.location.y)
            pl!!.config.set(gameID + ".vz", p.location.z)
            pl!!.saveConfig()
            p.sendMessage(pl!!.prefix + "§l座標を設定しました。")
        } else {
            p.sendMessage(pl!!.prefix + "§lbookmakerワールドで実行してください。")
        }
    }

    fun viewTeleport(gameID: String, p: Player) {
        if (p.hasPermission("mb.view")) {
            if (runningGames[gameID] != null) {
                if (runningGames[gameID]!!.viewingLocation != null) {
                    if (runningGames[gameID]!!.status != GameStatus.FIGHT) {
                        p.sendMessage(pl!!.prefix + "§lこのゲームは試合中ではありません。")
                    } else {
                        if (!runningGames[gameID]!!.players.keys.contains(p.uniqueId)) {
                            p.teleport(runningGames[gameID]!!.viewingLocation)
                        } else {
                            p.sendMessage(pl!!.prefix + "§l選手は観戦できません。")
                        }
                    }
                } else {
                    p.sendMessage(pl!!.prefix + "§lこのゲームにはテレポート先が存在しません。")
                }
            } else {
                p.sendMessage(pl!!.prefix + "§l指定したゲームが存在しません。")
            }
        } else {
            p.sendMessage(pl!!.prefix + "§l権限がありません。")
        }
    }

    fun openNewQ(gameID: String, question: String, s1: String, s2: String) {
        var s1UUID = UUID.randomUUID()
        var s2UUID = UUID.randomUUID()
        UUIDMap.put(s1UUID, s1)
        UUIDMap.put(s2UUID, s2)
        //FLAG
        var openingGame = Game(
                "Q: " + question,
                Material.SIGN,
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
                0.0
        )
        runningGames.put(gameID, openingGame)
        Bukkit.broadcastMessage(pl!!.prefix + "§f§l質問§6§l「" + openingGame.gameName + "」")
        Bukkit.broadcastMessage(pl!!.prefix + "§c§l選択肢1: §f§l" + s1)
        Bukkit.broadcastMessage(pl!!.prefix + "§c§l選択肢2: §f§l" + s2)
        Bukkit.broadcastMessage(pl!!.prefix + "§6§l/mb§f§lでベットしよう！")
        for (selection in openingGame.players) {
            var virtualBet = Bet(UUID.randomUUID(), 100000.0)
            selection.value.add(virtualBet)
        }
    }

    fun Double.roundTo2DecimalPlaces() = BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()

    fun resetPlayerStatus(p: Player) {
        p.fireTicks = 0
        p.inventory.clear()
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