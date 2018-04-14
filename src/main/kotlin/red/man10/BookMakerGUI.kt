package red.man10

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandException
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin


class BookMakerGUI {

    var redGlassPlaces = listOf(0,1,45,46)
    var blueGlassPlaces = listOf(3,4,5,48,49,50)
    var yellowGlassPlaces = listOf(7,8,52,53)
    var whiteGlassPlaces = listOf(2,11,20,29,38,47,6,15,24,33,42,51)

    var joinPhasePlaces = listOf(9,10,18,19,27,28,36,37)
    var betPhasePlaces = listOf(12,13,14,21,22,23,30,31,32,39,40,41)
    var fightPhasePlaces = listOf(16,17,25,26,34,35,43,44)

    companion object {
        var pl: BookMakerPlugin? = null
    }

    fun returnGUI(plugin: BookMakerPlugin) : BookMakerGUI{
        pl = plugin
        return BookMakerGUI()
    }

    fun openGUI(p: Player) {
        var topMenu = Bukkit.getServer().createInventory(null, 54, "§0§l[§7§lm§8§lBookMaker§0§l] §r§l開催中のゲーム")
        //var testInv = Bukkit.getServer().createInventory(null, InventoryType.ANVIL)

        var nolores: List<String> = ArrayList()

        for (place in redGlassPlaces) {
            createItem(place, topMenu, Material.STAINED_GLASS_PANE, 14, 1, "§c§l参加§f§lフェーズ§7§lのゲーム", nolores)
        }
        for (place in blueGlassPlaces) {
            createItem(place, topMenu, Material.STAINED_GLASS_PANE, 11, 1, "§9§lベット§f§lフェーズ§7§lのゲーム", nolores)
        }
        for (place in yellowGlassPlaces) {
            createItem(place, topMenu, Material.STAINED_GLASS_PANE, 4, 1, "§e§l試合§f§lフェーズ§7§lのゲーム", nolores)
        }
        for (place in whiteGlassPlaces) {
            createItem(place, topMenu, Material.STAINED_GLASS_PANE, 0, 1, " ", nolores)
        }


        var joinPhase = 0
        var betPhase = 0
        var fightPhase = 0
        for (runningGame in pl!!.gameManager.runningGames) {
            when (runningGame.value.status) {
                GameStatus.JOIN -> {
                    var lore: List<String> = listOf(
                            "§eクリックで参加登録！ (抽選)",
                            "§7現在登録者: §6" + runningGame.value.candidates.count() + "人",
                            "§8id: "+ runningGame.key
                    )
                    if (joinPhase != joinPhasePlaces.size) {
                        createItem(joinPhasePlaces[joinPhase], topMenu, runningGame.value.item, 0, 1, "§6§l§n" + runningGame.value.gameName, lore)
                        joinPhase++
                    }
                }
                GameStatus.BET -> {
                    var lore: List<String> = listOf(
                            "§eクリックでベット！",
                            "勝つと思う方にベットしよう！",
                            "§8id: "+ runningGame.key
                    )
                    if (betPhase != betPhasePlaces.size) {
                        createItem(joinPhasePlaces[betPhase], topMenu, runningGame.value.item, 0, 1, "§6§l" + runningGame.value.gameName, lore)
                        betPhase++
                    }
                }
                GameStatus.FIGHT -> {
                    var lore: List<String> = listOf(
                            "§eクリックで試合を観戦しよう！",
                            "§8id: "+ runningGame.key
                    )
                    if (fightPhase != fightPhasePlaces.size) {
                        createItem(joinPhasePlaces[fightPhase], topMenu, runningGame.value.item, 0, 1, "§6§l" + runningGame.value.gameName, lore)
                        fightPhase++
                    }
                }
                else -> {
                    p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l問題が発生しました。運営に報告してください。")
                }
            }
        }
        p.openInventory(topMenu)
    }

    fun createItem(place: Int?, gui: Inventory, material: Material, itemtype: Short?, amount: Int?, itemName: String, loreList: List<String>) {
        val CIitemStack = ItemStack(material, amount!!, itemtype!!)
        val CIitemMeta = CIitemStack.itemMeta
        CIitemMeta.displayName = itemName
        CIitemMeta.lore = loreList
        CIitemStack.itemMeta = CIitemMeta
        gui.setItem(place!!, CIitemStack)
    }
}

