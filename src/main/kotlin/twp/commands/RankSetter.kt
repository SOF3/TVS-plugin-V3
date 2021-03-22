package twp.commands

import twp.database.enums.RankType
import twp.Main

// Rank setter lets admins to change build-in ranks of players
// Its designed for use on multiple places (terminal, game, discord)
open class RankSetter : Command() {
    override fun run(id: String, vararg args: String) {
        // Verify - place dependent
        if (!verifier.verify(id)) {
            result = Result.noPerm // done
            return
        }

        // Resolve rank type, tis also checks if rank exists
        val rank = Main.ranks!!.buildIn[args[1]]
        if (rank == null) {
            setArg(Main.ranks!!.rankList(RankType.rank))
            result = Result.wrongRank // done
            return
        }

        // Search target
        val data = Main.db!!.findAccount(args[0])
        if (data == null) {
            playerNotFound()
            return
        }

        // admin rank can be set only through terminal
        if (!freeAccess && (rank.admin || data.admin())) {
            result = Result.wrongAccess // done
            return
        }

        // setting arguments to show change
        setArg(data.getRank(RankType.rank).suffix, rank.suffix)
        Main.db!!.handler.setRank(data.id, rank, RankType.rank, if (args.size > 2) args[2] else "reason not provided")

        // if player is online kick him. I do not want to deal with bag prone code to change his rank manually.
        val pd = Main.db!!.online[data.uuid]
        if (pd != null && !Main.testMode) {
            Main.queue!!.post { pd.kick("kick-rankChange", 0, rank.suffix) }
        }
    }

    companion object {
        var game: RankSetter = object : RankSetter() {
            init {
                verifier = Verifier { id: String? -> isPlayerAdmin(main.java.twp.commands.id) }
            }
        }
        var terminal = RankSetter()
    }

    init {
        name = "setrank"
        argStruct = "<id/name> <rank> [note...]"
        description = "Sets rank of players, name can be used if player is online."
    }
}