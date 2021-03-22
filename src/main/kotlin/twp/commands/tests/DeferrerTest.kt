package twp.commands.tests

object DeferrerTest : Test() {
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        Deferrer.terminal.run("", "exit")
        Deferrer.terminal.assertResult(Command.Result.success)
        Deferrer.terminal.run("", "recover")
        Deferrer.terminal.assertResult(Command.Result.recoverSuccess)
    }
}