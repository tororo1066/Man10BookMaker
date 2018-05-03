package red.man10

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.*
import java.util.*

class BookMakerListener: Listener {

    var hasBMWorld = false
    var gameIDs = mutableListOf<String>()

//    fun loadGoals() {
//        hasBMWorld = (Bukkit.getWorld("bookmaker") != null)
//        gameIDs = mutableListOf()
//        for (loadedGameID in pl!!.gameManager.loadedGames.keys) {
//            gameIDs.add(loadedGameID)
//        }
//    }


    companion object {
        var pl: BookMakerPlugin? = null
    }

    fun returnListener(plugin: BookMakerPlugin): BookMakerListener {
        pl = plugin
        return BookMakerListener()
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        for (game in pl!!.gameManager.runningGames.values) {
            if (game.status == GameStatus.FIGHT) {
                for (fighter in game.players.keys) {
                    if (fighter == e.whoClicked.uniqueId) {
                        e.isCancelled = true
                        e.whoClicked.sendMessage(pl!!.prefix + "試合中はインベントリは操作できません。")
                        return
                    }
                }
            }
        }
        when (e.inventory.title) {
            "§0§l[§7§lm§8§lBookMaker§0§l] §r§l開催中のゲーム" -> {
                e.isCancelled = true
                if (e.currentItem.itemMeta.lore != null) {
                    val id = e.currentItem.itemMeta.lore.last().substring(6)
                    when (pl!!.gameManager.runningGames[id]!!.status) {
                        GameStatus.JOIN -> {
                            if (e.whoClicked.hasPermission("mb.join")) {
                                pl!!.gameManager.addCandidate(e.whoClicked as Player, id)
                                e.whoClicked.closeInventory()
                            } else {
                                e.whoClicked.sendMessage(pl!!.prefix + "権限がありません。")
                            }
                        }
                        GameStatus.BET -> {
                            if (e.whoClicked.hasPermission("mb.bet")) {
                                pl!!.gui.openPlayerSelectMenu(e.whoClicked as Player, id)
                            } else {
                                e.whoClicked.sendMessage(pl!!.prefix + "権限がありません。")
                            }
                        }
                        GameStatus.FIGHT -> {
                            if (e.whoClicked.hasPermission("mb.view")) {
                                if (e.currentItem.itemMeta.lore.size == 2) {
                                    pl!!.gameManager.viewTeleport(id, e.whoClicked as Player)
                                }
                            } else {
                                e.whoClicked.sendMessage(pl!!.prefix + "権限がありません。")
                            }
                        }
                        GameStatus.OFF -> {
                            e.whoClicked.sendMessage("§4§lERROR: §f§lOPに報告してください")
                        }
                    }
                }
            }

            "§0§l[§7§lm§8§lBookMaker§0§l] §r§l勝者を予想してベット!" -> {
                e.isCancelled = true
                if (e.whoClicked.hasPermission("mb.bet")) {
                    if (e.currentItem != null) {
                        var uuid = e.currentItem.itemMeta.lore[1].substring(6)
                        var gameId = e.currentItem.itemMeta.lore[2].substring(8)
                        pl!!.gui.openBetMenu(e.whoClicked as Player, gameId, UUID.fromString(uuid))
                    }
                } else {
                    e.whoClicked.sendMessage("権限がありません。")
                }
            }
            "§0§l[§7§lm§8§lBookMaker§0§l] §r§l正解を予想してベット!" -> {
                e.isCancelled = true
                if (e.whoClicked.hasPermission("mb.bet")) {
                    if (e.currentItem != null) {
                        var uuid = e.currentItem.itemMeta.lore[1].substring(6)
                        var gameId = e.currentItem.itemMeta.lore[2].substring(8)
                        pl!!.gui.openBetMenu(e.whoClicked as Player, gameId, UUID.fromString(uuid))
                    }
                } else {
                    e.whoClicked.sendMessage("権限がありません。")
                }
            }

            "§0§l[§7§lm§8§lBookMaker§0§l] §r§lベット金額を入力" -> {
                e.isCancelled = true
                if (pl!!.gui.numberPlaces.contains(e.slot)) {
                    var number = e.currentItem.itemMeta.displayName.substring(4)
                    if (pl!!.gui.currentNumbers[e.whoClicked.name]!!.length <= 8) {
                        pl!!.gui.currentNumbers[e.whoClicked.name] = (pl!!.gui.currentNumbers[e.whoClicked.name]!! + number)
                        pl!!.gui.setBetNumber(e.whoClicked as Player)
                    }
                } else {
                    when (e.currentItem.itemMeta.displayName) {
                        "§4§lリセット" -> {
                            pl!!.gui.resetBetNumber(e.whoClicked as Player)
                        }
                        "§a§l決定" -> {
                            var gameId = e.currentItem.itemMeta.lore[0].substring(6)
                            var playerUUID = UUID.fromString(e.currentItem.itemMeta.lore[1].substring(5))

                            pl!!.gameManager.bet(e.whoClicked as Player, playerUUID, gameId, pl!!.gui.currentNumbers[e.whoClicked.name]!!.toDouble())
                            e.whoClicked.closeInventory()
                        }
                        "§4§lキャンセル" -> {
                            e.whoClicked.closeInventory()
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onInventoryClose(e: InventoryCloseEvent) {
        if (e.inventory.title == "§0§l[§7§lm§8§lBookMaker§0§l] §r§lベット金額を入力") {
            pl!!.gui.currentNumbers.remove(e.player.name)
        }
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        for (i in pl!!.gameManager.runningGames) {
            when (i.value.status) {
                GameStatus.JOIN -> {
                    for (j in i.value.candidates) {
                        if (e.player.uniqueId == j) {
                            i.value.candidates.remove(j)
                        }
                    }
                }
                GameStatus.BET -> {
                    for (j in i.value.players.keys) {
                        if (e.player.uniqueId == j) {
                            Bukkit.broadcastMessage(pl!!.prefix + "§lプレイヤーが抜けたため、§6§l「" + i.value.gameName + "」§f§lが停止されました。")
                            pl!!.gameManager.stopGame(i.key)
                        }
                    }
//                    for (betList in i.value.players.values) {
//                        for (bet in betList) {
//                            if (bet.playerUUID == e.player.uniqueId) {
//                                betList.remove(bet)
//                                pl!!.vault!!.deposit(bet.playerUUID, bet.price)
//                            }
//                        }
//                    }
                }
                GameStatus.FIGHT -> {
                    for (uuid in i.value.players.keys) {
                        if (e.player.uniqueId == uuid) {
                            Bukkit.broadcastMessage(pl!!.prefix + "§lプレイヤーが抜けたため、§6§l「" + i.value.gameName + "」§f§lが停止されました。")
                            pl!!.gameManager.stopGame(i.key)
                        }
                    }
                }
                else -> {
                }

            }
        }
    }

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        if (pl!!.freezedPlayer.contains(e.player.uniqueId)) {
            e.player.teleport(e.from)
        }
        if (hasBMWorld) {
            if (e.player.world.name == "bookmaker") {
                var regions = pl!!.worldguard!!.getRegionManager(Bukkit.getWorld("bookmaker")).getApplicableRegions(e.player.location)
                for (region in regions) {
                    for (gameID in gameIDs) {
                        if ("mb_" + gameID == region.id) {
                            if (pl!!.gameManager.loadedGames[gameID]!!.status == GameStatus.FIGHT) {
                                if (pl!!.gameManager.loadedGames[gameID]!!.players.keys.contains(e.player.uniqueId)) {
                                    pl!!.gameManager.endGame(gameID, e.player.uniqueId)
                                }
                            } else { }
                        } else { }
                    }
                }
            } else { }
        } else { }
    }

        //以下不正対策

    @EventHandler
    fun onCommandPreProcess(e: PlayerCommandPreprocessEvent) {
        if (!e.player.isOp) {
            for (game in pl!!.gameManager.runningGames.values) {
                if (game.status == GameStatus.FIGHT) {
                    for (fighter in game.players.keys) {
                        if (fighter == e.player.uniqueId) {
                            e.isCancelled = true
                            e.player.sendMessage(pl!!.prefix + "試合中はコマンドは使用できません。")
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onItemConsume(e: PlayerItemConsumeEvent) {
        if (e.item == Material.POTION || e.item == Material.SPLASH_POTION || e.item == Material.LINGERING_POTION) {
            for (game in pl!!.gameManager.runningGames.values) {
                if (game.status == GameStatus.FIGHT) {
                    for (fighter in game.players.keys) {
                        if (fighter == e.player.uniqueId) {
                            e.isCancelled = true
                            e.player.sendMessage(pl!!.prefix + "試合中はポーションは使用できません。")
                        }
                    }
                }
            }
        }
    }
}

