package red.man10

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
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

    var numberTextures = listOf(
        "http://textures.minecraft.net/texture/3f09018f46f349e553446946a38649fcfcf9fdfd62916aec33ebca96bb21b5",
        "http://textures.minecraft.net/texture/ca516fbae16058f251aef9a68d3078549f48f6d5b683f19cf5a1745217d72cc",
        "http://textures.minecraft.net/texture/4698add39cf9e4ea92d42fadefdec3be8a7dafa11fb359de752e9f54aecedc9a",
        "http://textures.minecraft.net/texture/b85d4fda56bfeb85124460ff72b251dca8d1deb6578070d612b2d3adbf5a8",
        "http://textures.minecraft.net/texture/f2a3d53898141c58d5acbcfc87469a87d48c5c1fc82fb4e72f7015a3648058",
        "http://textures.minecraft.net/texture/d1fe36c4104247c87ebfd358ae6ca7809b61affd6245fa984069275d1cba763",
        "http://textures.minecraft.net/texture/3ab4da2358b7b0e8980d03bdb64399efb4418763aaf89afb0434535637f0a1",
        "http://textures.minecraft.net/texture/297712ba32496c9e82b20cc7d16e168b035b6f89f3df014324e4d7c365db3fb",
        "http://textures.minecraft.net/texture/abc0fda9fa1d9847a3b146454ad6737ad1be48bdaa94324426eca0918512d",
        "http://textures.minecraft.net/texture/d6abc61dcaefbd52d9689c0697c24c7ec4bc1afb56b8b3755e6154b24a5d8ba"
    )
    var numberPlaces = listOf(46, 19, 20, 21, 28, 29, 30, 37, 38, 39)

    var numberStacks = mutableListOf<ItemStack>()

    var textCSM = listOf(66,69,84)
    var textPlaces = listOf(23, 24, 25)

    var currentNumbers: MutableMap<String, String> = mutableMapOf()

    companion object {
        lateinit var pl: BookMakerPlugin
    }

    fun returnGUI(plugin: BookMakerPlugin) : BookMakerGUI{
        pl = plugin
        return BookMakerGUI()
    }

    fun openTopMenu(p: Player) {
        val topMenu = Bukkit.getServer().createInventory(null, 54, "§0§l[§7§lm§8§lBookMaker§0§l] §r§l開催中のゲーム")
        //var testInv = Bukkit.getServer().createInventory(null, InventoryType.ANVIL)

        val nolores: List<String> = ArrayList()

        for (place in redGlassPlaces) {
            createItem(place, topMenu, Material.GLASS_PANE, 0, 1, "§c§l参加登録受付中§f§lのゲーム", nolores)
        }
        for (place in blueGlassPlaces) {
            createItem(place, topMenu, Material.BLUE_STAINED_GLASS_PANE, 0, 1, "§9§lベット受付中§f§lのゲーム", nolores)
        }
        for (place in yellowGlassPlaces) {
            createItem(place, topMenu, Material.YELLOW_STAINED_GLASS_PANE, 0, 1, "§e§l試合中§f§lのゲーム", nolores)
        }
        for (place in dividerPlaces) {
            createItem(place, topMenu, Material.IRON_BARS, 0, 1, " ", nolores)
        }


        var joinPhase = 0
        var betPhase = 0
        var fightPhase = 0
        for (runningGame in pl.gameManager.runningGames) {
            when (runningGame.value.status) {
                GameStatus.JOIN -> {
                    val lore: List<String> = listOf(
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
                    val lore: List<String> = if (!pl.gameManager.uuidMap.keys.contains(runningGame.value.players.keys.first())) {
                        mutableListOf(
                            "§eクリックでベット！",
                            "§f勝つと思う方に賭けて、配当金を受け取ろう！",
                            "§8id: " + runningGame.key
                        )
                    } else {
                        mutableListOf(
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
                    val lore: MutableList<String> = mutableListOf()
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
                    p.sendMessage(pl.prefix + "§4§lERROR: §f§l問題が発生しました。運営に報告してください。")
                }
            }
        }
        p.openInventory(topMenu)
    }

    fun openPlayerSelectMenu(p: Player, gameId: String) {
        if (pl.gameManager.runningGames[gameId] != null) {
            if (pl.gameManager.runningGames[gameId]!!.status == GameStatus.BET) {
                val players = pl.gameManager.runningGames[gameId]!!.players.keys
                if (players.size <= 6) {
                    if (!pl.gameManager.uuidMap.keys.contains(players.first())) {
                        val playerSelectMenu = Bukkit.getServer().createInventory(null, 9, "§0§l[§7§lm§8§lBookMaker§0§l] §r§l勝者を予想してベット!")
                        for ((i, place) in fighterPlaces[players.size - 1].withIndex()) {
                            //createItem(place, playerSelectMenu, Material.STICK, 0, 1, "i", listOf())
                            val playerName: String = Bukkit.getPlayer(players.toMutableList()[i])?.name?:""
                            val dataLore = listOf(
                                    "§eクリックでベット!",
                                    "§8id: " + (players.toMutableList()[i]),
                                "§8game: $gameId"
                            )
                            createSkull(playerName, "§c§lP" + (i + 1).toString() + ": §f§l" + playerName, playerSelectMenu, place, dataLore)
                        }
                        p.openInventory(playerSelectMenu)
                    } else {
                        //AskType
                        val playerSelectMenu = Bukkit.getServer().createInventory(null, 9, "§0§l[§7§lm§8§lBookMaker§0§l] §r§l正解を予想してベット!")
                        for ((i, place) in fighterPlaces[players.size - 1].withIndex()) {
                            //createItem(place, playerSelectMenu, Material.STICK, 0, 1, "i", listOf())
                            val selectionText: String = pl.gameManager.uuidMap[players.toList()[i]]!!
                            val dataLore = listOf(
                                    "§eクリックでベット!",
                                    "§8id: " + (players.toMutableList()[i]),
                                "§8game: $gameId"
                            )
                            createItem(place, playerSelectMenu, Material.OAK_SIGN, null, 1, "§c§l" + (i + 1).toString() + ": §f§l" + selectionText, dataLore)
                        }
                        p.openInventory(playerSelectMenu)
                    }
                } else {
                    p.sendMessage(pl.prefix + "§4§lERROR: §f§l問題が発生しました。エラーコード001")
                }
            } else {
                p.sendMessage(pl.prefix + "§l今はベットフェーズではありません。")
            }
        } else {
            p.sendMessage(pl.prefix + "§lゲームが存在しません。")
        }
    }

    fun openBetMenu(p: Player, gameId: String, bettedUUID: UUID) {
        if (pl.gameManager.runningGames[gameId] != null) {
            if (pl.gameManager.runningGames[gameId]!!.status == GameStatus.BET) {
                if (currentNumbers[p.name] == null) {
                    currentNumbers[p.name] = ""
                } else {
                    currentNumbers[p.name] = ""
                }

                val betMenu = Bukkit.getServer().createInventory(null, 54, "§0§l[§7§lm§8§lBookMaker§0§l] §r§lベット金額を入力")
                for (i in 9..53) {
                    createItem(i, betMenu, Material.GLASS_PANE, 15, 1, " ", listOf())
                }
                for (i in 0..9) {
                    val numberItemStack = SkullMaker().withSkinUrl(numberTextures[i]).withName("§r§l$i").build()
                    numberStacks.add(numberItemStack)
                    betMenu.setItem(numberPlaces[i], numberItemStack)
                }
                for (i in 0..2) {
                    val numberItemStack = ItemStack(Material.QUARTZ)
                    val numberItemMeta = numberItemStack.itemMeta
                    numberItemMeta?.setDisplayName(" ")
                    numberItemMeta?.setCustomModelData(textCSM[i])
                    numberItemStack.itemMeta = numberItemMeta
                    betMenu.setItem(textPlaces[i], numberItemStack)
                }
                createItem(48, betMenu, Material.TNT, 0, 1, "§4§lリセット", listOf())
                createItem(41, betMenu, Material.EMERALD_BLOCK, 0, 1, "§a§l決定", listOf(
                    "§8id: $gameId",
                    "§8p: $bettedUUID"
                ))
                createItem(43, betMenu, Material.REDSTONE_BLOCK, 0, 1, "§4§lキャンセル", listOf())

                p.openInventory(betMenu)
            } else {
                p.sendMessage("§4§lERROR: §f§l試合がすでに始まっています")
            }
        }
    }

    fun setBetNumber(p: Player) {
        val numberString = currentNumbers[p.name]
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

    private fun createItem(place: Int?, gui: Inventory, material: Material, itemtype: Int?, amount: Int, itemName: String, loreList: List<String>) {
        val cIitemStack = ItemStack(material, amount)
        val cIitemMeta = cIitemStack.itemMeta
        cIitemMeta?.setDisplayName(itemName)
        cIitemMeta?.lore = loreList
        cIitemMeta?.setCustomModelData(itemtype)
        cIitemStack.itemMeta = cIitemMeta
        gui.setItem(place!!, cIitemStack)
    }

    private fun createSkull(username: String, itemName: String, gui: Inventory, place: Int, lore: List<String>) {
        val skull = ItemStack(Material.PLAYER_HEAD, 1)
        val meta = skull.itemMeta as SkullMeta
        meta.setDisplayName(itemName)
        meta.owningPlayer = Bukkit.getOfflinePlayer(username)
        meta.lore = lore
        skull.itemMeta = meta
        gui.setItem(place, skull)
    }
}

