package twp.commands

import twp.Main
import twp.security.Action

open class Undoer : Command() {
    override fun run(id: String, vararg args: String) {
        val data = if (args.size == 2 || caller == null) Main.db!!.findAccount(args[1]) else caller.account
        if (data == null) {
            playerNotFound()
            return
        }
        if (!verifier.verify(id) && data.id != caller.id) {
            result = Result.noPerm
            return
        }
        if (isNotInteger(args, 0)) {
            return
        }
        Main.queue!!.post { Action.execute(data.id, args[0].toInt().toLong()) }
    }

    companion object {
        var game: Undoer = object : Undoer() {
            init {
                verifier = Verifier { id: String? -> isPlayerAdmin(main.java.twp.commands.id) }
            }
        }
    }

    init {
        name = "undo"
        argStruct = "<amount> [id/name]"
        description = "undo undoes the action of targeted player, if you do not provide player your actions are undone"
    }
}