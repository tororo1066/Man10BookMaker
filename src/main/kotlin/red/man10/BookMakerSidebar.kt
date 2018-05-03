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
import java.nio.file.Files.list
import java.util.*


class BookMakerSidebar {

    var sideBar = SidebarDisplay()

    companion object {
        var pl: BookMakerPlugin? = null
    }

    fun returnSidebar(plugin: BookMakerPlugin) : BookMakerSidebar{
        pl = plugin
        return BookMakerSidebar()
    }

    fun showOdds(game: Game) {
        sideBar.remove()
        sideBar = SidebarDisplay()
        sideBar.setTitle("§l==( §a§lm§6§lBookMaker§f§l )==")
        sideBar.setScore("§lゲーム: §a§l" + game.gameName, 5)
        sideBar.setScore("§l総賭け金: §a§l" + pl!!.gameManager.getTotalPrice(game.players), 4)
        sideBar.setScore("§lオッズ:", 3)
        for (fighter in game.players) {
            sideBar.setScore("§c§l" + Bukkit.getPlayer(fighter.key).name + ": §a§l" + pl!!.gameManager.getOdds(game.players, fighter.key, game.tax, game.prize) + "倍", 2)
        }
        showToAll()
    }

    fun showToAll() {
        for (player in Bukkit.getServer().onlinePlayers) {
            sideBar.setMainScoreboard(player)
            sideBar.setShowPlayer(player)

        }
    }

    fun removeAll() {
        sideBar.remove()
        sideBar = SidebarDisplay()
        for (player in Bukkit.getServer().onlinePlayers) {
            sideBar.setMainScoreboard(player)
            sideBar.setShowPlayer(player)

        }
    }
}

