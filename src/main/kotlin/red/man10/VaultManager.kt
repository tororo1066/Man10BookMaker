package red.man10

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import java.util.*


/**
 * Created by sho on 2017/07/21.
 */
class VaultManager {
    private fun setupEconomy(): Boolean {
        Bukkit.getLogger().info("setupEconomy")
        if (Bukkit.getServer().pluginManager.getPlugin("Vault") == null) {
            Bukkit.getLogger().warning("Vault plugin is not installed")
            return false
        }
        val rsp = Bukkit.getServer().servicesManager.getRegistration(
            Economy::class.java
        )
        if (rsp == null) {
            Bukkit.getLogger().warning("Can't get vault service")
            return false
        }
        economy = rsp.provider
        Bukkit.getLogger().info("Economy setup")
        return economy != null
    }

    /////////////////////////////////////
    //      残高確認
    /////////////////////////////////////
    fun getBalance(uuid: UUID?): Double {
        return economy!!.getBalance(Bukkit.getOfflinePlayer(uuid!!).player)
    }

    /////////////////////////////////////
    //      残高確認
    /////////////////////////////////////
    fun showBalance(uuid: UUID?) {
        val p: OfflinePlayer? = Bukkit.getOfflinePlayer(uuid!!).player
        val money = getBalance(uuid)
        p!!.player!!.sendMessage(ChatColor.YELLOW.toString() + "あなたの所持金は" + money + "円です")
    }

    /////////////////////////////////////
    //      引き出し
    /////////////////////////////////////
    fun withdraw(uuid: UUID, money: Double): Boolean {
        val p = Bukkit.getOfflinePlayer(uuid)
        val resp = economy!!.withdrawPlayer(p, money)
        if (resp.transactionSuccess()) {
            if (p.isOnline) {
                p.player!!.sendMessage(ChatColor.YELLOW.toString() + "電子マネー" + money + "円支払いました")
            }
            return true
        }
        return false
    }

    /////////////////////////////////////
    //      お金を入れる
    /////////////////////////////////////
    fun deposit(uuid: UUID, money: Double): Boolean {
        val p = Bukkit.getOfflinePlayer(uuid)
        val resp = economy!!.depositPlayer(p, money)
        if (resp.transactionSuccess()) {
            if (p.isOnline) {
                p.player!!.sendMessage(ChatColor.YELLOW.toString() + "電子マネー" + money + "円受取りました")
            }
            return true
        }
        return false
    }

    /////////////////////////////////////
    //      引き出し
    /////////////////////////////////////
    fun silentWithdraw(uuid: UUID, money: Double): Boolean {
        val p = Bukkit.getOfflinePlayer(uuid)
        val resp = economy!!.withdrawPlayer(p, money)
        return resp.transactionSuccess()
    }

    /////////////////////////////////////
    //      お金を入れる
    /////////////////////////////////////
    fun silentDeposit(uuid: UUID, money: Double): Boolean {
        val p = Bukkit.getOfflinePlayer(uuid)
        val resp = economy!!.depositPlayer(p, money)
        return resp.transactionSuccess()
    }

    companion object {
        var economy: Economy? = null
    }

    init {
        setupEconomy()
    }
}