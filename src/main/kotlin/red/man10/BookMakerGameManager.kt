package red.man10

import org.apache.commons.lang.mutable.Mutable
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.util.*

//FLAG
data class Game(var gameName: String,
                var item: Material,
                var playerNumber: Int,
                var joinFee: Double,

                var status: GameStatus,
                var players:  MutableMap<Any, MutableList<Bet>>,//AnyはUUIDかStringが入る
                var candidates: MutableList<UUID>)

//data class RunningGame(val game: Game, //config変えるとこれも変わる
//                       var players: MutableMap<Any, MutableList<Bet>>)//AnyはUUIDかStringが入る

data class Bet(val playerUUID: UUID,
               val price: Double)

class BookMakerGameManager {

    var loadedGames = mutableMapOf<String, Game>()

    var runningGames = mutableMapOf<String, Game>()

    companion object {
        var pl: BookMakerPlugin? = null
    }


    fun returnGameManager(plugin: BookMakerPlugin) : BookMakerGameManager{
        pl = plugin
        return BookMakerGameManager()
    }

    fun setUpGames(config: FileConfiguration, p: Player?) {
        var newGames = mutableMapOf<String, Game>()
        val keys = config.getKeys(false)

        for (gameKey in keys) {
            //gameに不備がないか確認
            //FLAG
            if (config.getKeys(true).containsAll(listOf(gameKey + ".name", gameKey + ".playerNumber", gameKey + ".item", gameKey + ".joinFee"))) {

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
                                    GameStatus.OFF,
                                    mutableMapOf(),
                                    mutableListOf()
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
        print(newGames)
        loadedGames = newGames
    }

    fun openNewGame(id: String, p: Player) {
        var openingGame: Game? = loadedGames[id]
        if (openingGame == null) {
            p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l指定されたゲームは存在しません。")
        } else {
            if (runningGames.keys.contains(id)) {
                p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l指定されたゲームはすでに開いています。")
            } else {
                openingGame.status = GameStatus.JOIN
                runningGames.put(id, openingGame)
                p.sendMessage(pl!!.prefix + "ゲーム「" + id + "」を開きました")
                Bukkit.broadcastMessage(pl!!.prefix + "§lゲーム§6§l「" + openingGame.gameName + "」§f§lがオープンしました。 §6§l/mb§f§lで参加しよう！")
            }
        }
    }

    fun addCandidate(p: Player, gameID: String){
        if (runningGames[gameID] == null) {
            p.sendMessage(pl!!.prefix + "§4§lERROR: §f§lゲームが存在しません。")
        } else {
            if (runningGames[gameID]!!.candidates.contains(p.uniqueId)) {
                p.sendMessage(pl!!.prefix + "§4§lERROR: §f§lあなたはすでにこのゲームに参加しています！")
            } else {
                if (pl!!.vault!!.getBalance(p.uniqueId) < runningGames[gameID]!!.joinFee) {
                    p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l参加費が足りません！")
                } else {
                    runningGames[gameID]!!.candidates.add(p.uniqueId)
                    p.sendMessage(pl!!.prefix + "§lゲーム「" + runningGames[gameID]!!.gameName + "」に参加登録されました。抽選で選ばれると試合に参加できます。")
                    pl!!.vault!!.withdraw(p.uniqueId, runningGames[gameID]!!.joinFee)
                    print(runningGames[gameID])
                }
            }
        }
    }
}