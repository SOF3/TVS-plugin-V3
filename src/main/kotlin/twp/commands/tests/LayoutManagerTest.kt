package twp.commands.tests

object LayoutManagerTest : Test() {
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        Main.db!!.online[""] = Main.db!!.handler.loadData(DBPlayer())
        LoadoutManager.game.caller = Main.db!!.handler.loadData(DBPlayer())
        LoadoutManager.game.run("", "get", "itm", "10")
        LoadoutManager.game.assertResult(Command.Result.invalidRequest)
        LoadoutManager.game.run("", "get", "copper", "h")
        LoadoutManager.game.assertResult(Command.Result.notInteger)
        Main.docks!!.ships.size = 3
        LoadoutManager.game.run("", "get", "copper", "10")
        LoadoutManager.game.assertResult(Command.Result.penalty)
        Main.docks!!.ships.size = 0
        LoadoutManager.game.run("", "", "copper", "10")
        LoadoutManager.game.assertResult(Command.Result.wrongOption)
        LoadoutManager.game.run("", "get", "spore-pod", "10")
        LoadoutManager.game.assertResult(Command.Result.redundant)
        Main.db!!.loadout[Items.coal] = 10000
        LoadoutManager.game.run("", "get", "coal", "10000")
        LoadoutManager.game.assertResult(Command.Result.voteStartSuccess)
        Assert.assertEquals(3, Main.docks!!.ships.size.toLong())
    }
}