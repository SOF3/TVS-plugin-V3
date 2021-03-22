package twp.commands.tests

import twp.Main
import twp.commands.Command
import twp.commands.Tester
import twp.database.DBPlayer

object TesterTest : Test() {
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        Main.db!!.online[""] = Main.db!!.handler.loadData(DBPlayer())
        Tester.game.caller = Main.db!!.online[""]
        Tester.game.run("")
        Tester.game.assertResult(Command.Result.hint)
        Tester.game.run("")
        Tester.game.assertResult(Command.Result.hint)
        Tester.game.run("", "10")
        Tester.game.assertResult(Command.Result.invalidRequest)
        Tester.game.run("", "-1")
        Tester.game.assertResult(Command.Result.notInteger)
        Tester.game.run("", "1")
        Tester.game.assertResult(Command.Result.hint)
        Tester.game.run("", "1")
        Tester.game.assertResult(Command.Result.testFail)
        Tester.game.run("")
        Tester.game.assertResult(Command.Result.penalty)
        Tester.game.recent.clear()
        Tester.game.run("", "4")
        Tester.game.assertResult(Command.Result.wrongOption)
        Tester.game.run("")
        Tester.game.assertResult(Command.Result.hint)
        Tester.game.run("", "3")
        Tester.game.assertResult(Command.Result.hint)
        Tester.game.run("", "3")
        Tester.game.assertResult(Command.Result.success)
    }
}