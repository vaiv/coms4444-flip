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
	private Pair<Integer,Point> runner;
	private Pair<Integer,Point> blocker;
	private Pair<Double, Double> closest_hole = null; 
	boolean blocked = false; 
	//private List<Pair<Double, Double>> potential_holes = new ArrayList<>(); 
	


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

	/**
	 * Default strategy for < 11 pieces, moves pieces forward based on whitespace 
	 * @param num_moves
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param isplayer1
	 * @return
	 */
	private List<Pair<Integer, Point>> main_strat(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
		//Collections.shuffle(player_list);
		Collections.sort(player_list, new Comparator<Integer>(){
			@Override
			public int compare(Integer pt1, Integer pt2) {
				// depends on player
				return (int)(look_forward(player_pieces, opponent_pieces, pt1, isplayer1)-look_forward(player_pieces, opponent_pieces, pt2, isplayer1));
			}
		});
		possible_moves = getPossibleMoves(player_pieces, opponent_pieces, isplayer1, player_list, num_moves);
		if(possible_moves.size() == 1){
			Integer potential_player = possible_moves.get(0).getKey();
			Point future_pos = possible_moves.get(0).getValue();
			Pair<Integer, Point> move = fan_out(future_pos, 0, Math.PI/2, player_pieces, opponent_pieces, potential_player);//.getKey();
			if (move != null) {
				possible_moves.add(move);
			}
		}
		if (possible_moves.size() <num_moves || stuck_pieces.size() > 0) {
			possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list, possible_moves);
		}
		return possible_moves.subList(0, num_moves);
	}

	/**
	 * runner strategy, if enough pieces to build a wall, send the runner half way to get ready to block 
	 * @param num_moves
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param isplayer1
	 * @return
	 */
	private List<Pair<Integer, Point>> runner_strat(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		runner = new Pair<Integer, Point>(0, player_pieces.get(0));
		for(Integer id : player_pieces.keySet()){
			Point op = player_pieces.get(id);
			boolean closer = isplayer1 ? op.x < runner.getValue().x : op.x > runner.getValue().x;
			if(closer){
				runner = new Pair<Integer, Point>(id, op);
			}
		}
		boolean halfway = isplayer1 ? runner.getValue().x < 0 : runner.getValue().x > 0; 
		if(!halfway){
			double theta = 0;
			Point curr_position = runner.getValue(); 
			Integer player = runner.getKey(); 
			for (int i = 0; i < num_moves; i++){
				Pair<Integer, Point> move = fan_out(curr_position, theta, Math.PI/2,player_pieces, opponent_pieces, player);//.getKey();
				curr_position = move.getValue();
				possible_moves.add(move);
			}
			return(possible_moves);
		}

		// Remove the runner from player_list
		final Pair<Integer, Point> run = runner;
		List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
		player_list.remove(run.getKey());

		Collections.sort(player_list, new Comparator<Integer>(){
			@Override
			public int compare(Integer pt1, Integer pt2) {
				return (int)(look_forward(player_pieces, opponent_pieces, pt1, isplayer1)-look_forward(player_pieces, opponent_pieces, pt2, isplayer1));
			}
		});
		
		possible_moves = getPossibleMoves(player_pieces, opponent_pieces, isplayer1, player_list, num_moves);
		if(possible_moves.size() == 1){
			Integer potential_player = possible_moves.get(0).getKey();
			Point future_pos = possible_moves.get(0).getValue();
			Pair<Integer, Point> move = fan_out(future_pos, 0, Math.PI/2, player_pieces, opponent_pieces, potential_player);
			if (move != null) {
				possible_moves.add(move);
			}
		}
		if (possible_moves.size() <num_moves || stuck_pieces.size() > 0) {
			possible_moves = getUnStuck(player_pieces, opponent_pieces, isplayer1, player_list, possible_moves);
		}
		return possible_moves.subList(0, num_moves);
	}

	

	private List<Pair<Integer, Point>> scatter_inside(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1){
		List<Pair<Integer, Point>> possible_moves = new ArrayList<>(); 
		int i = 0; 
		for (int player : player_pieces.keySet()){
			if (player == runner.getKey() || player == blocker.getKey()) {
				Pair<Integer, Point> move = fan_out(player_pieces.get(player), 0, Math.PI/2, player_pieces, opponent_pieces, player);
				possible_moves.add(move);
				if(possible_moves.size() >= num_moves) break ;
			}
			boolean over_line = isplayer1 ? player_pieces.get(player).x < -20 - 1E-7 & player_pieces.get(player).x > -30: player_pieces.get(player).x > 20 + 1E-7 & player_pieces.get(player).x < 30  ;
			if (over_line){
				Pair<Integer, Point> move = fan_out(player_pieces.get(player), 0, Math.PI/2, player_pieces, opponent_pieces, player);
				possible_moves.add(move);
				if(possible_moves.size() >= num_moves) {
					break ;
				}
			}
		}
		return possible_moves.size() >= num_moves ? possible_moves : null; 
	}

	
	/**
	 * Strategy for if a wall is detected being formed 
	 * @param num_moves
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param isplayer1
	 * @return
	 */
	private List<Pair<Integer, Point>> block_strat(Integer num_moves, HashMap<Integer,Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {				
		Point runner_position = player_pieces.get(runner.getKey()); 	
		List<Pair<Integer, Point>> possible_moves = new ArrayList<>();
		boolean at_wall = isplayer1 ? runner_position.x < wall_location : runner_position.x > wall_location;
		if(at_wall) blocked = true; 
		if(!blocked && !at_wall){
			possible_moves = send_initial_runner(num_moves, player_pieces, opponent_pieces, isplayer1);
			if(possible_moves != null) return possible_moves; 
		}
		else if (!blocked){
			return main_strat(num_moves, player_pieces, opponent_pieces, isplayer1);
		}
		if(blocked){
			if(!at_wall){
				possible_moves = scatter_inside(num_moves, player_pieces, opponent_pieces, isplayer1);
				if(possible_moves != null && possible_moves.size()>=2){
					return possible_moves; 
				}
			}
			runner = new Pair(runner.getKey(), player_pieces.get(runner.getKey()));
			List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
			player_list.remove(runner.getKey());
			double closest_x = isplayer1 ? 60 : -60; 
			int closest_player = -1; 		
			for (int player : player_list){
				Point position = player_pieces.get(player);
				boolean closest = isplayer1 ? position.x < closest_x && position.x > runner_position.x : position.x > closest_x && position.x < runner_position.x; 
				if(closest){
					closest_x = position.x; 
					closest_player = player; 
				}
			}
			blocker = new Pair<>(closest_player, player_pieces.get(closest_player));	
			possible_moves = trade_blocker(num_moves, player_pieces, opponent_pieces, player_list);
			if(possible_moves != null && possible_moves.size() >= num_moves) {
				return possible_moves;
			}
			if(possible_moves.size() == 1){
				Integer potential_player = possible_moves.get(0).getKey();
				Point future_pos = possible_moves.get(0).getValue();
				Pair<Integer, Point> move = fan_out(future_pos, 0, Math.PI/2, player_pieces, opponent_pieces, potential_player);//.getKey();
				if (move != null) {
					possible_moves.add(move);
				}
			}
			
		}
		if (possible_moves == null || possible_moves.size() < num_moves){
			List<Integer> player_list = new ArrayList<>(player_pieces.keySet());
			Collections.shuffle(player_list);
			Collections.sort(player_list, new Comparator<Integer>(){
				@Override
				public int compare(Integer pt1, Integer pt2) {
					// depends on player	
					return (int)(look_forward(player_pieces, opponent_pieces, pt1, isplayer1)-look_forward(player_pieces, opponent_pieces, pt2, isplayer1));
				}
			});
			possible_moves = getPossibleMoves(player_pieces, opponent_pieces, isplayer1, player_list, num_moves);
			blocked = false; 
		}
		return possible_moves.subList(0, num_moves);	 
		
	}

	/**
	 * Method called each turn to determine strategy 
	 * @param num_moves
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param isplayer1
	 * @return
	 */
	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		if (player_pieces.size() < 11) {
			return main_strat(num_moves, player_pieces, opponent_pieces, isplayer1);
		}
		else{
			if(check_wall(opponent_pieces)){
				return block_strat(num_moves, player_pieces, opponent_pieces, isplayer1);
			} else {
				return runner_strat(num_moves, player_pieces, opponent_pieces, isplayer1);
			}
		}
	}


	/* UTILITY FUNCTIONS */

	/**
	 * Default check validity 
	 * @param move
	 * @param player_pieces
	 * @param opponent_pieces
	 * @return
	 */
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

	/**
	 * Collision check specific to finding a move mid turn 
	 * @param m
	 * @param move
	 * @return
	 */
	public boolean mid_check_collision(HashMap<Integer, Point> m, Pair<Integer, Point> move)
    {
        for (HashMap.Entry<Integer, Point> entry : m.entrySet()) 
        {
			if (entry.getKey().equals(runner.getKey())){
				continue;
			}
            if ( Board.getdist(move.getValue(), entry.getValue()) + 1E-7 < diameter_piece)
            {
                return true;
            }
        }
        return false;
	}

	/**
	 * Validity check specific to finding a move mid turn 
	 * @param move
	 * @param player_pieces
	 * @param opponent_pieces
	 * @return
	 */
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

	/**
	 * Validity check which determines only if a location is free from blockage 
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param move
	 * @return
	 */
	private boolean valid_whitespace(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Pair<Integer, Point> move) {
		boolean valid = true;
	  
		// check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
		return valid;
	}
	
	/**
	 * Determines if piece should continue moving accross opponents line 
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param piece_id
	 * @return
	 */
	private Boolean far_enough(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece_id){
		Point curr_position = player_pieces.get(piece_id);
		Double cutoff = (Math.ceil(player_pieces.size() / 10)*diameter_piece) + 21; 
		boolean over_edge = isplayer1 ?  curr_position.x < -cutoff : curr_position.x > cutoff ;
		if (over_edge){
			return true;
		}
		return false; 
	}

	/**
	 * Looks at the whitespace needed to move forward - heuristic for WHICH coin to move
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param piece_id
	 * @param isplayer1
	 * @return
	 */
	public double look_forward(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece_id, boolean isplayer1){
		Point curr_position = new Point(player_pieces.get(piece_id));
		curr_position.x += isplayer1 ? -diameter_piece : diameter_piece;
		Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, curr_position);
		while(valid_whitespace(player_pieces, opponent_pieces, move)){
			curr_position.x += isplayer1 ? - diameter_piece : diameter_piece;
			move = new Pair<Integer, Point>(piece_id,curr_position);
		}
		return Math.abs((curr_position.x-player_pieces.get(piece_id).x))/diameter_piece; 
	}

	/** Detect when a wall is being formed 
	 * @param opponent_pieces
	 * @return
	 */
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

	/**
	 * Utility method to get a point from an angle 
	 * @param current_point
	 * @param theta
	 * @return
	 */
	private Point getPointFromAngle(Point current_point, double theta) {
		Point new_point = new Point(current_point);
		double delta_x = diameter_piece * Math.cos(theta);
		double delta_y = diameter_piece * Math.sin(theta);
		new_point.x = isplayer1 ? new_point.x - delta_x : new_point.x + delta_x;
		new_point.y += delta_y;
		return new_point;
	}

	/**
	 * Utility method to find the move with the smallest theta 
	 * @param curr_position
	 * @param theta_start
	 * @param theta_end
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param player
	 * @return
	 */ 
	private Pair<Integer, Point> fan_out(Point curr_position, double theta_start, double theta_end, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer player) {
		do {
			Point new_position = getPointFromAngle(curr_position, theta_start);
			theta_start = theta_start > 0 ? -theta_start : -theta_start + 0.01; 
			Pair<Integer, Point> move = new Pair<Integer, Point>(player, new_position);
			// check valid_white
			if (valid_whitespace(player_pieces, opponent_pieces, move)){
				return move; //new Pair<Pair<Integer, Point>, Double>(move, theta_start); 
			}
		}
		while(Math.abs(theta_start) < theta_end);	
		return null;
	}

	/**
	 * Utility function specific to checking possible moves in the middle of a turn 
	 * @param curr_position
	 * @param theta_start
	 * @param theta_end
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param player
	 * @return
	 */
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


	/* HELPER FUNCTIONS */

	/**
	 * General helper method to greedily look for moves for each player, returns one move per player 
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param isplayer1
	 * @param player_list
	 * @param num_moves
	 * @return
	 */
	private List<Pair<Integer, Point>> getPossibleMoves(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, List<Integer> player_list, Integer num_moves){
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();
		for(int player : player_list){
			if (far_enough(player_pieces, opponent_pieces, player)) {
				continue; 
			}
			double theta_start = 0; 
			double theta_end =  Math.PI/2;			
			Point curr_position = player_pieces.get(player);
			Pair<Integer, Point> move = fan_out(curr_position, theta_start, theta_end, player_pieces, opponent_pieces, player);
			if (move != null) {
				possible_moves.add(move);
			}
		}
		return possible_moves;
	}

	

	/**
	 * Method for pieces when they have run out of other moves 
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param isplayer1
	 * @param player_list
	 * @param possible_moves
	 * @return
	 */
	private List<Pair<Integer, Point>> getUnStuck( HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, List<Integer> player_list, List<Pair<Integer, Point>> possible_moves){		
		System.out.println("get unstuck");
		possible_moves = new ArrayList<>();
		List<Pair<Double, Double>> potential_holes = new ArrayList<>();
		for (Map.Entry<Integer, Point> entry : player_pieces.entrySet()){
			boolean over_line = isplayer1 ? entry.getValue().x < -20 - 1E-7  : entry.getValue().x > 20 + 1E-7 ;
			if (!over_line && !stuck_pieces.containsKey(entry.getKey())){
				stuck_pieces.put(entry.getKey(), false);
			}
		}	
		
		for (Integer stuck_piece : stuck_pieces.keySet()){
			potential_holes.addAll(find_hole(opponent_pieces, player_pieces, player_pieces.get(stuck_piece)));
		}
		if(potential_holes.size() > 0){
			double closest_dist = Double.MAX_VALUE;
			Pair<Double, Double> closest_hole = new Pair<>(22.0, 22.0);
			Integer closest_player = -1;
			HashMap<Integer, Double> side_of_hole = new HashMap<>();
			// Identify a stuck point that has the minimal distance to a hole
			for (Pair<Double, Double> potential_hole : potential_holes){
				for (int stuck_player : stuck_pieces.keySet()){
					Point stuck_loc = player_pieces.get(stuck_player); 
					double dist = 22.0;
					if(stuck_loc.y > potential_hole.getKey()){
						dist = Math.sqrt(/*Math.pow(stuck_loc.x - potential_hole.,2) + */Math.pow(stuck_loc.y - potential_hole.getKey(),2)); 
						side_of_hole.put(stuck_player, potential_hole.getKey());
					}
					else if(stuck_loc.y < potential_hole.getValue()){
						dist = Math.sqrt(/*Math.pow(stuck_loc.x - potential_hole.,2) + */Math.pow(stuck_loc.y - potential_hole.getValue(),2)); 
						side_of_hole.put(stuck_player, potential_hole.getValue());
					}
					else{
						dist = Math.sqrt(Math.pow(stuck_loc.y - potential_hole.getKey(),2));
						side_of_hole.put(stuck_player, potential_hole.getKey());
					}
					if (dist < closest_dist){
					 	closest_dist = dist; 
						closest_player = stuck_player; 
						closest_hole = potential_hole;
					}// we have now found our closest y coord to send our stuck player to
				}
			}

			boolean moved_backwards = stuck_pieces.get(closest_player);
			if(!moved_backwards){
				Point curr_position = new Point(player_pieces.get(closest_player));
				for(int i = 0; i < 2; i++){
					Point new_position = new Point(curr_position);
					new_position.x = isplayer1 ? new_position.x + diameter_piece : new_position.x - diameter_piece;
					Pair<Integer, Point> move = new Pair<Integer, Point>(closest_player, new_position);		
					curr_position = move.getValue();
					if(!Board.check_collision(opponent_pieces, move) && !Board.check_collision(player_pieces, move)){
						possible_moves.add(move);
					}
					
				}
				if(possible_moves.size() >= 2){
					stuck_pieces.put(closest_player, true);		
					return possible_moves;		
				}
			}
			else{			
				Double closest_y = side_of_hole.get(closest_player);
				boolean above = closest_y > closest_hole.getValue();
				Point curr_position = new Point(player_pieces.get(closest_player));
				for (int i = 0; i < 2; i++){
					Point new_position = new Point(curr_position);
					new_position.y = above ? new_position.y - diameter_piece : new_position.y + diameter_piece;
					Pair<Integer, Point> move = new Pair<Integer, Point>(closest_player, new_position);
					if(valid_whitespace(player_pieces, opponent_pieces, move)){
						possible_moves.add(move);
					}
					curr_position = move.getValue();
				}
				// Remove players that are at the hole  
				boolean beyond_y =  above? player_pieces.get(closest_player).y < closest_y + 1 : player_pieces.get(closest_player).y > closest_y + 1 ;
				if(beyond_y){
					stuck_pieces.remove(closest_player);
				}
				if(possible_moves.size()>=2){
					return possible_moves;
				}
			}	
		}
		else{
			// if we can't find a hole (we should use the bouncy strategy from the group we saw in class)
			System.out.println("we can't find a hole");
		}
		// TODO: this check shouldn't be if ran out of moves but should be if can move forward 
		if( possible_moves.size() < 2){
			stuck_pieces = new HashMap<>();
		}
		return null;
	}

	/**
	 * Helper method, looks for holes in walls / for stuck pieces 
	 * @param opponent_pieces
	 * @param player_pieces
	 * @param location
	 * @return
	 */
	private List<Pair<Double, Double>> find_hole(HashMap<Integer, Point> opponent_pieces,HashMap<Integer, Point> player_pieces, Point location) {		
		List<Pair<Double, Double>> potential_holes = new ArrayList<>(); 
		Integer piece_id = 0 ; 
		Point check_sweep = new Point(location.x, 20);
		Point original_sweep = new Point(check_sweep);
		Double start_hole = -22.0;
		Double end_hole = -22.0;
		boolean in_hole = false; 
 		Pair<Integer, Point> move_sweep = new Pair<Integer, Point>(piece_id, check_sweep);

		while(check_sweep.y > -20){
			check_sweep.x = original_sweep.x;
			check_sweep.y -= diameter_piece;
			check_sweep.x += isplayer1 ? -diameter_piece : diameter_piece;
			move_sweep = new Pair<Integer, Point>(piece_id, check_sweep);
			if(!in_hole){
				if(!Board.check_collision(opponent_pieces, move_sweep)){
					start_hole = move_sweep.getValue().y;
					in_hole = true; 
				}
			}
			else{
				if(!Board.check_within_bounds(move_sweep) || Board.check_collision(opponent_pieces, move_sweep)){
					end_hole = move_sweep.getValue().y;

					in_hole = false; 
					if(Math.abs(start_hole-end_hole) > 2){
						Pair<Double, Double> y_coords = new Pair<Double, Double>(start_hole, end_hole);
						if(!potential_holes.contains(y_coords)){
							potential_holes.add(y_coords);
						}
					}
				}
			}
		}
		return potential_holes;
	}	


	/**
	 * Helper method -- sends runner to block wall formation 
	 * @param num_moves
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param isplayer1
	 * @return
	 */
	private List<Pair<Integer, Point>> send_initial_runner(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1){
		List<Pair<Integer, Point>> possible_moves = new ArrayList<>();
		Integer runner_id = runner.getKey(); 
		Point runner_position = player_pieces.get(runner_id);
		Point search_pos = new Point(0,0); 
		search_pos.x = isplayer1 ? wall_location + diameter_piece : wall_location - diameter_piece;  
		List<Pair<Double, Double>> holes = find_hole(opponent_pieces, player_pieces, search_pos); 
		double closest_dist = Double.MAX_VALUE;
		closest_hole = new Pair<>(22.0, 22.0);
		Integer closest_player = -1;
		HashMap<Integer, Double> side_of_hole = new HashMap<>();
		// Identify a stuck point that has the minimal distance to a hole
		for (Pair<Double, Double> potential_hole : holes){
			Point stuck_loc = runner.getValue(); 
			Integer stuck_player = runner.getKey();
			double dist = 22.0;
			if(stuck_loc.y > potential_hole.getKey()){
				dist = Math.sqrt(/*Math.pow(stuck_loc.x - potential_hole.,2) + */Math.pow(stuck_loc.y - potential_hole.getKey(),2)); 
				side_of_hole.put(stuck_player, potential_hole.getKey());
			}
			else if(stuck_loc.y < potential_hole.getValue()){
				dist = Math.sqrt(/*Math.pow(stuck_loc.x - potential_hole.,2) + */Math.pow(stuck_loc.y - potential_hole.getValue(),2)); 
				side_of_hole.put(stuck_player, potential_hole.getValue());
			}
			else{
				dist = Math.sqrt(Math.pow(stuck_loc.y - potential_hole.getKey(),2));
				side_of_hole.put(stuck_player, potential_hole.getKey());
			}
			if (dist < closest_dist){
				closest_dist = dist; 
				closest_player = stuck_player; 
				closest_hole = potential_hole;
			}// we have now found our closest y coord to send our stuck player to	
		}
	
		Point runner_pos = runner.getValue();
		Double x_dist = wall_location - runner_pos.x;
		Double theta_start = Math.atan2(Math.abs(closest_hole.getKey()-runner_pos.y), x_dist);
		Double theta_end = Math.atan2(Math.abs(closest_hole.getValue()-runner_pos.y), x_dist);

		theta_start = isplayer1 ? theta_start+Math.PI : theta_start;
		theta_end = isplayer1 ? theta_end +Math.PI: theta_end;
		
		for (int i = 0; i < num_moves; i++){
			Pair<Integer, Point> move = fan_out(runner_position, theta_start, theta_end, player_pieces, opponent_pieces, runner_id);//.getKey();
			runner_position = move.getValue();
			possible_moves.add(move);
		}
		if(possible_moves.size()>=num_moves) {
			return(possible_moves);	
		}
		return null; 
	}

	/**
	 * Helper method -- if current runner (aka piece in the wall) has the new blocker behind it, exchange moves to continue blocking the wall 
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param player_list
	 * @return
	 */
	private List<Pair<Integer, Point>> switch_blocker(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, List<Integer> player_list){
		List<Pair<Integer, Point>> possible_moves = new ArrayList<>();
		double theta_start = Math.PI;
		Point runner_position = runner.getValue(); 
		Integer runner_id = runner.getKey(); 
		Pair<Integer, Point> move = new Pair(runner_id, runner_position);
		do{
			Point new_position = new Point(runner_position);
			double delta_x = diameter_piece * Math.cos(theta_start);
			double delta_y = diameter_piece * Math.sin(theta_start);

			new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
			new_position.y += delta_y;

			move = new Pair<Integer, Point>(runner_id, new_position);
			double diff = theta_start-Math.PI;
			theta_start = diff > 0 ? Math.PI-diff-0.01 : Math.PI-diff+0.01; 
			
			if (Board.check_collision(player_pieces, move)){
				player_list.add(runner_id);
				double closest_dist = Double.MAX_VALUE; 
				int closest_player = -1; 
				for (int option : player_pieces.keySet()){
					if(option == runner_id) continue; 
					Point option_loc = player_pieces.get(option); 
					// check after wall points
					double dist = Math.sqrt(Math.pow(option_loc.x - runner_position.x,2) + Math.pow(option_loc.y - runner_position.y,2)); 
					if (dist < closest_dist){
						closest_dist = dist; 
						closest_player = option; 
					}
				}
				// Move curr blocker out of wall, new blocker to wall 
				Pair<Integer, Point> runner_move = fan_out(runner_position, 0, Math.PI/2, player_pieces, opponent_pieces, runner.getKey());//.getKey();
				Pair<Integer, Point> backup_move = mid_fan_out(player_pieces.get(closest_player), 0, Math.PI/2, player_pieces, opponent_pieces, closest_player);
				possible_moves.add(runner_move);
				possible_moves.add(backup_move);
				return possible_moves;
			}
		}
		while(Math.abs(theta_start) > Math.PI/2);
		return null; 
	}


	/**
	 * Helper method -- moves closest piece to runner in order to replace it (calls switch blocker when out of moves)
	 * @param num_moves
	 * @param player_pieces
	 * @param opponent_pieces
	 * @param player_list
	 * @return
	 */
	private List<Pair<Integer, Point>> trade_blocker(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, List<Integer> player_list){
		
		List<Pair<Integer, Point>> possible_moves = new ArrayList<>(); 
		Point blocker_pos = player_pieces.get(blocker.getKey());
		Point runner_position = runner.getValue(); 		
		
		Double x_dist = runner_position.x - blocker_pos.x+1;
		Double runner_top = runner_position.y; 
		Double runner_bottom = runner_position.y; 
		Double theta_start = Math.atan2(Math.abs(runner_top-blocker_pos.y), x_dist);
		Double theta_end = Math.atan2(Math.abs(runner_bottom-blocker_pos.y), x_dist);

		theta_start = isplayer1 ? theta_start+Math.PI : theta_start;
		theta_end = isplayer1 ? theta_end +Math.PI: theta_end;
		for (int i = 0; i < num_moves; i++){
			Pair<Integer, Point> move = fan_out(blocker_pos, theta_start, theta_end, player_pieces, opponent_pieces, blocker.getKey());
			if(move !=null){
				blocker_pos = move.getValue();
				possible_moves.add(move);
			}		
		}
		if(possible_moves.size() < num_moves){
			List<Pair<Integer, Point>> trade_moves = switch_blocker(player_pieces, opponent_pieces, player_list);
			if (trade_moves!=null){
				player_list.add(runner.getKey());
				runner = blocker; 
				player_list.remove(runner.getKey());
				return trade_moves;
			}
		}
		return possible_moves;	
		
	
	}
}

