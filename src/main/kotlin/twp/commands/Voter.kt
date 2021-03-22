package twp.commands

import twp.Main
import twp.democracy.Voting

class Voter : Command() {
    override fun run(id: String, vararg args: String) {
        if (cannotInteract(id)) {
            return
        }
        var idx = 0
        if (args.size > 1) {
            if (isNotInteger(args, 1)) {
                return
            }
            idx = args[1].toInt()
        }
        result = Voting.processor.addVote(idx, Main.db!!.online[id]!!.id, args[0])
    }

    companion object {
        var game = Voter()
    }

    init {
        name = "v"
        argStruct = "<y/n> <session>"
        description = "Allows you to participate in vote sessions."
    }
}