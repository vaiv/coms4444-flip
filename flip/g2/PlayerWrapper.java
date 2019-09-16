package flip.g2;

import flip.sim.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import javafx.util.Pair;

import flip.sim.Player;
import flip.sim.Point;

public class PlayerWrapper {

    private final Timer timer;
    private final Player player;
    private final String name;
    private final long timeout;

    public PlayerWrapper(Player player, String name, long timeout) {
        this.player = player;
        this.name = name;
        this.timeout = timeout;
        this.timer = new Timer();
    }

    // Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
    public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece) {
        Log.record("Initializing player " + this.name);
        // Initializing ID mapping array
        try {
            if (!timer.isAlive()) {
                timer.start();
            }

            timer.call_start(() -> {
                player.init(pieces, n, t, isplayer1, diameter_piece);
                return null;
            });

            timer.call_wait(timeout);
        } catch (Exception ex) {
            Log.record("Player " + this.name + " has possibly timed out.");
            // throw ex;
        }

    }

    // Gets the moves from the player. Number of moves is specified by first parameter.
    public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        Log.record("Getting moves for player " + this.name);
        List<Pair<Integer, Point>> moves = new ArrayList<>();

        try {
            if (!timer.isAlive()) {
                timer.start();
            }

            timer.call_start(() -> player.getMoves(num_moves, player_pieces, opponent_pieces, isplayer1));

            moves = timer.call_wait(timeout);
        } catch (Exception ex) {
            Log.record("Player " + this.name + " has possibly timed out.");
            // throw ex;
        }

        return moves;
    }

    public String getName() {
        return name;
    }

    public Player getPlayer() {
        return player;
    }

    public void destroy() {
        timer.destroy();
    }
}
