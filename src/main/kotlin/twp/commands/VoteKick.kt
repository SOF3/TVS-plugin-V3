package twp.commands

import twp.database.enums.Perm
import twp.Main
import twp.democracy.Voting

class VoteKick : Command() {
    var main: Voting = object : Voting(this, "main", 2, 5) {
        init {
            protection = Perm.antiGrief
            increase = java.twp.database.enums.Stat.mkgfVotes
        }
    }

    override fun run(id: String, vararg args: String) {
        val pd = Main.db!!.online[id]
        if (cannotInteract(id)) {
            return
        }
        val account = Main.db!!.findAccount(args[0])
        if (account == null) {
            playerNotFound()
            return
        }
        if (!account.markable()) {
            result = main.java.twp.commands.Command.Result.wrongAccess
            return
        }
        if (account.id == pd!!.id) {
            result = main.java.twp.commands.Command.Result.cannotApplyToSelf
            return
        }
        val existingSession =
            Voting.processor.query(Voting.Processor.Query { s: Voting.Session -> s.voting === main && s.args[1] == account.id })
        if (existingSession != -1) {
            Voter.Companion.game.run(id, if (args.size == 1) "y" else "n", "" + existingSession)
            return
        }
        result = main.java.twp.commands.Command.Result.redundant
        if (args.size == 1) {
            if (account.isGriefer) {
                return
            }
            result = main.pushSession(
                pd,
                Voting.VoteRunner { s: Voting.Session? -> RankSetter.terminal.run("", args[0], "griefer") },
                account.name,
                account.id,
                "[red]griefer[]"
            )
        } else if (!wrongOption(1, args, "unmark")) {
            if (!account.isGriefer) {
                return
            }
            result = main.pushSession(
                pd,
                Voting.VoteRunner { s: Voting.Session? -> RankSetter.terminal.run("", args[0], "newcomer") },
                account.name,
                account.id,
                "[green]newcomer[]"
            )
        }
    }

    companion object {
        var game = VoteKick()
    }

    init {
        name = "votekick"
        argStruct = "<id/name> [unmark]"
        description = "Marks player a griefer witch means he/she can only spectate."
    }
}