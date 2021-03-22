package twp.commands

import arc.util.Time
import twp.database.PD
import twp.Global
import twp.Main
import twp.tools.Json
import twp.tools.Text
import java.io.File
import java.lang.StringBuilder
import java.util.HashMap

class Tester : Command() {
    var tests = HashMap<String, Test>()
    var recent = HashMap<String, Long>()
    override fun run(id: String, vararg args: String) {
        if (cannotInteract(id)) {
            return
        }
        val since = Time.timeSinceMillis(recent.getOrDefault(id, 0L))
        if (since < Main.config.testPenalty) {
            setArg(Text.milsToTime(Main.config.testPenalty - since))
            result = Result.penalty
            return
        }
        if (args.size > 0 && !tests.containsKey(id)) {
            result = Result.notFound
            return
        }
        val test = tests.computeIfAbsent(id) { k: String? -> Test(caller, this) }
        if (args.size > 0) {
            if (isNotInteger(args, 0)) {
                return
            }
            test.processAnswer(args[0].toInt())
            if (result == Result.invalidRequest) {
                return
            }
            if (test.finished()) {
                test.evaluate(caller)
                return
            }
        }
        test.ask(caller)
    }

    class Test(pd: PD, var tester: Tester) {
        var question: String? = null
        var options: Array<String>
        var progress = 0
        var points = 0
        var empty = false
        var questions: HashMap<String?, Array<String>>?
        fun loadQuestions(locStr: String): HashMap<String?, Array<String>>? {
            var bundle = testFile + "_" + locStr.replace("-", "_") + ".json"
            val fi = File(bundle)
            if (!fi.exists() || fi.isDirectory) bundle = testFile + ".json"
            return Json.loadHashmap(bundle, Array<String>::class.java, example)
        }

        fun ask(pd: PD) {
            val sb = StringBuilder()
            question = questions!!.keys.toTypedArray()[progress]
            sb.append(question).append("\n")
            options = questions!![question]!!
            for (i in options.indices) {
                sb.append("[yellow]").append(i + 1).append(")[gray]")
                sb.append(options[i].replace("#", ""))
                sb.append("\n")
            }
            pd.sendMessage(sb.toString())
            tester.setArg(1, options.size)
            tester.result = Result.hint
        }

        fun evaluate(pd: PD) {
            if (points == questions!!.size) {
                tester.setArg(Main.ranks!!.verified.suffix)
                RankSetter.terminal.run("", pd.id.toString(), "verified")
            } else {
                tester.setArg(points, questions!!.size)
                tester.result = Result.testFail
                tester.recent[pd.player.uuid] = Time.millis()
            }
            tester.tests.remove(pd.player.uuid)
        }

        fun finished(): Boolean {
            return progress == questions!!.size
        }

        fun processAnswer(answer: Int) {
            if (answer > options.size || answer <= 0) {
                tester.result = Result.invalidRequest
                tester.setArg(answer, options.size)
                return
            }
            if (options[answer - 1].startsWith("#")) {
                points += 1
            }
            progress++
        }

        init {
            questions = loadQuestions(pd.locString)
            if (questions!!.isEmpty()) {
                pd.sendServerMessage("test-missing")
                tester.tests.remove(pd.player.uuid)
                empty = true
            }
            pd.sendServerMessage("test-start")
        }
    }

    companion object {
        const val testFile = Global.config_dir + "test"
        private val example: HashMap<String, Array<String>> = object : HashMap<String?, Array<String?>?>() {
            init {
                put(
                    "Some question?", arrayOf<String>(
                        "Some answer",
                        "Som other answer",
                        "#correct answer"
                    )
                )
                put(
                    "Another question?", arrayOf<String>(
                        "Some answer",
                        "Som other answer",
                        "#correct answer",
                        "#correct answer"
                    )
                )
            }
        }
        var game = Tester()
    }

    init {
        name = "test"
        argStruct = "[answer-number]"
        description = "Easiest way to get verified."
    }
}