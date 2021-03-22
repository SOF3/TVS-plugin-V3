package twp.database

import mindustry.gen.Player

// This is basically absolutely useless peace of shit that is only needed because
// player class cannot be constructed
class DBPlayer {
    @JvmField
    var name = "noname"
    @JvmField
    var uuid = ""
    var usid = ""
    @JvmField
    var ip = "127.0.0.1"
    var admin = false
    var id = 0
    @JvmField
    var p: Player? = null

    constructor() {}
    constructor(player: Player?) {
        if (player != null) {
            name = player.name
            uuid = player.uuid()
            usid = player.usid()
            ip = player.con.address
            id = player.id
            admin = player.admin
            p = player
        }
    }
}