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


class BookMakerGUI {

    var redGlassPlaces = listOf(0,1,45,46)
    var blueGlassPlaces = listOf(3,4,5,48,49,50)
    var yellowGlassPlaces = listOf(7,8,52,53)
    var dividerPlaces= listOf(2,11,20,29,38,47,6,15,24,33,42,51)

    var joinPhasePlaces = listOf(9,10,18,19,27,28,36,37)
    var betPhasePlaces = listOf(12,13,14,21,22,23,30,31,32,39,40,41)
    var fightPhasePlaces = listOf(16,17,25,26,34,35,43,44)

    var fighterPlaces = listOf(listOf(4), listOf(3,5), listOf(2,4,6), listOf(1,3,5,7), listOf(0,2,4,6,8))

    var numberDurabilities = mapOf<Int, Short>(0 to 933, 1 to 932, 2 to 931, 3 to 930, 4 to 929, 5 to 928, 6 to 927, 7 to 926, 8 to 925, 9 to 924)
    var numberPlaces = listOf(46, 19, 20, 21, 28, 29, 30, 37, 38, 39)

    var numberStacks = mutableListOf<ItemStack>()

    var textDurabilities = listOf(958, 955, 940)
    var textPlaces = listOf(23, 24, 25)

    var currentNumbers: MutableMap<String, String> = mutableMapOf()

    companion object {
        var pl: BookMakerPlugin? = null
    }

    fun returnGUI(plugin: BookMakerPlugin) : BookMakerGUI{
        pl = plugin
        return BookMakerGUI()
    }

    fun openTopMenu(p: Player) {
        var topMenu = Bukkit.getServer().createInventory(null, 54, "§0§l[§7§lm§8§lBookMaker§0§l] §r§l開催中のゲーム")
        //var testInv = Bukkit.getServer().createInventory(null, InventoryType.ANVIL)

        var nolores: List<String> = ArrayList()

        for (place in redGlassPlaces) {
            createItem(place, topMenu, Material.STAINED_GLASS_PANE, 14, 1, "§c§l参加登録受付中§f§lのゲーム", nolores)
        }
        for (place in blueGlassPlaces) {
            createItem(place, topMenu, Material.STAINED_GLASS_PANE, 11, 1, "§9§lベット受付中§f§lのゲーム", nolores)
        }
        for (place in yellowGlassPlaces) {
            createItem(place, topMenu, Material.STAINED_GLASS_PANE, 4, 1, "§e§l試合中§f§lのゲーム", nolores)
        }
        for (place in dividerPlaces) {
            createItem(place, topMenu, Material.IRON_FENCE, 0, 1, " ", nolores)
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
                            "§7プレイ人数: §6" + runningGame.value.playerNumber + "人",
                            "§7参加費: §6" + runningGame.value.joinFee + "円",
                            "§8id: "+ runningGame.key
                    )
                    if (joinPhase != joinPhasePlaces.size) {
                        createItem(joinPhasePlaces[joinPhase], topMenu, runningGame.value.item, 0, 1, "§6§l§n" + runningGame.value.gameName, lore)
                        joinPhase++
                    }
                }
                GameStatus.BET -> {
                    var lore: List<String> = mutableListOf()
                    if (!pl!!.gameManager.UUIDMap.keys.contains(runningGame.value.players.keys.first())) {
                        lore = mutableListOf(
                                "§eクリックでベット！",
                                "§f勝つと思う方に賭けて、配当金を受け取ろう！",
                                "§8id: " + runningGame.key
                        )
                    } else {
                        lore = mutableListOf(
                                "§eクリックでベット！",
                                "§f正解だと思う方に賭けて、賞金を受け取ろう！",
                                "§8id: " + runningGame.key
                        )
                    }
                    if (betPhase != betPhasePlaces.size) {
                        createItem(betPhasePlaces[betPhase], topMenu, runningGame.value.item, 0, 1, "§6§l§n" + runningGame.value.gameName, lore)
                        betPhase++
                    }
                }
                GameStatus.FIGHT -> {
                    var lore: MutableList<String> = mutableListOf()
                    if (runningGame.value.viewingLocation != null) {
                        lore.add("§eクリックで観戦しよう!")
                    }
                    lore.add("§8id: "+ runningGame.key)
                    if (fightPhase != fightPhasePlaces.size) {
                        createItem(fightPhasePlaces[fightPhase], topMenu, runningGame.value.item, 0, 1, "§6§l§n" + runningGame.value.gameName, lore)
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

    fun openPlayerSelectMenu(p: Player, gameId: String) {
        if (pl!!.gameManager.runningGames[gameId] != null) {
            if (pl!!.gameManager.runningGames[gameId]!!.status == GameStatus.BET) {
                val players = pl!!.gameManager.runningGames[gameId]!!.players.keys
                if (players.size <= 6) {
                    if (!pl!!.gameManager.UUIDMap.keys.contains(players.first())) {
                        var playerSelectMenu = Bukkit.getServer().createInventory(null, 9, "§0§l[§7§lm§8§lBookMaker§0§l] §r§l勝者を予想してベット!")
                        var i = 0
                        for (place in fighterPlaces[players.size - 1]) {
                            //createItem(place, playerSelectMenu, Material.STICK, 0, 1, "i", listOf())
                            var playerName: String = Bukkit.getOfflinePlayer(players.toMutableList()[i]).name
                            var dataLore = listOf(
                                    "§eクリックでベット!",
                                    "§8id: " + (players.toMutableList()[i]),
                                    "§8game: " + gameId)
                            createSkull(playerName, "§c§lP" + (i + 1).toString() + ": §f§l" + playerName, playerSelectMenu, place, dataLore)
                            i++
                        }
                        p.openInventory(playerSelectMenu)
                    } else {
                        //AskType
                        var playerSelectMenu = Bukkit.getServer().createInventory(null, 9, "§0§l[§7§lm§8§lBookMaker§0§l] §r§l正解を予想してベット!")
                        var i = 0
                        for (place in fighterPlaces[players.size - 1]) {
                            //createItem(place, playerSelectMenu, Material.STICK, 0, 1, "i", listOf())
                            var selectionText: String = pl!!.gameManager.UUIDMap[players.toList()[i]]!!
                            var dataLore = listOf(
                                    "§eクリックでベット!",
                                    "§8id: " + (players.toMutableList()[i]),
                                    "§8game: " + gameId)
                            createItem(place, playerSelectMenu, Material.SIGN, 0.toShort(), 1, "§c§l" + (i + 1).toString() + ": §f§l" + selectionText, dataLore)
                            i++
                        }
                        p.openInventory(playerSelectMenu)
                    }
                } else {
                    p.sendMessage(pl!!.prefix + "§4§lERROR: §f§l問題が発生しました。エラーコード001")
                }
            }
        }
    }

    fun openBetMenu(p: Player, gameId: String, bettedUUID: UUID) {
        if (pl!!.gameManager.runningGames[gameId] != null) {
            if (pl!!.gameManager.runningGames[gameId]!!.status == GameStatus.BET) {
                if (currentNumbers[p.name] == null) {
                    currentNumbers.put(p.name, "")
                } else {
                    currentNumbers[p.name] = ""
                }

                var betMenu = Bukkit.getServer().createInventory(null, 54, "§0§l[§7§lm§8§lBookMaker§0§l] §r§lベット金額を入力")
                for (i in 9..53) {
                    createItem(i, betMenu, Material.STAINED_GLASS_PANE, 15, 1, " ", listOf())
                }
                for (i in 0..9) {
                    val numberItemStack = ItemStack(Material.DIAMOND_HOE, 1, numberDurabilities[i]!!.toShort())
                    val numberItemMeta = numberItemStack.itemMeta
                    numberItemMeta.displayName = "§r§l" + i.toString()
                    numberItemMeta.isUnbreakable = true
                    numberItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                    numberItemStack.itemMeta = numberItemMeta
                    numberStacks.add(numberItemStack)
                    betMenu.setItem(numberPlaces[i], numberItemStack)
                }
                for (i in 0..2) {
                    val numberItemStack = ItemStack(Material.DIAMOND_HOE, 1, textDurabilities[i].toShort())
                    val numberItemMeta = numberItemStack.itemMeta
                    numberItemMeta.displayName = " "
                    numberItemMeta.isUnbreakable = true
                    numberItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                    numberItemStack.itemMeta = numberItemMeta
                    betMenu.setItem(textPlaces[i], numberItemStack)
                }
                createItem(48, betMenu, Material.TNT, 0, 1, "§4§lリセット", listOf())
                createItem(41, betMenu, Material.EMERALD_BLOCK, 0, 1, "§a§l決定", listOf("§8id: " + gameId, "§8p: " + bettedUUID.toString()))
                createItem(43, betMenu, Material.REDSTONE_BLOCK, 0, 1, "§4§lキャンセル", listOf())
                p.openInventory(betMenu)
            } else {
                p.sendMessage("§4§lERROR: §f§l試合がすでに始まっています")
            }
        }
    }

    fun setBetNumber(p: Player) {
        var numberString = currentNumbers[p.name]
        var oppositeNumberString = ""
        for (char in numberString!!) {
            oppositeNumberString = char + oppositeNumberString
        }
        var i = 8
        for (char in oppositeNumberString) {
            p.openInventory.topInventory.setItem(i, numberStacks[char.toString().toInt()]) //toString挟まないとおかしくなる
            i--
        }
    }

    fun resetBetNumber(p: Player) {
        currentNumbers[p.name] = ""
        for (i in 0..8) {
            p.openInventory.topInventory.setItem(i, ItemStack(Material.AIR))
        }
    }

    fun createItem(place: Int?, gui: Inventory, material: Material, itemtype: Short?, amount: Int?, itemName: String, loreList: List<String>) {
        val CIitemStack = ItemStack(material, amount!!, itemtype!!)
        val CIitemMeta = CIitemStack.itemMeta
        CIitemMeta.displayName = itemName
        CIitemMeta.lore = loreList
        CIitemStack.itemMeta = CIitemMeta
        gui.setItem(place!!, CIitemStack)
    }

    fun createSkull(username: String, itemName: String, gui: Inventory, place: Int, lore: List<String>) {
        val skull = ItemStack(Material.SKULL_ITEM, 1, 3.toShort())
        val meta = skull.itemMeta as SkullMeta
        meta.displayName = itemName
        meta.owner = username
        meta.lore = lore
        skull.itemMeta = meta
        gui.setItem(place, skull)
    }
}

