package twp.commands.tests

internal object SetterTest : Test() {
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        DBSetter.terminal.freeAccess = true
        Main.db!!.handler.loadData(object : DBPlayer() {
            init {
                uuid = "a"
                ip = "a"
                name = "ca"
            }
        })
        Main.db!!.handler.incOne(0, Stat.age)
        Main.db!!.handler.incOne(0, Stat.buildCoreVotes)
        DBSetter.terminal.run("", "sda", "", "")
        DBSetter.terminal.assertResult(Command.Result.notInteger)
        DBSetter.terminal.run("", "1", "", "")
        DBSetter.terminal.assertResult(Command.Result.notFound)
        DBSetter.terminal.run("", "0", "asdasd", "")
        DBSetter.terminal.assertResult(Command.Result.wrongOption)
        DBSetter.terminal.run("", "0", "settings", "")
        DBSetter.terminal.assertResult(Command.Result.wrongAccess)
        DBSetter.terminal.run("", "0", "age", "asda")
        DBSetter.terminal.assertResult(Command.Result.notInteger)
        DBSetter.terminal.run("", "0", "age", "100")
        DBSetter.terminal.assertResult(Command.Result.success)
        DBSetter.terminal.run("", "0", "name", "ono")
        DBSetter.terminal.assertResult(Command.Result.success)
        DBSetter.terminal.run("", "0", "buildCoreVotes", "null")
        DBSetter.terminal.assertResult(Command.Result.unsetSuccess)
        Main.db!!.online[""] = Main.db!!.handler.loadData(DBPlayer())
        DBSetter.game.run("", "0", "buildCoreVotes", "null")
        DBSetter.game.assertResult(Command.Result.noPerm)
        Main.db!!.handler.setRank(1, Main.ranks!!.admin, RankType.rank)
        Main.db!!.online[""] = Main.db!!.handler.loadData(DBPlayer())
        DBSetter.game.run("", "0", "name", "null")
        DBSetter.game.assertResult(Command.Result.wrongAccess)
    }
}