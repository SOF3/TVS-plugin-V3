package twp.commands.tests

object InformerTest : Test() {
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        Main.db!!.handler.loadData(DBPlayer())
        Informer.general.run("", "0")
        Informer.general.assertResult(Command.Result.info)
        Informer.general.run("", "0", "stats")
        Informer.general.assertResult(Command.Result.stats)
        Informer.general.run("", "20", "stats")
        Informer.general.assertResult(Command.Result.notFound)
    }
}