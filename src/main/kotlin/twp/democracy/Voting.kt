package twp.democracy

import arc.math.Mathf
import twp.commands.Command
import twp.database.PD
import java.lang.StringBuilder
import twp.database.enums.Perm
import java.util.concurrent.atomic.AtomicInteger
import twp.database.enums.Stat
import twp.Main
import java.util.ArrayList
import java.util.HashSet

// Voting handles all of the vote sessions
open class Voting(var parent: Command, var name: String, var minVotes: Int, var maxVotes: Int) {
    @JvmField
    var protection: Perm? = null
    @JvmField
    var increase: Stat? = null
    val majority: Int
        get() {
            val counter = AtomicInteger()
            for (pd in Main.db!!.online.values) {
                if (pd.canParticipate()) {
                    counter.getAndIncrement()
                }
            }
            val count = counter.get()
            return if (count == 1) {
                minVotes
            } else Mathf.clamp(count / 2 + if (count % 2 == 0) 0 else 1, 2, maxVotes)
        }

    fun pushSession(pd: PD, runner: VoteRunner, vararg args: Any?): Command.Result {
        if (processor.isVoting(pd.id)) {
            return Command.Result.alreadyVoting
        }
        if (pd.cannotInteract()) {
            return Command.Result.cannotVote
        }
        processor.addSession(Session(pd.hasThisPerm(protection), this, runner, args, pd.id))
        processor.addVote(pd.id)
        return Command.Result.voteStartSuccess
    }

    enum class Messages {
        request, fail, success
    }

    fun revolve(session: Session) {
        // session is always special at the end unless time runs out
        if (session.spacial && session.yes > session.no) {
            if (increase != null) {
                Main.db!!.handler.inc(session.owner, increase, 1)
            }
            session.runner.run(session)
            if (Main.testMode) return
            Main.hud!!.sendMessage(getMessage(Messages.success), session.args, 10, "green", "gray")
        } else {
            if (Main.testMode) return
            Main.hud!!.sendMessage(getMessage(Messages.fail), session.args, 10, "red", "gray")
        }
    }

    fun getMessage(messages: Messages): String {
        return parent.name + "-" + name + "-" + messages.name
    }

    interface VoteRunner {
        fun run(session: Session?)
    }

    class Session(special: Boolean, voting: Voting, runner: VoteRunner, args: Array<Any?>, owner: Long) {
        @JvmField
        var voting: Voting
        var runner: VoteRunner
        var counter: Int
        var yes = 0
        var no = 0
        var spacial: Boolean
        var owner: Long
        var voted = HashSet<Long>()
        @JvmField
        var args: Array<Any?>
        fun run() {
            voting.revolve(this)
        }

        companion object {
            const val duration = 60
        }

        init {
            counter = duration
            if (special) {
                counter /= 3
            }
            this.runner = runner
            this.owner = owner
            this.args = args
            spacial = special
            this.voting = voting
        }
    }

    class Processor : Hud.Displayable {
        var sessions = ArrayList<Session>()
        fun query(con: Query): Int {
            var i = 0
            for (s in sessions) {
                if (con[s]) {
                    return i
                }
                i++
            }
            return -1
        }

        fun isVoting(id: Long): Boolean {
            for (s in sessions) {
                if (s.owner == id) {
                    return true
                }
            }
            return false
        }

        fun addSession(session: Session) {
            sessions.add(session)
        }

        fun addVote(idx: Int, id: Long, vote: String): Command.Result {
            if (sessions.size <= idx) {
                return Command.Result.invalidVoteSession
            }
            val s = sessions[idx]
            if (s.voted.contains(id)) {
                return Command.Result.alreadyVoted
            }
            s.voted.add(id)
            if ("y" == vote) {
                s.yes++
            } else {
                s.no++
            }
            val major = s.voting.majority
            if (s.yes >= major || s.no >= major || Main.testMode) {
                s.spacial = true
                s.run()
                sessions.remove(s)
            }
            return Command.Result.voteSuccess
        }

        fun addVote(id: Long) {
            addVote(sessions.size - 1, id, "y")
        }

        override fun getMessage(pd: PD): String {
            val sb = StringBuilder()
            var i = 0
            for (s in ArrayList(sessions)) {
                val major = s.voting.majority
                val ac = Main.db!!.handler.getAccount(s.owner)
                sb.append(if (s.counter % 2 == 0) "[gray]" else "[white]")
                sb.append(pd.translate(s.voting.getMessage(Messages.request), *s.args))
                sb.append("[]\n")
                if (s.spacial) {
                    sb.append(pd.translate("vote-specialStatus", i, s.yes, s.no, ac.name, s.owner, s.counter))
                } else {
                    sb.append(pd.translate("vote-status", i, s.yes, s.no, major, ac.name, s.owner, s.counter))
                }
                sb.append("\n")
                i++
            }
            return sb.toString()
        }

        override fun tick() {
            for (s in ArrayList(sessions)) {
                s.counter--
                if (s.counter < 0) {
                    s.run()
                    sessions.remove(s)
                }
            }
        }

        interface Query {
            operator fun get(s: Session?): Boolean
        }
    }

    companion object {
        @JvmField
        var processor = Processor()
    }
}