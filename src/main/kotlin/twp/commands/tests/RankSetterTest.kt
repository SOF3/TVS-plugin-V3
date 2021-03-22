package twp.commands.tests

internal object RankSetterTest : Test() {
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        Main.db!!.handler.loadData(object : DBPlayer() {})
        Main.db!!.handler.loadData(object : DBPlayer() {
            init {
                uuid = "sdad"
                ip = "asdasd"
            }
        })
        RankSetter.terminal.run("", "asdk", "admin")
        RankSetter.terminal.assertResult(Command.Result.playerNotFound)
        RankSetter.terminal.run("", "0", "admin")
        RankSetter.terminal.assertResult(Command.Result.success)
        RankSetter.terminal.run("", "0", "asdasd")
        RankSetter.terminal.assertResult(Command.Result.wrongRank)
        RankSetter.game.run("", "0", "newcomer")
        RankSetter.game.assertResult(Command.Result.noPerm)
        val data = Main.db!!.handler.loadData(object : DBPlayer() {})
        Main.db!!.online[data.player.uuid] = data
        RankSetter.game.run("", "0", "newcomer")
        RankSetter.game.assertResult(Command.Result.wrongAccess)
        RankSetter.game.run("", "1", "verified")
        RankSetter.game.assertResult(Command.Result.success)
    }
}