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
	private Double wall_location;
	private Pair<Integer, Point> runner;
	


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
				wall_location = opp.x;
				return true;
			}
		}
		return false;
	}
	private Pair<Integer, Point> find_hole(HashMap<Integer, Point> opponent_pieces,HashMap<Integer, Point> player_pieces, Integer piece_id, Boolean direction) {
		Point curr_position = new Point(player_pieces.get(piece_id));
		Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, curr_position);	
		while(Board.check_within_bounds(move)){
			curr_position.y += direction ? diameter_piece : -diameter_piece;
			//System.out.println(curr_position.y);
			if(Board.check_collision(opponent_pieces, move)){
				return new Pair<Integer, Point>(piece_id,curr_position);
			}
		}
		return null; 
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
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
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

	private List<Pair<Integer, Point>> runner_strat(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		System.out.println("runner");
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		//List<Integer> player_list = new ArrayList<>(player_pieces.keySet());

		runner = new Pair(0, player_pieces.get(0));
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
			for (int i = 0; i < num_moves; i++){
				Pair<Integer, Point> move = fan_out(curr_position, theta, player_pieces, opponent_pieces, player);
				curr_position = move.getValue();
				possible_moves.add(move);
			}
			return(possible_moves);
				
		}
		//remove the runner from player_list
		final Pair<Integer, Point> run = runner;
		//player_pieces.entrySet().removeIf(e -> player_pieces.containsKey(run.getKey()));
		//Set<Integer> withoutRunner = new HashSet(player_pieces.keySet());
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
		player_list.remove(run.getKey());
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
		//send runner to wall_location
		Point curr_position = player_pieces.get(runner.getKey()); 
		List<Pair<Integer, Point>> possible_moves = new ArrayList<>();
		boolean at_wall = isplayer1 ? curr_position.x < wall_location : curr_position.x > wall_location;
		if(!at_wall){
			double theta = 0;
			Integer player = runner.getKey(); 
			for (int i = 0; i < num_moves; i++){
				Pair<Integer, Point> move = fan_out(curr_position, theta, player_pieces, opponent_pieces, player);
				curr_position = move.getValue();
				possible_moves.add(move);
			}
			return(possible_moves);	
		}
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
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

	private List<Pair<Integer, Point>> getPossibleMoves( HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, List<Integer> player_list){
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		for(int player : player_list){
			if (farEnough(player_pieces, opponent_pieces, player)) {
				continue; 
			}
			
			double theta = 0;
			Point curr_position = player_pieces.get(player);
			Pair<Integer, Point> move = fan_out(curr_position, theta, player_pieces, opponent_pieces, player);
			if(move!=null){
				possible_moves.add(move);
			}
			else{
				//possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list);
			}
		 }
		
		
		return possible_moves;
	}
	// private List<Pair<Integer, Point>> getUnStuck( HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, List<Integer> player_list){
	// 	System.out.println("stuck");
	// 	List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
	// 	double stop_angle = Math.PI; 
	// 	for(int player : player_list){
	// 		if (farEnough(player_pieces, opponent_pieces, player)) {
	// 			continue; 
	// 		}
	// 		// currently only looking upwards
	// 		Pair<Integer, Point> potential_move = find_hole(opponent_pieces, player_pieces, player, true);
	// 		if(potential_move!= null){
	// 			Point hole = potential_move.getValue(); 
	// 			double closest_dist = Double.MAX_VALUE; 
	// 			int closest_player = player; 
	// 			for (int option : player_list){
	// 				Point place = player_pieces.get(option); 
	// 				double dist = Math.sqrt(Math.pow(place.x - hole.x,2) - Math.pow(place.y - hole.y,2)); 
	// 				if (dist < closest_dist){
	// 					closest_dist = dist; 
	// 					closest_player = option; 
	// 				}
	// 			}
	// 			// we need to find a move for the player:
	// 			// we are gonna want this to be in a loop 
	// 			double theta = player_pieces.get(closest_player).y < hole.y ? Math.PI/2 : -Math.PI/2;
	// 			Pair<Integer, Point> move = fan_out(player_pieces.get(closest_player), theta, player_pieces, opponent_pieces, closest_player);
	// 			boolean over_line = isplayer1 ? player_pieces.get(closest_player).x < -21 : player_pieces.get(closest_player).x > 21;
	// 			if (over_line && !Board.check_collision(player_pieces, move)) {
	// 				continue;
	// 			}
	// 			System.out.println("HI");
	// 			possible_moves.add(move);
	// 		}
	// 		else{
	// 			// if we can't find a hole 
	// 			double theta = 0;
	// 			Point curr_position = player_pieces.get(player);
	// 			Pair<Integer, Point> move = new Pair<Integer, Point>(player,curr_position);
	// 			do {
	// 				Point new_position = new Point(curr_position);
	// 				double delta_x = diameter_piece * Math.cos(theta);
	// 				double delta_y = diameter_piece * Math.sin(theta);

	// 				new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
	// 				new_position.y += delta_y;

	// 				move = new Pair<Integer, Point>(player, new_position);
	// 				theta = theta > 0 ? -theta : -theta + random.nextDouble()*Math.PI; 
	// 				//theta = theta + random.nextDouble()*Math.PI; 
					
	// 				if (check_validity(move, player_pieces, opponent_pieces)){
	// 					boolean over_line = isplayer1 ? curr_position.x < -21 : curr_position.x > 21;
	// 					if (over_line && !Board.check_collision(player_pieces, move)) {
	// 						continue;
	// 					}
	// 					System.out.println("HELLO");
	// 					possible_moves.add(move);
	// 					//break;
	// 				}
					
	// 			}
	// 			while(Math.abs(theta) < stop_angle); 
	// 		}
			
	// 	 }
		
	// 	return possible_moves;
	// }


	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		
		if (player_pieces.size() < 11) {
			return main_strat(num_moves, player_pieces, opponent_pieces, isplayer1);
		}
		else{ //send a runner:
			if(check_asshole(opponent_pieces)){
				System.out.println("asshole");
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

