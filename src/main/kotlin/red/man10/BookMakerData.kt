package red.man10

class BookMakerData {

    companion object {
        var pl: BookMakerPlugin? = null
        var mysql: MySQLManager? = null
    }

    fun returnData(plugin: BookMakerPlugin) : BookMakerData{
        pl = plugin
        print("t------e----s---ttt")
        mysql = MySQLManager(pl!!, "Bookmaker")
        return BookMakerData()
    }

    fun test() {
        var sql = "select * from games"
        print(mysql!!.query(sql).toString())
        mysql!!.close()
    }


}