package twp.commands.tests

internal object SearcherTest : Test() {
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        Main.db!!.handler.loadData(object : DBPlayer() {
            init {
                uuid = "a"
                ip = "a"
                name = "ca"
            }
        })
        Main.db!!.handler.setRank(0, Main.ranks!!.admin, RankType.rank)
        Main.db!!.handler.loadData(object : DBPlayer() {
            init {
                uuid = "b"
                ip = "b"
                name = "cb"
            }
        })
        Main.db!!.handler.loadData(object : DBPlayer() {
            init {
                uuid = "c"
                ip = "c"
                name = "c"
            }
        })
        Main.db!!.handler.loadData(object : DBPlayer() {
            init {
                uuid = "d"
                ip = "d"
                name = "d"
            }
        })
        Searcher.terminal.run("", "c")
        Searcher.terminal.assertResult(Command.Result.success)
        Searcher.terminal.run("", "none")
        Searcher.terminal.assertResult(Command.Result.success)
        Searcher.terminal.run("", "none", "age")
        Searcher.terminal.assertResult(Command.Result.success)
        Searcher.terminal.run("", "none", "admin")
        Searcher.terminal.assertResult(Command.Result.success)
        Searcher.terminal.run("", "none", "age", "2=4")
        Searcher.terminal.assertResult(Command.Result.success)
        Searcher.terminal.run("", "none", "age", "-1=-3")
        Searcher.terminal.assertResult(Command.Result.success)
        Searcher.terminal.run("", "none", "age", "-1=-3", "inv")
        Searcher.terminal.assertResult(Command.Result.success)
        Searcher.terminal.run("", "none", "age", "v")
        Searcher.terminal.assertResult(Command.Result.invalidSlice)
        Searcher.terminal.run("", "none", "age", "10=10")
        Searcher.terminal.assertResult(Command.Result.emptySlice)
        Searcher.terminal.run("", "none", "age", "10/hm")
        Searcher.terminal.assertResult(Command.Result.invalidSlice)
        Searcher.terminal.run("", "none", "sjhsd")
        Searcher.terminal.assertResult(Command.Result.invalidSearch)
    }
}