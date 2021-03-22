package twp.commands.tests

internal object MapManagerTest : Test() {
    @JvmStatic
    fun main(args: Array<String>) {
        Test.Companion.init()
        MapHandler.mapFolder = "C:\\Users\\jakub\\Documents\\programming\\java\\mindustry_plugins\\TheWorstV3"
        MapManager.terminal.run("", "add", "nonexistent.msav")
        MapManager.terminal.assertResult(Command.Result.notExist)
        MapManager.terminal.run(
            "",
            "add",
            "C:\\Users\\jakub\\Documents\\programming\\java\\mindustry_plugins\\TheWorstV3\\libs\\Novastar_V2.1.msav"
        )
        MapManager.terminal.assertResult(Command.Result.addSuccess)
        MapManager.terminal.run(
            "",
            "add",
            "C:\\Users\\jakub\\Documents\\programming\\java\\mindustry_plugins\\TheWorstV3\\libs\\Novastar_V2.1.msav"
        )
        MapManager.terminal.assertResult(Command.Result.alreadyAdded)
        MapManager.terminal.run(
            "",
            "add",
            "C:\\Users\\jakub\\Documents\\programming\\java\\mindustry_plugins\\TheWorstV3\\libs\\dummy.msav"
        )
        MapManager.terminal.assertResult(Command.Result.invalidFile)
        MapManager.terminal.run(
            "",
            "update",
            "C:\\Users\\jakub\\Documents\\programming\\java\\mindustry_plugins\\TheWorstV3\\libs\\Novastar_V2.1.msav"
        )
        MapManager.terminal.assertResult(Command.Result.updateSuccess)
        MapManager.terminal.run("", "enable", "noainteger")
        MapManager.terminal.assertResult(Command.Result.notInteger)
        MapManager.terminal.run("", "enable", "0")
        MapManager.terminal.assertResult(Command.Result.success)
        MapManager.terminal.run("", "enable", "0")
        MapManager.terminal.assertResult(Command.Result.alreadyEnabled)
        MapManager.terminal.run("", "disable", "0")
        MapManager.terminal.assertResult(Command.Result.success)
        MapManager.terminal.run("", "disable", "0")
        MapManager.terminal.assertResult(Command.Result.alreadyDisabled)
        MapManager.terminal.run("", "remove", "0")
        MapManager.terminal.assertResult(Command.Result.success)
        Assertions.assertNull(Main.db!!.maps.getMap(0))
        MapManager.terminal.run("", "remove", "0")
        MapManager.terminal.assertResult(Command.Result.notFound)
    }
}