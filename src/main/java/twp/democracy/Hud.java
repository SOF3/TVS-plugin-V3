package twp.democracy;

import mindustry.gen.Call;
import twp.Main;
import twp.database.PD;
import twp.database.enums.Setting;
import twp.tools.*;

import java.util.ArrayList;
import java.util.Iterator;

import static twp.Main.*;

// Hud manages updating of ingame hud, it also removes disconnected players from online list
public class Hud {

    ArrayList<Displayable> displayable = new ArrayList<>();

    public Hud() {
        displayable.add(Voting.processor);
        displayable.add(docks);
        Logging.on(Main.TickEvent.class, e -> update());
    }

    public void sendMessage(String message, Object[] args, int seconds, String ...colors) {
        Message.messages.add(new Message(message, args, seconds, colors));
    }

    public void update() {
        for (Message value : Message.messages) {
            value.tick();
        }
        for(Displayable displayable : displayable) {
            displayable.tick();
        }
        Call.sendMessage(shooter.garbage.toString());
        for(Iterator<PD> iter = db.online.values().iterator(); iter.hasNext(); ){
            PD pd = iter.next();
            if(pd.isInvalid()) {
                return;
            }

            if(pd.disconnected()) {
                iter.remove();
            }

            if(!db.hasEnabled(pd.id, Setting.hud)) {
                Call.hideHudText(pd.player.p.con);
                return;
            }
            StringBuilder sb = new StringBuilder();

            for(Displayable displayable : displayable) {
                sb.append(displayable.getMessage(pd));
            }

            Iterator<Message> it = Message.messages.iterator();
            while (it.hasNext()){
                Message message = it.next();
                sb.append(message.getMessage(pd)).append("\n");
                if (message.counter < 1) {
                    it.remove();
                }
            }

            if(sb.length() == 0) {
                Call.hideHudText(pd.player.p.con);
            } else {
                Call.setHudText(pd.player.p.con, "[#cbcbcb]" + sb.substring(0, sb.length() - 1));
            }

        }

    }

    public interface Displayable {
        String getMessage(PD pd);
        void tick();
    }

    static class Message implements Displayable {
        static ArrayList<Message> messages = new ArrayList<>();

        int counter;
        Object[] args;
        String message;
        String[] colors;

        Message(String message, Object[] args, int counter, String ...colors) {
            this.message = message;
            this.counter = counter;
            this.args = args;

            if(colors.length == 0) {
                this.colors = new String[]{"white"};
            } else {
                this.colors = colors;
            }
        }


        @Override
        public String getMessage(PD pd) {

            return String.format("[%s]%s[](%ds)", colors[counter % colors.length], pd.translate(message, args), counter);
        }

        @Override
        public void tick() {
            counter--;
        }
    }
}
