package red.man10

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class BookMakerListener: Listener {

    companion object {
        var pl: BookMakerPlugin? = null
    }

    fun returnListener(plugin: BookMakerPlugin) : BookMakerListener{
        pl = plugin
        return BookMakerListener()
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        if (e.inventory.title == "§0§l[§7§lm§8§lBookMaker§0§l] §r§l開催中のゲーム") {
            e.isCancelled = true
            try {
                if (e.currentItem.itemMeta.lore != null) {
                    val id = e.currentItem.itemMeta.lore.last().substring(6)
                    when (pl!!.gameManager.runningGames[id]!!.status) {
                        GameStatus.JOIN -> {
                            pl!!.gameManager.addCandidate(e.whoClicked as Player, id)
                            e.whoClicked.closeInventory()
                        }
                        GameStatus.BET -> {

                        }
                        GameStatus.FIGHT -> {

                        }
                        GameStatus.OFF -> {
                            e.whoClicked.sendMessage("§4§lERROR: §f§lエラー: OPに報告してください")
                        }
                    }
                }
            }
            catch(e: Exception) {

            }
        }
    }
}