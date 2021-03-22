package twp.database.enums

// player permission enumeration
enum class Perm {
    normal(0), high(1), higher(2), highest(3), loadout(Stat.loadoutVotes), factory(Stat.factoryVotes), restart, change, gameOver, build, destruct, suicide, colorCombo, antiGrief(
        Stat.mkgfVotes
    ),
    skip, coreBuild(Stat.buildCoreVotes);

    @JvmField
    var value = -1
    var relation: Stat? = null

    constructor(relation: Stat) {
        this.relation = relation
        value = 0
    }

    constructor() {}
    constructor(value: Int) {
        this.value = value
    }
}