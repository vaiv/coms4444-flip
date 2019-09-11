package flip.greedy;
import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
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
	private Double largest_x;

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
		System.out.println("N = " + n);
		this.n = n;
		this.isplayer1 = isplayer1;
		this.diameter_piece = diameter_piece;
		this.largest_x = isplayer1 ? -20 - 4 * diameter_piece : 20 + 4 * diameter_piece;
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();

		//my code here
		for (int i = 0; i < n; i++) {
			Point curr_position = player_pieces.get(i);

			Point new_position = new Point(curr_position);
			new_position.x = isplayer1 ? new_position.x - this.diameter_piece : new_position.x + this.diameter_piece;
			// Want delta x > 0
			Pair<Integer, Point> move = new Pair<Integer, Point>(i, new_position);
			if(check_validity(move, player_pieces, opponent_pieces)) {
				//System.out.println("IDEAL: " + move.getValue().x);
				if(isplayer1) {
					if(move.getValue().x < -30) continue;
				} else {
					if(move.getValue().x > 30) continue;
				}
				moves.add(move);
				continue;
			}
			double theta = 0.1;
			do {
				new_position.x = curr_position.x;
				new_position.y = curr_position.y;

				//TODO: could create a table w/ these values to make runtime faster
				double delta_x1 = diameter_piece * Math.cos(theta);
				double delta_y1 = diameter_piece * Math.sin(theta);

				new_position.x = isplayer1 ? new_position.x - delta_x1 : new_position.x + delta_x1;
				new_position.y += delta_y1;
				move = new Pair<Integer, Point>(i, new_position);
				if(check_validity(move,	player_pieces, opponent_pieces)) {
					//System.out.println("VALID");
					break;
				}
				//check same move reflected over x axis
				new_position.x = curr_position.x;
				new_position.y = curr_position.y;
				double delta_x2 = diameter_piece * Math.cos(-1 * theta);
				double delta_y2 = diameter_piece * Math.sin(-1 * theta);
				new_position.x = isplayer1 ? new_position.x - delta_x2 : new_position.x + delta_x2;
				new_position.y += delta_y2;
				move = new Pair<Integer, Point>(i, new_position);
				if(check_validity(move,	player_pieces, opponent_pieces)) {
					//System.out.println("VALID");
					break;
				}
				theta += .1;
			} while (theta < Math.PI / 2 - .05); //slightly under 90 degree angle

		 	if(check_validity(move, player_pieces, opponent_pieces)) {
		 		if(isplayer1) {
					if(move.getValue().x < -30) continue;
				} else {
					if(move.getValue().x > 30) continue;
				}
				possible_moves.add(move);
			}
		 }
		 if(moves.size() >= 2)
		 	return pick_i_moves(num_moves, moves, isplayer1); 

		//System.out.println("POSSIBLE MOVES: " + possible_moves.size());
		return pick_moves(num_moves, possible_moves, moves, isplayer1);
	}

	public List<Pair<Integer, Point>> pick_i_moves(Integer num_moves, List<Pair<Integer, Point>> possible_moves, boolean isplayer1) {
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

		for(int i = 0; i<num_moves; i++) {
			if(possible_moves.isEmpty()) {
				return moves;
			}
			Pair<Integer, Point> best_move = possible_moves.get(0);
			for(int j = 1; j<possible_moves.size(); j++) {
				if(isBetterThan(best_move, possible_moves.get(j), isplayer1)) {
					best_move = possible_moves.get(j);
				}
			}
			moves.add(best_move);
			possible_moves.remove(best_move);
		}
		/*for(int i = 0; i < num_moves; i++) {
			System.out.println(moves.get(i).getValue().x);
		}*/
		return moves;
	}

	public List<Pair<Integer, Point>> pick_moves(Integer num_moves, List<Pair<Integer, Point>> possible_moves, List<Pair<Integer, Point>> i_moves, boolean isplayer1) {
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
		moves.addAll(i_moves);

		for(int i = 0; i<num_moves-moves.size(); i++) {
			if(possible_moves.isEmpty()) {
				return moves;
			}
			Pair<Integer, Point> best_move = possible_moves.get(0);
			for(int j = 1; j<possible_moves.size(); j++) {
				if(isBetterThan(best_move, possible_moves.get(j), isplayer1)) {
					best_move = possible_moves.get(j);
				}
			}
			moves.add(best_move);
			possible_moves.remove(best_move);
		}
		return moves;
	}

	private boolean isBetterThan(Pair<Integer, Point> current_move, Pair<Integer, Point> new_move, boolean isplayer1) {
		if(isplayer1) {
			return current_move.getValue().x > new_move.getValue().x;
		} else {
			return current_move.getValue().x < new_move.getValue().x;
		}
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

