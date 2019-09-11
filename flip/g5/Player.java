package flip.g5;
import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import javafx.util.Pair; 
import java.util.ArrayList;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;

public class Player implements flip.sim.Player
{
	private int seed = 42;
	private Random random;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;


	public Player()
	{
		random = new Random(seed);
	}

	// Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
	{
		this.n = n;
		this.isplayer1 = isplayer1;
		this.diameter_piece = diameter_piece;
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		System.out.println("moves requested");
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
		 
		Pair<Integer, Point> best_move = getBestMove(player_pieces, opponent_pieces, isplayer1);
		moves.add(best_move);
		player_pieces.put(best_move.getKey(), best_move.getValue());
		Pair<Integer, Point> best_move2 = getBestMove(player_pieces, opponent_pieces, isplayer1);
		moves.add(best_move2);
		 
		return moves;
	}

	private Pair<Integer, Point> getBestMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		HashMap<Integer, Point> player_pieces_eval = deepCopy(player_pieces);
		Pair<Integer, Point> best_move = null;
		Pair<Integer, Point> move = null;
		Double best_value = evaluate(player_pieces, opponent_pieces);
		System.out.println("current_value: " + best_value);
		for (int i = 0; i < n; i++) {
		 	Point curr_position = player_pieces.get(i);
		 	Point new_position = new Point(curr_position.x, curr_position.y);
		 	new_position.x = isplayer1 ? new_position.x - 2 : new_position.x + 2;
		 	new_position.y = new_position.y;
		 	move = new Pair<Integer, Point>(i, new_position);
		 	if(check_validity(move, player_pieces, opponent_pieces)) {
		 		player_pieces_eval.put(move.getKey(), move.getValue());
		 		Double value = evaluate(player_pieces_eval, opponent_pieces);
		 		System.out.println("valid move! move: " + move + " value: " + value);
		 		if (isplayer1 && (value < best_value)) {
		 			best_value = value;
		 			best_move = move;
		 		} else if (!isplayer1 && (value > best_value)) {
		 			best_value = value;
		 			best_move = move;
		 		}
		 	}
			player_pieces_eval = deepCopy(player_pieces);
		}
		System.out.println("best move! move: " + best_move + " best_value: " + best_value);
		return best_move;
	}

	private HashMap<Integer, Point> deepCopy(HashMap<Integer, Point> map) {
		HashMap<Integer, Point> copy = new HashMap<>();
		for (Map.Entry<Integer, Point> entry: map.entrySet()) {
			copy.put(new Integer(entry.getKey()), new Point(entry.getValue()));
		}
		return copy;
	}

	private double evaluate(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
		double value = 0.0;
		for (Map.Entry<Integer, Point> entry: player_pieces.entrySet()) {
			value += (60+entry.getValue().x) * (60+entry.getValue().x);
		}
		return value;
	}

	public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        boolean valid = true;
       
        // check if move is adjacent to previous position.
        if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), diameter_piece))
            {
                return false;
            }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;

    }
}
