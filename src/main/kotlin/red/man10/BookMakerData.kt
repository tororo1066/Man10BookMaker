package red.man10

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player

class BookMakerData {

    companion object {
        var pl: BookMakerPlugin? = null
        var mysql: MySQLManager? = null
    }

    fun returnData(plugin: BookMakerPlugin) : BookMakerData{
        pl = plugin
        print("t------e----s---ttt")
        mysql = MySQLManager(pl!!, "Bookmaker")
        return BookMakerData()
    }

    fun test() {
        var sql = "select * from games"
        var q = mysql!!.query(sql)
        while (q.next()) {
            print(q.getDouble("start_x"))
            print(q.wasNull())
        }
        mysql!!.close()
    }

    fun reload(p: CommandSender) {
        pl!!.listener.hasBMWorld = (Bukkit.getWorld("bookmaker") != null) //ワールドリロード

        //以下mysqlロード
        var newGames = mutableMapOf<String, Game>()

        var sql = "select * from games"
        var rs = mysql!!.query(sql)

        while (rs.next()) {
            //itemが存在するアイテムかの確認
            var gameItem: Material = Material.STONE
            try {
                gameItem = Material.valueOf(rs.getString("item"))
            } catch (e: Exception) {
                p.sendMessage(BookMakerGameManager.pl!!.prefix + "§e§lWARNING: §r§fゲーム「" + rs.getString("id") + "」のitemが存在しないアイテムです。")
            }

            var startingLocation: Location? = null
            if (pl!!.listener.hasBMWorld) {
                startingLocation = Location(
                        Bukkit.getWorld("bookmaker"),
                        rs.getDouble("start_x"),
                        rs.getDouble("start_y"),
                        rs.getDouble("start_z")
                )
                if (rs.wasNull()) {
                    startingLocation = null
                }
            }

            var viewingLocation: Location? = null
            if (pl!!.listener.hasBMWorld) {
                viewingLocation = Location(
                        Bukkit.getWorld("bookmaker"),
                        rs.getDouble("view_x"),
                        rs.getDouble("view_y"),
                        rs.getDouble("view_z")
                )
            }

            var duration = mutableListOf<Int>()
            duration.add(rs.getInt("join_duration"))
            duration.add(rs.getInt("bet_duration"))
            duration.add(rs.getInt("fight_duration"))

            if (pl!!.gameManager.loadedGames[rs.getString("id")] == null) {
                //新ゲーム
                var newGame = Game(
                        rs.getString("game_name"),
                        gameItem,
                        rs.getInt("fighter_number"),
                        rs.getDouble("join_fee"),
                        rs.getDouble("tax"),
                        rs.getDouble("prize"),
                        startingLocation,
                        viewingLocation,
                        duration,
                        GameStatus.OFF,
                        mutableMapOf(),
                        mutableListOf(),
                        0.0
                )
                newGames.put(rs.getString("id"), newGame)
            } else {
                var changingGame = pl!!.gameManager.loadedGames[rs.getString("id")]!!
                changingGame.gameName = rs.getString("game_name")
                changingGame.item = gameItem
                changingGame.playerNumber = rs.getInt("fighter_number")
                changingGame.joinFee = rs.getDouble("join_fee")
                changingGame.tax = rs.getDouble("tax")
                changingGame.prize = rs.getDouble("prize")
                changingGame.startingLocation = startingLocation
                changingGame.viewingLocation = viewingLocation
                changingGame.duration = duration
                newGames.put(rs.getString("id"), changingGame)
            }
        }
        pl!!.gameManager.loadedGames = newGames
        pl!!.listener.gameIDs = mutableListOf()
        for (loadedGameID in pl!!.gameManager.loadedGames.keys) {
            pl!!.listener.gameIDs.add(loadedGameID)
        }
    }
}

//    fun setUpGames(p: Player?) {
//        var newGames = mutableMapOf<String, Game>()
//        val keys = config.getKeys(false)
//
//        pl!!.listener.hasBMWorld = (Bukkit.getWorld("bookmaker") != null)
//
//        for (gameKey in keys) {
//            //gameに不備がないか確認
//            //FLAG
//            if (config.getKeys(true).containsAll(listOf(gameKey + ".name", gameKey + ".playerNumber", gameKey + ".item", gameKey + ".joinFee", gameKey + ".tax", gameKey + ".prize"))) {
//
//                //itemが存在するアイテムかの確認
//                var gameItem: Material = Material.STONE
//                try {
//                    gameItem = Material.valueOf(config.getString(gameKey + ".item"))
//                } catch (e: Exception) {
//                    //コンソール・プレイヤー分岐
//                    if (p == null) {
//                        print(BookMakerGameManager.pl!!.prefix + "§e§lWARNING: §r§fゲーム「" + gameKey + "」のitemが存在しないアイテムです。")
//                    } else {
//                        p.sendMessage(BookMakerGameManager.pl!!.prefix + "§e§lWARNING: §r§fゲーム「" + gameKey + "」のitemが存在しないアイテムです。")
//                    }
//                }
//
//                var startLocation: Location? = null
//                if (config.getKeys(true).containsAll(listOf(gameKey + ".x", gameKey + ".y", gameKey + ".x"))) {
//                    if (BookMakerGameManager.pl!!.listener.hasBMWorld) {
//                        startLocation = Location(
//                                Bukkit.getWorld("bookmaker"),
//                                config.getDouble(gameKey + ".x"),
//                                config.getDouble(gameKey + ".y"),
//                                config.getDouble(gameKey + ".z"))
//                    }
//                } else {
//                }
//
//                var durationList = mutableListOf<Int>()
//                if (config.getKeys(true).containsAll(listOf(gameKey + ".time1", gameKey + ".time2", gameKey + ".time3"))) {
//                    durationList.add(config.getInt(gameKey + ".time1"))
//                    durationList.add(config.getInt(gameKey + ".time2"))
//                    durationList.add(config.getInt(gameKey + ".time3"))
//                } else {
//                    durationList = mutableListOf(120, 120, 120)
//                }
//
//
//                var viewLocation: Location? = null
//                if (config.getKeys(true).containsAll(listOf(gameKey + ".vx", gameKey + ".vy", gameKey + ".vx"))) {
//                    if (BookMakerGameManager.pl!!.listener.hasBMWorld) {
//                        viewLocation = Location(
//                                Bukkit.getWorld("bookmaker"),
//                                config.getDouble(gameKey + ".vx"),
//                                config.getDouble(gameKey + ".vy"),
//                                config.getDouble(gameKey + ".vz"))
//                    }
//                } else {
//                }
//
//                //仮配列にゲームを追加
//                if (loadedGames[gameKey] == null) {
//                    //新しいゲーム
//                    newGames.put(
//                            gameKey,
//                            //FLAG
//                            Game(
//                                    config.getString(gameKey + ".name"),
//                                    gameItem,
//                                    config.getInt(gameKey + ".playerNumber"),
//                                    config.getInt(gameKey + ".joinFee").toDouble(),
//                                    config.getDouble(gameKey + ".tax"),
//                                    config.getDouble(gameKey + ".prize"),
//                                    startLocation,
//                                    viewLocation,
//                                    durationList,
//                                    GameStatus.OFF,
//                                    mutableMapOf(),
//                                    mutableListOf(),
//                                    0.0
//                            )
//                    )
//                } else {
//                    //元々あったゲームの改変
//                    //FLAG
//                    var changingGame = loadedGames[gameKey]!!
//                    changingGame.gameName = config.getString(gameKey + ".name")
//                    changingGame.item = gameItem
//                    changingGame.playerNumber = config.getInt(gameKey + ".playerNumber")
//                    changingGame.joinFee = config.getInt(gameKey + ".joinFee").toDouble()
//                    changingGame.tax = config.getDouble(gameKey + ".tax")
//                    changingGame.prize = config.getDouble(gameKey + ".prize")
//                    changingGame.startingLocation = startLocation
//                    changingGame.viewingLocation = viewLocation
//                    changingGame.duration = durationList
//                    newGames.put(gameKey, changingGame)
//                }
//            } else {
//                //configに不備がある
//                if (p == null) {
//                    print(BookMakerGameManager.pl!!.prefix + "§4§lERROR: §f§lゲーム「" + gameKey + "」のconfigに不備があります。")
//                } else {
//                    p.sendMessage(BookMakerGameManager.pl!!.prefix + "§4§lERROR: §f§lゲーム「" + gameKey + "」のconfigに不備があります。")
//                }
//            }
//        }
//        loadedGames = newGames
//
//        BookMakerGameManager.pl!!.listener.gameIDs = mutableListOf()
//        for (loadedGameID in loadedGames.keys) {
//            BookMakerGameManager.pl!!.listener.gameIDs.add(loadedGameID)
//        }
//    }