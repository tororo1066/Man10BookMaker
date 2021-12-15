package red.man10

import org.bukkit.Bukkit
import java.math.BigDecimal
import java.util.*

class BookMakerData {

    companion object {
        lateinit var pl: BookMakerPlugin
        lateinit var mysql: MySQLManager
    }

    fun returnData(plugin: BookMakerPlugin) : BookMakerData{
        pl = plugin
        print("t------e----s---ttt")
        mysql = MySQLManager(pl, "Bookmaker")
        mysql.execute(fightsTable)
        mysql.execute(fightersTable)
        mysql.execute(betsTable)
        return BookMakerData()
    }

    fun saveFight(gameKey: String, game: Game, winnerUUID: UUID) {
        val query = mysql.execute("insert into fights values (0, NULL, '" + gameKey + "', " + game.gameTimer + ")")
        if (query) {
            val fightIdResult = mysql.query("SELECT id FROM fights;")
            val fightKeys = mutableListOf<Int>()
            while (fightIdResult.next()) {
                fightKeys.add(fightIdResult.getInt("id"))
            }
            val fightId = fightKeys.max()

            for (fighter in game.players) {

                mysql.execute("insert into fighters values (0, " + fightId + ", '" + Bukkit.getOfflinePlayer(fighter.key).name + "', '" + fighter.key + "')")

                val fighterIdResult = mysql.query("SELECT id FROM fighters;")
                val fighterKeys = mutableListOf<Int>()
                while (fighterIdResult.next()) {
                    fighterKeys.add(fighterIdResult.getInt("id"))
                }
                val fighterId = fighterKeys.max()

                for (bet in fighter.value) {
                    if (Bukkit.getOfflinePlayer(bet.playerUUID).name == null) {
                        mysql.execute("insert into bets values (0, " + fighterId + ", " + bet.price + ", '" + bet.playerUUID + "', 'VIRTUAL_BET')")
                    } else {
                        mysql.execute("insert into bets values (0, " + fighterId + ", " + bet.price + ", '" + bet.playerUUID + "', '" + Bukkit.getOfflinePlayer(bet.playerUUID).name + "')")
                    }
                }

                if (fighter.key == winnerUUID) {
                    mysql.execute("UPDATE fights SET winner_id = " + fighterId + " WHERE id = " + fightId)
                }
            }
        } else {
            Bukkit.broadcastMessage(pl.prefix + "§lDATABASE ERROR")
        }
        mysql.close()
    }

    fun saveQuestion(gameKey: String, game: Game, winnerUUID: UUID) {
        val query = mysql.execute("insert into fights values (0, NULL, '" + gameKey + "', " + game.gameTimer + ")")
        if (query) {
            val fightIdResult = mysql.query("SELECT id FROM fights;")
            fightIdResult.afterLast()
            fightIdResult.previous()
            val fightId = fightIdResult.getInt("id")


            for (fighter in game.players) {

                mysql.execute("insert into fighters values (0, " + fightId + ", '" + Bukkit.getOfflinePlayer(fighter.key).name + "', '" + fighter.key + "')")

                val fighterIdResult = mysql.query("SELECT id FROM fighters;")
                fighterIdResult.afterLast()
                fighterIdResult.previous()
                val fighterId = fighterIdResult.getInt("id")

                for (bet in fighter.value) {
                    if (Bukkit.getOfflinePlayer(bet.playerUUID).name == null) {
                        mysql.execute("insert into bets values (0, " + fighterId + ", " + bet.price + ", '" + bet.playerUUID + "', 'VIRTUAL_BET')")
                    } else {
                        mysql.execute("insert into bets values (0, " + fighterId + ", " + bet.price + ", '" + bet.playerUUID + "', '" + Bukkit.getOfflinePlayer(bet.playerUUID).name + "')")
                    }
                }

                if (fighter.key == winnerUUID) {
                    mysql.execute("UPDATE fights SET winner_id = " + fighterId + " WHERE id = " + fightId)
                }
            }
        } else {
            Bukkit.broadcastMessage(pl.prefix + "§lDATABASE ERROR")
        }
        mysql.close()
    }

    fun getBestRecord(gameId: String, uuid: UUID): Double? {
        val rs = mysql.query("SELECT * FROM fights JOIN fighters ON fights .winner_id = fighters .id WHERE uuid = '" + uuid.toString() + "' AND game_id = '$gameId';")
        val records = mutableListOf<Double>()
        while (rs.next()) {
            records.add(rs.getDouble("record"))
        }
        mysql.close()
        return if (records.count() == 0) {
            null
        } else {
            records.min()
        }
    }

    private var betsTable = "CREATE TABLE IF NOT EXISTS`bets` (\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
            "  `fighter_id` int(11) NOT NULL,\n" +
            "  `price` double(16,2) NOT NULL,\n" +
            "  `better_uuid` varchar(50) NOT NULL DEFAULT '',\n" +
            "  `better_name` varchar(20) NOT NULL DEFAULT '',\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  KEY `fighter` (`fighter_id`),\n" +
            "  CONSTRAINT `fighter` FOREIGN KEY (`fighter_id`) REFERENCES `fighters` (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"

    private var fightersTable = "CREATE TABLE IF NOT EXISTS `fighters` (\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
            "  `fight_id` int(11) NOT NULL,\n" +
            "  `name` varchar(20) NOT NULL DEFAULT 'NONAME',\n" +
            "  `uuid` varchar(50) NOT NULL DEFAULT '',\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  KEY `fight` (`fight_id`),\n" +
            "  CONSTRAINT `fight` FOREIGN KEY (`fight_id`) REFERENCES `fights` (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"

    private var fightsTable = "CREATE TABLE IF NOT EXISTS `fights` (\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
            "  `winner_id` int(11) DEFAULT NULL,\n" +
            "  `game_id` varchar(64) NOT NULL,\n" +
            "  `record` double(16,2) NOT NULL,\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  KEY `winner` (`winner_id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"

}