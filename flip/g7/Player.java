package flip.g7;
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

	public Boolean farEnough(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece_id){
		Point curr_position = player_pieces.get(piece_id);
		// TODO: weight number of pieces accross & factor in blocking 
		// do not hard code 40 
		if (curr_position.x > 40){
			return true;
		}
		return false; 
	}


	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		 List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

		 int num_trials = 30;
		 int i = 0;

		 while(moves.size()!= num_moves && i<num_trials)
		 {
			// TODO: choose piece not randomly 
			Integer piece_id = random.nextInt(n);
			while(farEnough(player_pieces, opponent_pieces, piece_id)){
				piece_id = random.nextInt(n);
			}			
			Point curr_position = player_pieces.get(piece_id);
			Point new_position = new Point(curr_position);
			double theta = 0;
			Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id,curr_position);
			do {
				// TODO: want to change theta if blocked 
				double delta_x = diameter_piece * Math.cos(theta);
				double delta_y = diameter_piece * Math.sin(theta);

				Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));

				new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
				new_position.y += delta_y;
				move = new Pair<Integer, Point>(piece_id, new_position);
				
				Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
				// theta = theta > 0 ? theta+Math.PI/2 : theta-Math.PI/2;
				theta = -Math.PI/2 + Math.PI*random.nextDouble();
			
			} while(Board.check_collision(player_pieces, move) || Board.check_collision(opponent_pieces, move));
		 	if(check_validity(move, player_pieces, opponent_pieces)){
				moves.add(move);
			}
		 	i++;
		 }
		 
		 return moves;
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

