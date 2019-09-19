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
 * Notes: it is splitting the two moves between two people
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
		// not accurate 
		int cutoff = (int) (Math.ceil(player_pieces.size() / 10)*diameter_piece) + 21; 
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

	private boolean check_asshole(HashMap<Integer, Point> opponent_pieces) {
		HashMap<Double, Integer> locations = new HashMap<Double, Integer>();
		for (Point opp : opponent_pieces.values()){
			if (!locations.containsKey(opp.x)) locations.put(opp.x, 0);
			locations.put(opp.x, locations.get(opp.x)+1);
			if (locations.get(opp.x) == 3){
				System.out.println("this is an asshole move");
				return true;
			}
		}
		return false;
	}

	private Pair<Integer, Point> fan_out(Point curr_position, double theta, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer player) {
		Pair<Integer, Point> move = new Pair<Integer, Point>(player,curr_position);
		double stop_angle = Math.PI/2; 
		do {
			Point new_position = new Point(curr_position);
			double delta_x = diameter_piece * Math.cos(theta);
			double delta_y = diameter_piece * Math.sin(theta);

			//Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));

			new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
			new_position.y += delta_y;

			move = new Pair<Integer, Point>(player, new_position);
			
			//Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
			theta = theta > 0 ? -theta : -theta + 0.01; 
			
			if (valid(player_pieces, opponent_pieces, move)){
				return move; 
			}
		}
		while(Math.abs(theta) < stop_angle);	
		return null; 
	}

	private List<Pair<Integer, Point>> main_strat(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
		Collections.sort(player_list, new Comparator<Integer>(){
			@Override
			public int compare(Integer pt1, Integer pt2) {
				// depends on player
				return (int)(lookForward(player_pieces, opponent_pieces, pt1, isplayer1)-lookForward(player_pieces, opponent_pieces, pt2, isplayer1));
			}
		});
		possible_moves = getPossibleMoves(player_pieces, opponent_pieces, isplayer1, player_list);
		if (possible_moves.size() < num_moves) {
			possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list);
		}
		return possible_moves.subList(0, num_moves);
	}

	private List<Pair<Integer, Point>> runner_strat(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());

		Pair<Integer, Point> runner = new Pair(0, player_pieces.get(0));
		for(Integer id : player_pieces.keySet()){
			Point op = player_pieces.get(id);
			boolean closer = isplayer1 ? op.x < runner.getValue().x : op.x > runner.getValue().x;
			if(closer){
				runner = new Pair(id, op);
			}
		}
		boolean halfway = isplayer1 ? runner.getValue().x < 0 : runner.getValue().x > 0; 
		if(!halfway){
			double theta = 0;
			Point curr_position = runner.getValue(); 
			Integer player = runner.getKey(); 
			// want more than one move here; 
			for (int i = 0; i < num_moves; i++){
				Pair<Integer, Point> move = fan_out(curr_position, theta, player_pieces, opponent_pieces, player);
				curr_position = move.getValue();
				possible_moves.add(move);
			}
			return(possible_moves);
				
		}
		Collections.shuffle(player_list);		
		Collections.sort(player_list, new Comparator<Integer>(){
			@Override
			public int compare(Integer pt1, Integer pt2) {
				// depends on player
				return (int)(lookForward(player_pieces, opponent_pieces, pt1, isplayer1)-lookForward(player_pieces, opponent_pieces, pt2, isplayer1));
			}
		});
		possible_moves = getPossibleMoves(player_pieces, opponent_pieces, isplayer1, player_list);	
		if (possible_moves.size() < num_moves) {
			possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list);
		}
		return possible_moves.subList(0, num_moves);
	}


	private List<Pair<Integer, Point>> block_strat(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
		Collections.sort(player_list, new Comparator<Integer>(){
			@Override
			public int compare(Integer pt1, Integer pt2) {
				// depends on player
				return (int)(lookForward(player_pieces, opponent_pieces, pt1, isplayer1)-lookForward(player_pieces, opponent_pieces, pt2, isplayer1));
			}
		});
		List<Pair<Integer, Point>> possible_moves = getPossibleMoves(player_pieces, opponent_pieces, isplayer1, player_list);
		if (possible_moves.size() < num_moves) {
			possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list);
		}
		return possible_moves.subList(0, num_moves);

	}

	private List<Pair<Integer, Point>> getPossibleMoves( HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, List<Integer> player_list){
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		for(int player : player_list){
			if (farEnough(player_pieces, opponent_pieces, player)) {
				continue; 
			}
			double theta = 0;
			Point curr_position = player_pieces.get(player);
			Pair<Integer, Point> move = fan_out(curr_position, theta, player_pieces, opponent_pieces, player);
			if(move!=null) possible_moves.add(move);
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
		
		if (player_pieces.size() < 11) {
			return main_strat(num_moves, player_pieces, opponent_pieces, isplayer1);
		}
		else{ //send a runner:
			if(check_asshole(opponent_pieces)){
				if (!initialized) {
					for (int i = 0; i < player_pieces.size(); i++){
						loc_to_id.put(opponent_pieces.get(i), -i);
						loc_to_id.put(player_pieces.get(i), i);
						
					}
					loc_to_id.put(opponent_pieces.get(0), -opponent_pieces.size());
					initialized = true; 
				}
				// if they start building a wall -- want to block it with runner already there 
				return block_strat(num_moves, player_pieces, opponent_pieces, isplayer1);
			} else {
				return runner_strat(num_moves, player_pieces, opponent_pieces, isplayer1);
			}
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

