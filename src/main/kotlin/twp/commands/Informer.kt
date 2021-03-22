package twp.commands

import twp.Main

class Informer internal constructor() : Command() {
    override fun run(id: String?, vararg args: String) {
        val ac = Main.db!!.findAccount(args[0])
        if (ac == null) {
            result = Result.notFound
            return
        }
        result = if (args.size == 1) {
            setArg(ac.basicStats())
            Result.info
        } else {
            setArg(ac.stats())
            Result.stats
        }
    }

    companion object {
        var general = Informer()
    }

    init {
        name = "info"
        argStruct = "<id> [stats]"
        description = "Shows information about player."
    }
}