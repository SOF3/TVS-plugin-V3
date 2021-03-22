package twp.commands

import twp.tools.Enums.contains
import twp.database.enums.RankType
import twp.Main

// Command used for editing database
open class DBSetter : Command() {
    var forbidden = arrayOf(
        "name",
        "uuid",
        "ip",
        "password",
        "_id"
    )

    override fun run(id: String?, vararg args: String) {
        if (!verifier.verify(id)) {
            result = Result.noPerm
            return
        }
        var forbid = false
        for (s in forbidden) {
            if (s == args[1]) {
                forbid = true
                break
            }
        }
        if (forbid && !freeAccess) { // removing some of properties can break staff
            result = Result.wrongAccess
            return
        }
        if (contains(RankType::class.java, args[1])) { // we already have command for this
            result = Result.wrongCommand
            return
        }
        if (isNotInteger(args, 0)) {
            return
        }
        val i = args[0].toLong()
        val doc = Main.db!!.handler.getAccount(i)
        if (doc == null) {
            result = Result.notFound
            return
        }
        val field = Main.db!!.handler[i, args[1]]
        if (field == null) {
            setArg(1, doc.fieldList())
            result = Result.wrongOption // ok
            return
        }
        if (args[2] == "null") {
            Main.db!!.handler.unset(i, args[1])
            result = Result.unsetSuccess
            return
        }
        if (field is String) {
            Main.db!!.handler[i, args[1]] = args[2]
        } else if (field is Long) {
            if (isNotInteger(args, 2)) {
                return
            }
            Main.db!!.handler[i, args[1]] = args[2].toLong()
        } else {
            result = Result.wrongAccess
            return
        }
        setArg(field.toString(), args[2])
    }

    companion object {
        var terminal = DBSetter()
        var game: DBSetter = object : DBSetter() {
            init {
                verifier = Verifier { id: String? -> isPlayerAdmin(main.java.twp.commands.id) }
            }
        }
    }

    init {
        name = "dbset"
        argStruct = "<id> <field> <value/null>"
        description = "Directly edits player accounts. For example bring down a stats of farmers."
    }
}