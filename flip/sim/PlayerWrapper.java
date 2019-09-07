package flip.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.*;
import javafx.util.Pair; 

import flip.sim.Player;

public class PlayerWrapper {
    private Timer timer;
    private Player player;
    private String name;
    private long timeout;

    public PlayerWrapper(Player player, String name, long timeout) 
    {
        this.player = player;
        this.name = name;
        this.timeout = timeout;
        this.timer = new Timer();
    }

    // Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
    public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece) 
    {
        Log.record("Initializing player " + this.name);
        // Initializing ID mapping array
         try {
            if (!timer.isAlive()) timer.start();

            timer.call_start(new Callable<Void>() 
            {
                @Override
                public Void call() throws Exception 
                {
                    player.init(pieces, n, t, isplayer1, diameter_piece);
                    return null;
                }
            });

            timer.call_wait(timeout);
        }
        catch (Exception ex) 
        {
            Log.record("Player " + this.name + " has possibly timed out.");
            // throw ex;
        }       
        
        
    }
    // Gets the moves from the player. Number of moves is specified by first parameter.
    public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) 
    {
        Log.record("Getting moves for player " + this.name);
        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

        try 
        {
            if (!timer.isAlive()) timer.start();

            timer.call_start(new Callable<List<Pair<Integer, Point>>>() 
            {
                @Override
                public List<Pair<Integer, Point>> call() throws Exception 
                {
                    return player.getMoves(num_moves, player_pieces, opponent_pieces, isplayer1);
                }
            });

            moves = timer.call_wait(timeout);
        }
        catch (Exception ex) 
        {
            Log.record("Player " + this.name + " has possibly timed out.");
            // throw ex;
        }       

        return moves;
    }

    public String getName() {
        return name;
    }
}