package twp.commands.tests

open class Test {
    companion object {
        fun init() {
            Vars.content = ContentLoader()
            Items().load()
            Main()
            Main.testMode = true
            Events.fire(EventType.ServerLoadEvent())
            Main.db!!.handler.drop()
            Main.db!!.maps.drop()
        }

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            init()
            val coll = Main.db!!.database.getCollection("test")
            coll.insertOne(Document())
            coll.updateMany(Document(), Updates.set("mem.ang", 10))
            System.out.printf((coll.find().first()["mem"] as Document?).toString())
            coll.drop()
        }
    }
}