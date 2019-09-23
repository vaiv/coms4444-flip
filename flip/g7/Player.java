package flip.g7;
import java.util.List;
import java.util.Collections;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import javafx.util.Pair; 
import java.util.ArrayList;
import java.lang.Math;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;
/**
 * 
 * Todo: 
 * - fix get unstuck 
 * - add blocker & runner 
 * Notes: it is splitting the two moves between two people
 */


public class Player implements flip.sim.Player
{
	private int seed = 42;
	private Random random;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;

	private HashMap<Integer, Boolean> stuck_pieces = new HashMap<>();
	private Double wall_location;
	private Pair<Integer, Point> runner;
	private HashSet<Point> potential_holes = new HashSet<>(); 
	


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

	private boolean check_wall(HashMap<Integer, Point> opponent_pieces) {
		HashMap<Double, Integer> locations = new HashMap<Double, Integer>();
		for (Point opp : opponent_pieces.values()){
			if (!locations.containsKey(opp.x)) locations.put(opp.x, 0);
			locations.put(opp.x, locations.get(opp.x)+1);
			if (locations.get(opp.x) == 2){
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
			if(Board.check_collision(opponent_pieces, move)){
				return new Pair<Integer, Point>(piece_id,curr_position);
			}
		}
		return null; 
	}

	private Pair<Integer, Point> fan_out(Point curr_position, double theta_start, double theta_end, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer player) {
		Pair<Integer, Point> move = new Pair<Integer, Point>(player,curr_position);
		do {
			Point new_position = new Point(curr_position);
			double delta_x = diameter_piece * Math.cos(theta_start);
			double delta_y = diameter_piece * Math.sin(theta_start);

			new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
			new_position.y += delta_y;

			move = new Pair<Integer, Point>(player, new_position);
			
			theta_start = theta_start > 0 ? -theta_start : -theta_start + 0.01; 
			
			if (valid(player_pieces, opponent_pieces, move)){
				return move; 
			}
		}
		while(Math.abs(theta_start) < theta_end);	
		return null; 
	}

	private Pair<Integer, Point> mid_fan_out(Point curr_position, double theta_start, double theta_end, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer player) {
		Pair<Integer, Point> move = new Pair<Integer, Point>(player,curr_position);
		do {
			Point new_position = new Point(curr_position);
			double delta_x = diameter_piece * Math.cos(theta_start);
			double delta_y = diameter_piece * Math.sin(theta_start);


			new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
			new_position.y += delta_y;

			move = new Pair<Integer, Point>(player, new_position);
			
			theta_start = theta_start > 0 ? -theta_start : -theta_start + 0.01; 
			
			if (mid_check_validity(move, player_pieces, opponent_pieces)){
				return move; 
			}
		}
		while(Math.abs(theta_start) < theta_end);	
		return null; 
	}

	private List<Pair<Integer, Point>> main_strat(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
		//Collections.shuffle(player_list);
		Collections.sort(player_list, new Comparator<Integer>(){
			@Override
			public int compare(Integer pt1, Integer pt2) {
				// depends on player
				return (int)(lookForward(player_pieces, opponent_pieces, pt1, isplayer1)-lookForward(player_pieces, opponent_pieces, pt2, isplayer1));
			}
		});
		possible_moves = getPossibleMoves(player_pieces, opponent_pieces, isplayer1, player_list);
		if (possible_moves.size() < num_moves) {
			possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list, possible_moves);
		}
		return possible_moves.subList(0, num_moves);
	}

	private List<Pair<Integer, Point>> runner_strat(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();

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
				Pair<Integer, Point> move = fan_out(curr_position, theta, Math.PI/2,player_pieces, opponent_pieces, player);
				curr_position = move.getValue();
				possible_moves.add(move);
			}
			return(possible_moves);
				
		}
		//remove the runner from player_list
		final Pair<Integer, Point> run = runner;
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
		player_list.remove(run.getKey());
		//Collections.shuffle(player_list);		
		Collections.sort(player_list, new Comparator<Integer>(){
			@Override
			public int compare(Integer pt1, Integer pt2) {
				// depends on player
				return (int)(lookForward(player_pieces, opponent_pieces, pt1, isplayer1)-lookForward(player_pieces, opponent_pieces, pt2, isplayer1));
			}
		});
		
		possible_moves = getPossibleMoves(player_pieces, opponent_pieces, isplayer1, player_list);	
		if (possible_moves.size() < num_moves || stuck_pieces.size() > 0) {
			possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list, possible_moves);
		}
		System.out.println(stuck_pieces.size());
		return possible_moves.subList(0, num_moves);
	}


	private List<Pair<Integer, Point>> block_strat(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		//send runner to wall_location
		Point runner_position = player_pieces.get(runner.getKey()); 
		// TODO identify points of entry to see which of our points is the closest
		List<Pair<Integer, Point>> possible_moves = new ArrayList<>();
		boolean at_wall = isplayer1 ? runner_position.x < wall_location : runner_position.x > wall_location;
		if(!at_wall){
			double theta = 0;
			Integer player = runner.getKey(); 
			for (int i = 0; i < num_moves; i++){
				Pair<Integer, Point> move = fan_out(runner_position, theta,Math.PI/2, player_pieces, opponent_pieces, player);
				runner_position = move.getValue();
				possible_moves.add(move);
			}
			return(possible_moves);	
		}

		// this move should be sequential (runner, then whatever is behind it)
	
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
		player_list.remove(runner.getKey());
		double theta_start = Math.PI;
		Pair<Integer, Point> move = new Pair(runner.getKey(), runner_position);
		do{
			Point new_position = new Point(runner_position);
			double delta_x = diameter_piece * Math.cos(theta_start);
			double delta_y = diameter_piece * Math.sin(theta_start);

			new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
			new_position.y += delta_y;

			move = new Pair<Integer, Point>(runner.getKey(), new_position);
			double diff = theta_start-Math.PI;
			theta_start = diff > 0 ? Math.PI-diff-0.01 : Math.PI-diff+0.01; 
			
			if (Board.check_collision(player_pieces, move)){
				player_list.add(runner.getKey());
				double closest_dist = Double.MAX_VALUE; 
				int closest_player = -1; 
				for (int option : player_pieces.keySet()){
					if(option == runner.getKey()) continue; 
					Point option_loc = player_pieces.get(option); 
					// check after wall points
					double dist = Math.sqrt(Math.pow(option_loc.x - runner_position.x,2) + Math.pow(option_loc.y - runner_position.y,2)); 
					
					if (dist < closest_dist){
						closest_dist = dist; 
						closest_player = option; 
					}
				}
				Pair<Integer, Point> runner_move = fan_out(runner_position, 0, Math.PI/2, player_pieces, opponent_pieces, runner.getKey());
				Pair<Integer, Point> backup_move = mid_fan_out(player_pieces.get(closest_player), 0, Math.PI/2, player_pieces, opponent_pieces, closest_player);
				// TODO: check far enough
				// blocker vs runner
				possible_moves.add(runner_move);
				possible_moves.add(backup_move);
				return possible_moves;
				// we need to figure out what piece it is blocking and move it 
			}
		}
		while(Math.abs(theta_start) > Math.PI/2);
		
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
			possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list, possible_moves);
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
			Pair<Integer, Point> move = fan_out(curr_position, theta,Math.PI/2, player_pieces, opponent_pieces, player);
			if(move!=null){
				possible_moves.add(move);
			}
			
		 }
		if(possible_moves.size()==1){
			for (Pair<Integer, Point> possible_move : possible_moves){
				if (farEnough(player_pieces, opponent_pieces, possible_move.getKey())) {
					continue; 
				}
				double theta = 0;
				Pair<Integer, Point> move = fan_out(possible_move.getValue(), theta,Math.PI/2, player_pieces, opponent_pieces, possible_move.getKey());
				if(move!=null){
					possible_moves.add(move);
					return possible_moves;
				}
			}
		}
		return possible_moves;
	}
	private List<Pair<Integer, Point>> getUnStuck( HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, List<Integer> player_list, List<Pair<Integer, Point>> possible_moves){		
		possible_moves = new ArrayList<>();
		for (Map.Entry<Integer, Point> entry : player_pieces.entrySet()){
			boolean over_line = isplayer1 ? entry.getValue().x < -21 : entry.getValue().x > 21;
			if (!over_line && !stuck_pieces.containsKey(entry.getKey())){
				//Double x_value = new Double(entry.getValue().x);
				stuck_pieces.put(entry.getKey(), false);
			}
		}
		
		
		for (Integer stuck_piece : stuck_pieces.keySet()){
			Pair<Integer, Point> look_up = find_hole(opponent_pieces, player_pieces, stuck_piece, true);
			Pair<Integer, Point> look_down = find_hole(opponent_pieces, player_pieces, stuck_piece, false);
			if (look_up != null) potential_holes.add(look_up.getValue());
			if (look_down != null) potential_holes.add(look_down.getValue());
		}
		if(potential_holes.size() > 0){
			for (Point potential_hole : potential_holes){
				double closest_dist = Double.MAX_VALUE; 
				//int closest_player = -1;
				for (int closest_player : stuck_pieces.keySet()){
					// Point place = player_pieces.get(option); 
					// double dist = Math.sqrt(Math.pow(place.x - potential_hole.x,2) + Math.pow(place.y - potential_hole.y,2)); 
					// if (dist < closest_dist){
					// 	closest_dist = dist; 
					// 	closest_player = option; 
					// }
				
				boolean moved_backwards = stuck_pieces.get(closest_player);
				if(!moved_backwards){
					Point curr_position = new Point(player_pieces.get(closest_player));
					for(int i = 0; i < 2; i++){
						Point new_position = new Point(curr_position);
						new_position.x = isplayer1 ? new_position.x + diameter_piece : new_position.x - diameter_piece;
						Pair<Integer, Point> move = new Pair<Integer, Point>(closest_player, new_position);		
						curr_position = move.getValue();			
						if(valid(player_pieces, opponent_pieces, move)){
							possible_moves.add(move);
						}
					}
				}
				else{
					// check if made it vaguely to hole -- if(player_pieces.get(closest_player).y)
					Point curr_position = new Point(player_pieces.get(closest_player));; 
					for (int i = 0; i < 2; i++){
						Point new_position = new Point(curr_position);
						// TODO : needs to go towards hole, not arbitrarily up / down 
						new_position.y = isplayer1 ? new_position.y + diameter_piece : new_position.y - diameter_piece;
						Pair<Integer, Point> move = new Pair<Integer, Point>(closest_player, new_position);
						if(valid(player_pieces, opponent_pieces, move)){
							possible_moves.add(move);
						}
						curr_position = move.getValue();
					}
					
				}
				if(possible_moves.size()>=2){
					stuck_pieces.put(closest_player, true);
					//TODO:actually check distance, once you send towards hole 
					// double dist = Math.sqrt(Math.pow(player_pieces.get(closest_player).y - potential_hole.y,2)); 
					// System.out.println(dist);
					return possible_moves;
				}
				}
			}
		}
		else{
			// if we can't find a hole 
			System.out.println("we can't find a hole");
		}
		return possible_moves;
	}


	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		
		if (player_pieces.size() < 11) {
			return main_strat(num_moves, player_pieces, opponent_pieces, isplayer1);
		}
		else{ //send a runner:
			if(check_wall(opponent_pieces)){
				// if (!initialized) {
				// 	for (int i = 0; i < player_pieces.size(); i++){
				// 		loc_to_id.put(opponent_pieces.get(i), -i);
				// 		loc_to_id.put(player_pieces.get(i), i);
						
				// 	}
				// 	loc_to_id.put(opponent_pieces.get(0), -opponent_pieces.size());
				// 	initialized = true; 
				// }
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

	public boolean mid_check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        boolean valid = true;
        // check for collisions
        valid = valid && !mid_check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;

	}
	
	public boolean mid_check_collision(HashMap<Integer, Point> m, Pair<Integer, Point> move)
    {
        for (HashMap.Entry<Integer, Point> entry : m.entrySet()) 
        {
			if (entry.getKey().equals(runner.getKey())){
				continue;
			}
            if ( Board.getdist(move.getValue(), entry.getValue()) + 1E-7 < diameter_piece)
            {
                // Double dist = getdist(move.getValue(), entry.getValue()) + eps;
                // Log.record("collision detected between pieces " + move.getKey().toString() + " and "+ entry.getKey().toString()+ "distance was "+ dist.toString());
                return true;
            }
                
        }
        return false;
	}
}

