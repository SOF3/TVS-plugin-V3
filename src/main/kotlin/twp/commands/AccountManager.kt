package twp.commands

import twp.tools.Security.hash2
import twp.tools.Security.hash
import twp.Main
import java.util.HashMap

// AccountManager lets players to manage their account
// create new, abandon it and protect it.
class AccountManager : Command() {
    override fun run(id: String, vararg args: String) {
        val pd = Main.db!!.online[id]

        // griefers cannot manipulate with their account, griefer rank would be then worthless
        if (pd!!.rank === Main.ranks!!.griefer) {
            setArg(Main.ranks!!.griefer.suffix)
            result = Result.noPerm
            return
        }
        when (args[0]) {
            "unprotect" -> {
                if (checkArgCount(args.size, 2)) {
                    return
                }
                val account1 = pd!!.account
                if (account1.isProtected) {
                    result = if (account1.password == hash2(args[1])) {
                        Main.db!!.handler.unset(pd.id, "password")
                        Result.unprotectSuccess
                    } else {
                        Result.incorrectPassword
                    }
                    return
                }
                if (checkArgCount(args.size, 2)) {
                    return
                }
                if (pd.account.isProtected) {
                    result = Result.alreadyProtected
                    return
                }
                val pa = confirms[pd.id]
                if (pa != null) {
                    if (pa == args[1]) {
                        Main.db!!.handler[pd.id, "password"] = hash2(args[1])
                        result = Result.confirmSuccess
                    } else {
                        result = Result.confirmFail
                    }
                    confirms.remove(pd.id)
                    return
                }
                confirms[pd.id] = args[1]
                result = Result.confirm
                return
            }
            "protect" -> {
                if (checkArgCount(args.size, 2)) {
                    return
                }
                if (pd!!.account.isProtected) {
                    result = Result.alreadyProtected
                    return
                }
                val pa = confirms[pd.id]
                if (pa != null) {
                    if (pa == args[1]) {
                        Main.db!!.handler[pd.id, "password"] = hash2(args[1])
                        result = Result.confirmSuccess
                    } else {
                        result = Result.confirmFail
                    }
                    confirms.remove(pd.id)
                    return
                }
                confirms[pd.id] = args[1]
                result = Result.confirm
                return
            }
            "abandon", "new" -> {
                // this is little protection for dummies, we dont need accidents, nor we need dead data in database
                if (args[0] != "abandon" && !pd!!.paralyzed && !pd.account.isProtected) {
                    result = Result.notExplicit
                    return
                }
                Main.db!!.handler.makeNewAccount(pd!!.player.uuid, pd.player.ip)
                Main.db!!.disconnectAccount(pd)
                return
            }
            else -> {
                if (isNotInteger(args, 0)) {
                    return
                }
                val id1 = args[0].toLong()
                val account = Main.db!!.handler.getAccount(id1)
                if (account == null) {
                    result = Result.notFound
                    return
                }
                val pass = if (args.size == 2) args[1] else ""
                val password = account.password
                result = if (pd!!.player.ip != account.ip && pd.player.uuid != account.uuid && password == null) {
                    Result.invalidRequest
                } else if (password == null || password == hash(pass) || password == hash2(
                        pass
                    )
                ) { // due to backward compatibility there are two hashes
                    Main.db!!.disconnectAccount(pd)
                    Main.db!!.handler.setIp(id1, pd.player.ip)
                    Main.db!!.handler.setUuid(id1, pd.player.uuid)
                    Result.loginSuccess
                } else {
                    Result.incorrectPassword
                }
            }
        }
    }

    companion object {
        var confirms = HashMap<Long, String>()
        var game = AccountManager()
    }

    init {
        name = "account"
        argStruct = "<abandon/new/id> [password]"
        description = "For account management, you can create new account of protect current one with password"
    }
}