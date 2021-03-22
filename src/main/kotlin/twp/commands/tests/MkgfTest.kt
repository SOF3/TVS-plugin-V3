package twp.commands.tests

internal object MkgfTest : Test() {
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        Main.db!!.online[""] = Main.db!!.handler.loadData(DBPlayer())
        Main.db!!.handler.loadData(object : DBPlayer() {
            init {
                uuid = "asd"
            }
        })
        Main.db!!.handler.loadData(object : DBPlayer() {
            init {
                uuid = "a"
            }
        })
        Main.db!!.online["b"] = Main.db!!.handler.loadData(object : DBPlayer() {
            init {
                uuid = "b"
            }
        })
        VoteKick.game.run("", "1", "asdas")
        VoteKick.game.assertResult(Command.Result.wrongOption)
        VoteKick.game.run("", "100")
        VoteKick.game.assertResult(Command.Result.playerNotFound)
        VoteKick.game.run("", "1", "unmark")
        VoteKick.game.assertResult(Command.Result.redundant)
        VoteKick.game.run("", "1")
        VoteKick.game.assertResult(Command.Result.voteStartSuccess)
        VoteKick.game.run("", "1")
        VoteKick.game.assertResult(Command.Result.alreadyVoted)
        VoteKick.game.run("", "0")
        VoteKick.game.assertResult(Command.Result.cannotApplyToSelf)
        VoteKick.game.run("", "2")
        VoteKick.game.assertResult(Command.Result.alreadyVoting)
        VoteKick.game.run("b", "1")
        VoteKick.game.assertResult(Command.Result.voteSuccess)
        Assert.assertEquals("griefer", Main.db!!.handler[1, "rank"])
        VoteKick.game.run("", "1", "unmark")
        VoteKick.game.assertResult(Command.Result.voteStartSuccess)
        Voter.game.run("b", "y")
        Voter.game.assertResult(Command.Result.voteSuccess)
        Assert.assertEquals("newcomer", Main.db!!.handler[1, "rank"])
    }
}