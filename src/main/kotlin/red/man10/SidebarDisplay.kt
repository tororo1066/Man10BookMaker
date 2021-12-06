package red.man10

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.*

/**
 * Created by takatronix on 2017/03/02.
 */
class SidebarDisplay {

    private val scoreboard: Scoreboard

    /**
     * コンストラクタ
     */
    init {
        scoreboard = Bukkit.getScoreboardManager()?.newScoreboard!!
        val sidebar = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy")
        sidebar.displaySlot = DisplaySlot.SIDEBAR
    }

    /**
     * 指定されたプレイヤーを、このスコアボードの表示対象にする
     *
     * @param player プレイヤー
     */
    fun setShowPlayer(player: Player) {
        player.scoreboard = scoreboard
    }

    /**
     * 指定されたプレイヤーを、メインスコアボード表示に戻す
     *
     * @param player プレイヤー
     */
    fun setMainScoreboard(player: Player) {
        player.scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
    }

    /**
     * サイドバーのタイトルを設定する。
     *
     * @param title タイトル
     */
    fun setTitle(title: String) {
        val obj = scoreboard.getObjective(OBJECTIVE_NAME)
        if (obj != null) {
            obj.displayName = title
        }
    }

    /**
     * スコア項目を設定する。項目名は16文字以下にすること。
     *
     * @param name  項目名
     * @param point 項目のスコア
     */
    fun setScore(name: String, point: Int) {
        val obj = scoreboard.getObjective(OBJECTIVE_NAME)
        val score = obj!!.getScore(name)
        if (point == 0) {
            score.score = 1 // NOTE: set temporary.
        }
        score.score = point
    }

    /**
     * 項目にスコアを加算する。マイナスを指定すれば減算も可能。
     *
     * @param name   項目名
     * @param amount 加算する値
     */
    fun addScore(name: String, amount: Int) {
        val obj = scoreboard.getObjective(OBJECTIVE_NAME)
        val score = obj!!.getScore(name)
        val point = score.score
        score.score = point + amount
    }

    /**
     * スコアボードを削除する。
     */
    fun remove() {
        if (scoreboard.getObjective(DisplaySlot.SIDEBAR) != null) {
            scoreboard.getObjective(DisplaySlot.SIDEBAR)!!.unregister()
        }
        scoreboard.clearSlot(DisplaySlot.SIDEBAR)
    }

    /**
     * スコア項目を削除する
     *
     * @param name
     */
    fun removeScores(scoreboard: Scoreboard, name: String) {
        scoreboard.resetScores(name)
    }

    companion object {
        private const val OBJECTIVE_NAME = "MB"
    }

}