package red.man10

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.nio.file.Files.setOwner
import org.bukkit.inventory.meta.SkullMeta
import java.math.BigDecimal
import java.nio.file.Files.list
import java.util.*


class BookMakerSidebar {

    var sideBar = SidebarDisplay()

    companion object {
        lateinit var pl: BookMakerPlugin
    }

    fun returnSidebar(plugin: BookMakerPlugin) : BookMakerSidebar{
        pl = plugin
        return BookMakerSidebar()
    }

    fun showOdds(game: Game) {
        sideBar.remove()
        sideBar = SidebarDisplay()
        sideBar.setTitle("§l==( §a§lm§6§lBookMaker§f§l )==")
        sideBar.setScore("§lゲーム: §a§l" + game.gameName, 9)
        sideBar.setScore("§l総賭け金: §a§l" + pl.gameManager.getTotalPrice(game.players), 8)
        sideBar.setScore("§lオッズ:", 7)
        for (fighter in game.players) {
            sideBar.setScore("§c§l" + Bukkit.getPlayer(fighter.key)?.name + ": §a§l" + pl.gameManager.getOdds(game.players, fighter.key, game.tax, game.prize).roundTo2DecimalPlaces() + "倍", 6)
        }
        sideBar.setScore("§e§l勝者を予想して、/mb でベット!", 5)
        showToAll()
    }

    fun showCandidates(game: Game, gameId: String) {
        sideBar.remove()
        sideBar = SidebarDisplay()
        if (pl.mode == MBGameMode.WHITELIST){
            sideBar.setTitle("§l==( §f§lWhitelist §a§lm§6§lBookMaker§f§l )==")
        }else{
            sideBar.setTitle("§l==( §a§lm§6§lBookMaker§f§l )==")
        }
        sideBar.setScore("§lゲーム: §a§l" + game.gameName, 9)
        sideBar.setScore("§l参加応募者:", 9)
        for (candidate in game.candidates) {
            if (BookMakerGameManager.pl.data.getBestRecord(gameId, candidate) != null) {
                sideBar.setScore("§c§l" + Bukkit.getPlayer(candidate)?.name + " §e§l最高記録: " + pl.data.getBestRecord(gameId, candidate).toString() + "秒", 7)
            } else {
                sideBar.setScore("§c§l" + Bukkit.getPlayer(candidate)?.name + " §e§l記録無し", 7)
            }
        }
        sideBar.setScore("§e§l/mbで試合に参加登録!", 5)
        showToAll()
    }

    private fun showToAll() {
        for (player in Bukkit.getServer().onlinePlayers) {
            sideBar.setMainScoreboard(player)
            sideBar.setShowPlayer(player)

        }
    }

    fun showWhileFight(game: Game) {
        sideBar.remove()
        sideBar = SidebarDisplay()
        sideBar.setTitle("§l==( §a§lm§6§lBookMaker§f§l )==")
        sideBar.setScore("§lゲーム: §a§l" + game.gameName, 9)
        sideBar.setScore("§l総賭け金: §a§l" + pl.gameManager.getTotalPrice(game.players), 8)
        sideBar.setScore("§lオッズ:", 7)
        for (fighter in game.players) {
            sideBar.setScore("§c§l" + Bukkit.getPlayer(fighter.key)?.name + ": §a§l" + pl.gameManager.getOdds(game.players, fighter.key, game.tax, game.prize).roundTo2DecimalPlaces() + "倍", 6)
        }
        sideBar.setScore("§e§l/mb で試合観戦!", 5)
        showToAll()
    }

    fun removeAll() {
        sideBar.remove()
    }

    private fun Double.roundTo2DecimalPlaces() = BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
}

