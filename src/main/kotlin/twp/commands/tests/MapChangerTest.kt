package twp.commands.tests

object MapChangerTest : Test() {
    // TODO test rate and change in game
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        Main.db!!.online[""] = Main.db!!.handler.loadData(DBPlayer())
        MapChanger.terminal.run("", "list")
        MapChanger.terminal.assertResult(Command.Result.none)
        MapChanger.terminal.run("", "list", "4")
        MapChanger.terminal.assertResult(Command.Result.none)
        MapChanger.terminal.run("", "list", "hh")
        MapChanger.terminal.assertResult(Command.Result.notInteger)
        MapChanger.terminal.run("", "flit")
        MapChanger.terminal.assertResult(Command.Result.wrongOption)
    }
}