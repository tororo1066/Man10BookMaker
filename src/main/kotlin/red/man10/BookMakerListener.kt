package red.man10

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.World
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.*
import org.bukkit.block.Sign
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
        lateinit var pl : BookMakerPlugin
    }

    fun returnListener(plugin: BookMakerPlugin): BookMakerListener {
        pl = plugin
        return BookMakerListener()
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        for (game in pl.gameManager.runningGames.values) {
            if (game.status == GameStatus.FIGHT) {
                if (game.players.keys.contains(e.whoClicked.uniqueId)){
                    e.isCancelled = true
                    e.whoClicked.sendMessage(pl.prefix + "試合中はインベントリは操作できません。")
                    return
                }
            }
        }
        when (e.view.title) {
            "§0§l[§7§lm§8§lBookMaker§0§l] §r§l開催中のゲーム" -> {
                e.isCancelled = true

                val clickItem = e.currentItem?:return
                val clickMeta = clickItem.itemMeta?:return
                val clickLore = clickMeta.lore?:return
                val player = e.whoClicked as Player

                val id = clickLore.last().substring(6)
                when (pl.gameManager.runningGames[id]!!.status) {
                    GameStatus.JOIN -> {
                        if (player.hasPermission("mb.join")) {
                            pl.gameManager.addCandidate(player, id)
                            player.closeInventory()
                        } else {
                            player.sendMessage(pl.prefix + "権限がありません。")
                        }
                    }
                    GameStatus.BET -> {
                        if (player.hasPermission("mb.bet")) {
                            pl.gui.openPlayerSelectMenu(player, id)
                        } else {
                            player.sendMessage(pl.prefix + "権限がありません。")
                        }
                    }
                    GameStatus.FIGHT -> {
                        if (player.hasPermission("mb.view")) {
                            if (clickLore.size == 2) {
                                pl.gameManager.viewTeleport(id, player)
                            }
                        } else {
                            player.sendMessage(pl.prefix + "権限がありません。")
                        }
                    }
                    GameStatus.OFF -> {
                        player.sendMessage("§4§lERROR: §f§lOPに報告してください")
                    }
                }
            }

            "§0§l[§7§lm§8§lBookMaker§0§l] §r§l勝者を予想してベット!" -> {
                e.isCancelled = true
                if (e.whoClicked.hasPermission("mb.bet")) {
                    if (e.currentItem != null) {
                        val uuid = e.currentItem!!.itemMeta!!.lore!![1]!!.substring(6)
                        val gameId = e.currentItem!!.itemMeta!!.lore!![2]!!.substring(8)
                        pl.gui.openBetMenu(e.whoClicked as Player, gameId, UUID.fromString(uuid))
                    }
                } else {
                    e.whoClicked.sendMessage("権限がありません。")
                }
            }
            "§0§l[§7§lm§8§lBookMaker§0§l] §r§l正解を予想してベット!" -> {
                e.isCancelled = true
                if (e.whoClicked.hasPermission("mb.bet")) {
                    if (e.currentItem != null) {
                        val uuid = e.currentItem!!.itemMeta!!.lore!![1]!!.substring(6)
                        val gameId = e.currentItem!!.itemMeta!!.lore!![2]!!.substring(8)
                        pl.gui.openBetMenu(e.whoClicked as Player, gameId, UUID.fromString(uuid))
                    }
                } else {
                    e.whoClicked.sendMessage("権限がありません。")
                }
            }

            "§0§l[§7§lm§8§lBookMaker§0§l] §r§lベット金額を入力" -> {
                e.isCancelled = true
                if (pl.gui.numberPlaces.contains(e.slot)) {
                    val number = e.currentItem!!.itemMeta!!.displayName.substring(2)
                    if (pl.gui.currentNumbers[e.whoClicked.name]!!.length <= 8) {
                        pl.gui.currentNumbers[e.whoClicked.name] = (pl.gui.currentNumbers[e.whoClicked.name]!! + number)
                        pl.gui.setBetNumber(e.whoClicked as Player)
                    }
                } else {
                    when (e.currentItem?.itemMeta?.displayName) {
                        "§4§lリセット" -> {
                            pl.gui.resetBetNumber(e.whoClicked as Player)
                        }
                        "§a§l決定" -> {
                            val gameId = e.currentItem!!.itemMeta!!.lore!![0]!!.substring(6)
                            val playerUUID = UUID.fromString(e.currentItem!!.itemMeta!!.lore!![1]!!.substring(5))

                            if (pl.gui.currentNumbers[e.whoClicked.name]!!.isEmpty()){
                                e.whoClicked.sendMessage(pl.prefix + "ベット金額が入力できていません！")
                            }
                            pl.gameManager.bet(e.whoClicked as Player, playerUUID, gameId, pl.gui.currentNumbers[e.whoClicked.name]!!.toDouble())
                            e.whoClicked.closeInventory()
                        }
                        "§4§lキャンセル" -> {
                            e.whoClicked.closeInventory()
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onInventoryClose(e: InventoryCloseEvent) {
        if (e.view.title == "§0§l[§7§lm§8§lBookMaker§0§l] §r§lベット金額を入力") {
            pl.gui.currentNumbers.remove(e.player.name)
        }
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        val uuid = e.player.uniqueId
        for (i in pl.gameManager.runningGames) {
            when (i.value.status) {
                GameStatus.JOIN -> {
                    if (i.value.candidates.contains(uuid)){
                        i.value.candidates.remove(uuid)
                    }
                }
                GameStatus.BET -> {
                    if (i.value.players.keys.contains(uuid)){
                        Bukkit.broadcastMessage(pl.prefix + "§lプレイヤーが抜けたため、§6§l「" + i.value.gameName + "」§f§lが停止されました。")
                        pl.gameManager.stopGame(i.key)
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
                    if (i.value.players.keys.contains(uuid)){
                        Bukkit.broadcastMessage(pl.prefix + "§lプレイヤーが抜けたため、§6§l「" + i.value.gameName + "」§f§lが停止されました。")
                        pl.gameManager.stopGame(i.key)
                    }
                }
                else -> return
            }
        }
    }

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        if (pl.freezedPlayer.contains(e.player.uniqueId)) {
            e.player.teleport(e.from)
        }
        if (!hasBMWorld)return
        if (e.player.world.name != "bookmaker") return
        val regions = pl.worldguard.get(BukkitAdapter.adapt(Bukkit.getWorld("bookmaker")))
            ?.getApplicableRegions(BlockVector3.at(e.player.location.x, e.player.location.y, e.player.location.z))?:return
        for (region in regions) {
            for (gameID in gameIDs) {
                if ("mb_$gameID" != region.id) {
                    if (pl.gameManager.loadedGames[gameID]!!.status == GameStatus.FIGHT) {
                        if (pl.gameManager.loadedGames[gameID]!!.players.keys.contains(e.player.uniqueId)) {
                            pl.gameManager.endGame(gameID, e.player.uniqueId)
                        }
                    }
                }
            }
        }
    }

        //以下不正対策

    @EventHandler
    fun onCommandPreProcess(e: PlayerCommandPreprocessEvent) {
        if (e.player.isOp)return
        for (game in pl.gameManager.runningGames.values) {
            if (game.status == GameStatus.FIGHT) {
                if (game.players.keys.contains(e.player.uniqueId)){
                    e.isCancelled = true
                    e.player.sendMessage(pl.prefix + "試合中はコマンドは使用できません。")
                }
            }
        }
    }

    @EventHandler
    fun onItemConsume(e: PlayerItemConsumeEvent) {
        if (e.item.type == Material.POTION || e.item.type == Material.SPLASH_POTION || e.item.type == Material.LINGERING_POTION) {
            for (game in pl.gameManager.runningGames.values) {
                if (game.status == GameStatus.FIGHT) {
                    if (game.players.keys.contains(e.player.uniqueId)){
                        e.isCancelled = true
                        e.player.sendMessage(pl.prefix + "試合中はポーションは使用できません。")
                    }
                }
            }
        }
    }

    @EventHandler
    fun onSignChange(e: SignChangeEvent) {
        if (e.player.hasPermission("mb.op")) {
            if (e.getLine(0) == "bookmaker") {
                if (pl.gameManager.loadedGames.keys.contains(e.getLine(1)!!)) {
                    e.setLine(3, e.getLine(1))
                    e.setLine(0, "§l[§8§lmBookMaker§0§l]")
                    e.setLine(1, "§l" + pl.gameManager.loadedGames[e.getLine(1)!!]!!.gameName)
                    when (e.getLine(2)) {
                        "open" -> {
                            e.setLine(2, "§l[ゲーム起動]")
                        }
                        "join" -> {
                            e.setLine(2, "§l[試合に参加登録]")
                        }
                        "bet" -> {
                            e.setLine(2, "§l[ベット]")
                        }
                        "view" -> {
                            e.setLine(2, "§l[観戦]")
                        }
                        else -> {
                            e.isCancelled = true
                            e.player.sendMessage(pl.prefix + "§l3行目はjoin/bet/view/startのいずれかにしてください。")
                        }
                    }
                } else {
                    e.isCancelled = true
                    e.player.sendMessage(pl.prefix + "§l指定されたゲーム名のゲームは存在しません。")
                }
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        //print(e.clickedBlock)
        if (e.clickedBlock != null) {
            if (e.clickedBlock!!.type == Material.OAK_SIGN) {
                val sign = e.clickedBlock!!.state as Sign
                //print(sign.getLine(0))
                if (sign.getLine(0).equals("§l[§8§lmBookMaker§0§l]", true)) {
                    //print(sign.getLine(2))
                    val gameId = sign.getLine(3)
                    if (!pl.isLocked) {
                        when (sign.getLine(2)) {
                            "§l[ゲーム起動]" -> {
                                pl.gameManager.openNewGame(gameId, e.player)
                            }
                            "§l[試合に参加登録]" -> {
                                pl.gameManager.addCandidate(e.player, gameId)
                            }
                            "§l[ベット]" -> {
                                if (e.player.hasPermission("mb.bet")) {
                                    pl.gui.openPlayerSelectMenu(e.player, gameId)
                                }
                            }
                            "§l[観戦]" -> {
                                pl.gameManager.viewTeleport(gameId, e.player)
                            }
                        }
                    } else {
                        e.player.sendMessage(pl.prefix + "§l現在BookmakerはOFFになってます。")
                    }
                }
            }
        }
    }
}

