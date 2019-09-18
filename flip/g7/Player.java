package flip.g7;
import java.util.List;
import java.util.Collections;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import javafx.util.Pair; 
import java.util.ArrayList;
import java.lang.Math;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;
/**
 * 
 * Todo: 
 * - implement multiple heuristics 
 * - make move backwards if stuck 
 * - implement and balance multiple strategies (rn ours is weak vs wall)
 * 		- greedy strategy 
 * 		- wall strategy ?
 */


public class Player implements flip.sim.Player
{
	private int seed = 42;
	private Random random;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;

	private boolean initialized = false;
	private HashMap<Point, Integer> loc_to_id = new HashMap<>();
	


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

	private Boolean farEnough(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece_id){
		Point curr_position = player_pieces.get(piece_id);
		// TODO: weight number of pieces accross & factor in blocking 
		// do not hard code 12 / 20 
		// factor in pieces behind you 
		int cutoff = (int) (Math.ceil(player_pieces.size() / 10)*diameter_piece) + 20; 
		boolean over_edge = isplayer1 ?  curr_position.x < -cutoff : curr_position.x > cutoff ;
		if (over_edge){
			return true;
		}
		return false; 
	}


	public double lookForward(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece_id, boolean isplayer1){
		Point curr_position = player_pieces.get(piece_id);
		Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, curr_position);	
		while(valid(player_pieces, opponent_pieces, move)){
			// Ideally make this more aware than just direct obstacles
			curr_position.x += isplayer1 ? diameter_piece : -diameter_piece;
			move = new Pair<Integer, Point>(piece_id,curr_position);
		}
		return (curr_position.x-player_pieces.get(piece_id).x)/diameter_piece; 
	}

	private boolean valid(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Pair<Integer, Point> move) {
		boolean valid = true;
		// check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
		return valid;
	}

	private List<Pair<Integer, Point>> getPossibleMoves( HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, List<Integer> player_list){
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		double stop_angle = Math.PI/2; 
		for(int player : player_list){
			if (farEnough(player_pieces, opponent_pieces, player)) {
				continue; 
			}
			double theta = 0;
			Point curr_position = player_pieces.get(player);
			Pair<Integer, Point> move = new Pair<Integer, Point>(player,curr_position);
			do {
				Point new_position = new Point(curr_position);
				double delta_x = diameter_piece * Math.cos(theta);
				double delta_y = diameter_piece * Math.sin(theta);

				new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
				new_position.y += delta_y;

				move = new Pair<Integer, Point>(player, new_position);
				
				theta = theta > 0 ? -theta : -theta + 0.01; 
				
				if (check_validity(move, player_pieces, opponent_pieces)){
					possible_moves.add(move);
					break;
				}
				
			}
			while(Math.abs(theta) < stop_angle); 
		 }
		
		return possible_moves;
	}
	private List<Pair<Integer, Point>> getUnStuck( HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, List<Integer> player_list){
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		double stop_angle = Math.PI*2; 
		for(int player : player_list){
			if (farEnough(player_pieces, opponent_pieces, player)) {
				continue; 
			}
			double theta = 0;
			Point curr_position = player_pieces.get(player);
			Pair<Integer, Point> move = new Pair<Integer, Point>(player,curr_position);
			do {
				Point new_position = new Point(curr_position);
				double delta_x = diameter_piece * Math.cos(theta);
				double delta_y = diameter_piece * Math.sin(theta);

				new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
				new_position.y += delta_y;

				move = new Pair<Integer, Point>(player, new_position);
				
				theta = theta + random.nextDouble()*Math.PI; 
				
				if (check_validity(move, player_pieces, opponent_pieces)){
					boolean over_line = isplayer1 ? curr_position.x < -20 : curr_position.x > 20;
					if (over_line && !Board.check_collision(player_pieces, move)) {
						continue;
					}
					possible_moves.add(move);
				}
				
			}
			while(Math.abs(theta) < stop_angle); 
		 }
		
		return possible_moves;
	}


	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{

		if (!initialized) {
			for (int i = 0; i < player_pieces.size(); i++){
				loc_to_id.put(opponent_pieces.get(i), -i);
				loc_to_id.put(player_pieces.get(i), i);
				
			}
			loc_to_id.put(opponent_pieces.get(0), -opponent_pieces.size());
			initialized = true; 
		}
		
		
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
		
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
		Collections.shuffle(player_list);
		Collections.sort(player_list, new Comparator<Integer>(){
			@Override
			public int compare(Integer pt1, Integer pt2) {
				// depends on player
				return (int)(lookForward(player_pieces, opponent_pieces, pt1, isplayer1)-lookForward(player_pieces, opponent_pieces, pt2, isplayer1));
			}
		});
		
		List<Pair<Integer, Point>> possible_moves = getPossibleMoves(player_pieces, opponent_pieces, isplayer1, player_list);
		System.out.println(possible_moves.size());
		if (possible_moves.size() < num_moves) {
			// get list of moves not over the line 
			possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list);
		} 
		moves = possible_moves.subList(0, num_moves);

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

