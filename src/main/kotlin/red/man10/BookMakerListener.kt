package red.man10

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
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
        lateinit var pl : BookMakerPlugin
    }

    fun returnListener(plugin: BookMakerPlugin): BookMakerListener {
        pl = plugin
        return BookMakerListener()
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        for (game in pl.gameManager.runningGames.values) {
            if (game.status == GameStatus.FIGHT || game.status == GameStatus.BET) {
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
                            if (pl.gameManager.runningGames[id]!!.candidates.contains(player.uniqueId)){
                                pl.gameManager.removeCandidate(player,id)
                            }else{
                                pl.gameManager.addCandidate(player, id)
                            }
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
                        pl.worldMsg(pl.prefix + "§lプレイヤーが抜けたため、§6§l「" + i.value.gameName + "」§f§lが停止されました。")
                        pl.gameManager.stopGame(i.key)
                    }
                }
                GameStatus.FIGHT -> {
                    if (i.value.players.keys.contains(uuid)){
                        pl.worldMsg(pl.prefix + "§lプレイヤーが抜けたため、§6§l「" + i.value.gameName + "」§f§lが停止されました。")
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
            e.isCancelled = true
            return
        }
        if (!hasBMWorld)return
        if (e.player.world.name != "bookmaker") return
        val regions = pl.worldguard.get(BukkitAdapter.adapt(Bukkit.getWorld("bookmaker")))
            ?.getApplicableRegions(BlockVector3.at(e.player.location.x, e.player.location.y, e.player.location.z))?:return

        for (region in regions.regions) {
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

    @EventHandler
    fun onWorldChange(e : PlayerChangedWorldEvent){
        val data = pl.gameManager.runningGames.entries.find { it.value.players.containsKey(e.player.uniqueId) } ?: return
        if (e.player.world.name != "bookmaker"){
            pl.worldMsg(pl.prefix + "§lプレイヤーがワールドから抜けたため、§6§l「" + data.value.gameName + "」§f§lが停止されました。")
            pl.gameManager.stopGame(data.key)
        }

    }

        //以下不正対策

    @EventHandler
    fun onCommandPreProcess(e: PlayerCommandPreprocessEvent) {
        if (e.player.isOp)return
        for (game in pl.gameManager.runningGames.values) {
            if (game.status == GameStatus.FIGHT || game.status == GameStatus.BET) {
                if (game.players.keys.contains(e.player.uniqueId)){
                    e.isCancelled = true
                    e.player.sendMessage(pl.prefix + "試合中はコマンドは使用できません。")
                }
            }
        }
    }

    @EventHandler
    fun onDamage(e: EntityDamageEvent) {
        if (e.entity !is Player)return
        for (game in pl.gameManager.runningGames.values) {
            if (game.status == GameStatus.FIGHT || game.status == GameStatus.BET) {
                if (game.players.keys.contains(e.entity.uniqueId)){
                    e.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onItemConsume(e: PlayerItemConsumeEvent) {
        if (e.item.type == Material.POTION || e.item.type == Material.SPLASH_POTION || e.item.type == Material.LINGERING_POTION) {
            for (game in pl.gameManager.runningGames.values) {
                if (game.status == GameStatus.FIGHT || game.status == GameStatus.BET) {
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
                if (e.getLine(1) == "return"){
                    e.setLine(0, "[mBookMaker]")
                    e.setLine(2,"[ロビーに戻る]")
                    return
                }
                if (pl.gameManager.loadedGames.keys.contains(e.getLine(1)!!)) {
                    e.setLine(3, e.getLine(1))
                    e.setLine(0, "[mBookMaker]")
                    e.setLine(1, "§l" + pl.gameManager.loadedGames[e.getLine(1)!!]!!.gameName)
                    when (e.getLine(2)) {
                        "open" -> {
                            e.setLine(2, "[ゲーム起動]")
                        }
                        "join" -> {
                            e.setLine(2, "[試合に参加登録]")
                        }
                        "leave"->{
                            e.setLine(2,"[参加登録解除]")
                        }
                        "bet" -> {
                            e.setLine(2, "[ベット]")
                        }
                        "view" -> {
                            e.setLine(2, "[観戦]")
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
            if (e.clickedBlock!!.state is Sign) {
                val sign = e.clickedBlock!!.state as Sign
                //print(sign.getLine(0))
                if (sign.getLine(0).equals("[mBookMaker]", true)) {
                    //print(sign.getLine(2))
                    val gameId = sign.getLine(3)
                    if (!pl.isLocked) {
                        when (sign.getLine(2)) {
                            "[ロビーに戻る]"->{
                                e.player.teleport(pl.lobbyLocation?:return)
                                e.player.sendMessage(pl.prefix + "§bロビーに転送しました")
                            }
                            "[ゲーム起動]" -> {
                                pl.gameManager.openNewGame(gameId, e.player)
                            }
                            "[試合に参加登録]" -> {
                                pl.gameManager.addCandidate(e.player, gameId)
                            }
                            "[参加登録解除]"->{
                                pl.gameManager.removeCandidate(e.player,gameId)
                            }
                            "[ベット]" -> {
                                if (e.player.hasPermission("mb.bet")) {
                                    pl.gui.openPlayerSelectMenu(e.player, gameId)
                                }
                            }
                            "[観戦]" -> {
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

    @EventHandler
    fun onDrop(e : PlayerDropItemEvent){
        if (e.player.world.name == "bookmaker"){
            e.player.sendMessage(pl.prefix + "bookmakerワールドではアイテムを捨てられません！")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onFoodLevelChange(e : FoodLevelChangeEvent){
        for (game in pl.gameManager.runningGames.values) {
            if (game.status == GameStatus.FIGHT || game.status == GameStatus.BET) {
                if (game.players.keys.contains(e.entity.uniqueId)){
                    e.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onChat(e : AsyncPlayerChatEvent){
        if (!pl.createGameManager.containsKey(e.player.uniqueId))return
        val p = e.player
        e.isCancelled = true
        if (e.message == "cancel"){
            pl.createGameManager.remove(e.player.uniqueId)
            pl.createGameManagerData.remove(e.player.uniqueId)
            p.sendMessage(pl.prefix + "キャンセルしました。")
            return
        }
        val number = pl.createGameManager[e.player.uniqueId]!!
        val data = pl.createGameManagerData[e.player.uniqueId]!!.second
        when(number){
            0->{
                data.gameName = e.message
                p.sendMessage(pl.prefix + "ゲーム名を「§a${e.message}§r」にしました。")
                p.sendMessage(pl.prefix + "次に参加人数を入力してください。")
            }

            1->{
                if (e.message.toIntOrNull() == null){
                    p.sendMessage(pl.prefix + "数字で入力してください！")
                    return
                }
                data.playerNumber = e.message.toInt()
                p.sendMessage(pl.prefix + "参加人数を「§a${e.message.toInt()}人§r」にしました。")
                p.sendMessage(pl.prefix + "次にアイテム名を入力してください。")
            }

            2->{
                val material = Material.getMaterial(e.message)
                if (material == null){
                    p.sendMessage(pl.prefix + "そのアイテム名は存在しません！")
                    return
                }
                data.item = material
                p.sendMessage(pl.prefix + "アイテムを「§a${e.message}§r」にしました。")
                p.sendMessage(pl.prefix + "次にbetが選手に行く倍率を入力してください。")
            }

            3->{
                if (e.message.toDoubleOrNull() == null){
                    p.sendMessage(pl.prefix + "実数で入力してください！")
                    return
                }
                data.prize = e.message.toDouble()
                p.sendMessage(pl.prefix + "betが選手に行く倍率を「§a${e.message.toDouble()}§r」にしました。")
                p.sendMessage(pl.prefix + "次に税の倍率を入力してください。")
            }

            4->{
                if (e.message.toDoubleOrNull() == null){
                    p.sendMessage(pl.prefix + "実数で入力してください！")
                    return
                }
                data.tax = e.message.toDouble()
                p.sendMessage(pl.prefix + "税の倍率を「§a${e.message.toDouble()}§r」にしました。")
                p.sendMessage(pl.prefix + "次に最初に1人1人の選手に自動で賭けられる金額を入力してください。")
            }

            5->{
                if (e.message.toIntOrNull() == null){
                    p.sendMessage(pl.prefix + "数字で入力してください！")
                    return
                }
                data.virtualBet = e.message.toInt()
                p.sendMessage(pl.prefix + "最初に1人1人の選手に自動で賭けられる金額を「§a${e.message.toInt()}円§r」にしました。")
                p.sendMessage(pl.prefix + "次に募集中の時間を入力してください。")
            }

            6->{
                if (e.message.toIntOrNull() == null){
                    p.sendMessage(pl.prefix + "数字で入力してください！")
                    return
                }
                data.duration.add(e.message.toInt())
                p.sendMessage(pl.prefix + "募集中の時間を「§a${e.message.toInt()}秒§r」にしました。")
                p.sendMessage(pl.prefix + "次にbetの時間を入力してください。")
            }

            7->{
                if (e.message.toIntOrNull() == null){
                    p.sendMessage(pl.prefix + "数字で入力してください！")
                    return
                }
                data.duration.add(e.message.toInt())
                p.sendMessage(pl.prefix + "betの時間を「§a${e.message.toInt()}秒§r」にしました。")
                p.sendMessage(pl.prefix + "次に試合の時間を入力してください。")
            }

            8->{
                if (e.message.toIntOrNull() == null){
                    p.sendMessage(pl.prefix + "数字で入力してください！")
                    return
                }
                data.duration.add(e.message.toInt())
                p.sendMessage(pl.prefix + "試合の時間を「§a${e.message.toInt()}秒§r」にしました。")
                p.sendMessage(pl.prefix + "次に参加費を入力してください。")
            }

            9->{
                if (e.message.toDoubleOrNull() == null){
                    p.sendMessage(pl.prefix + "実数で入力してください！")
                    return
                }
                data.joinFee = e.message.toDouble()
                p.sendMessage(pl.prefix + "参加費を「§a${e.message.toDouble()}円§r」にしました。")
                p.sendMessage(pl.prefix + "次にゲーム名(内部名)を入力してください。")
            }

            10->{
                if (e.message.contains(" ")){
                    p.sendMessage(pl.prefix + "空白を含めないでください！")
                    return
                }
                pl.createGameManagerData[e.player.uniqueId] = Pair(e.message,data)
                p.sendMessage(pl.prefix + "ゲーム名(内部名)を「§a${e.message}§r」にしました。")
                p.sendMessage(pl.prefix + "最後にゲーム開始時のコマンドを/無しで入力してください。(「no」でスルー出来ます。)")
            }

            11->{
                if (e.message != "no"){
                    data.commands.add(e.message)
                    p.sendMessage(pl.prefix + "コマンド「§a${e.message}§r」を追加しました。")
                    p.sendMessage(pl.prefix + "最後にゲーム開始時のコマンドを/無しで入力してください。(「no」でスルー出来ます。)")
                    return
                }

                p.sendMessage(pl.prefix + "確認")
                p.sendMessage(" ")
                p.sendMessage(pl.prefix + "ゲーム名：${data.gameName}")
                p.sendMessage(pl.prefix + "参加人数：${data.playerNumber}人")
                p.sendMessage(pl.prefix + "アイテム：${data.item.name}")
                p.sendMessage(pl.prefix + "betが選手に行く倍率：${data.prize}倍")
                p.sendMessage(pl.prefix + "税の倍率：${data.tax}倍")
                p.sendMessage(pl.prefix + "最初に1人1人の選手に自動で賭けられる金額：${data.virtualBet}円")
                p.sendMessage(pl.prefix + "募集中の時間：${data.duration[0]}秒")
                p.sendMessage(pl.prefix + "betの時間：${data.duration[1]}秒")
                p.sendMessage(pl.prefix + "試合の時間：${data.duration[2]}秒")
                p.sendMessage(pl.prefix + "参加費：${data.joinFee}円")
                p.sendMessage(pl.prefix + "ゲーム名(内部名)：${pl.createGameManagerData[e.player.uniqueId]!!.first}")
                data.commands.forEach {
                    p.sendMessage(pl.prefix + "コマンド：${it}")
                }
                p.sendMessage(" ")
                p.sendMessage(pl.prefix + "確認ができたら「confirm」と入力してください。")
            }

            12->{
                if (e.message != "confirm"){
                    p.sendMessage(pl.prefix + "取り消す場合は「cancel」と入力してください。")
                    return
                }
                val key = pl.createGameManagerData[e.player.uniqueId]!!.first
                pl.config.set("$key.name",data.gameName)
                pl.config.set("$key.playerNumber",data.playerNumber)
                pl.config.set("$key.item",data.item.name)
                pl.config.set("$key.joinFee",data.joinFee)
                pl.config.set("$key.tax",data.tax)
                pl.config.set("$key.prize",data.prize)
                pl.config.set("$key.virtualBet",data.virtualBet)
                pl.config.set("$key.time1",data.duration[0])
                pl.config.set("$key.time2",data.duration[1])
                pl.config.set("$key.time3",data.duration[2])
                pl.config.set("$key.commands",data.commands)
                pl.saveConfig()
                pl.createGameManager.remove(p.uniqueId)
                pl.createGameManagerData.remove(p.uniqueId)
                p.sendMessage(pl.prefix + "ゲームのデータが作成されました！")
                return
            }
        }

        pl.createGameManager[e.player.uniqueId] = number+1
    }

}

