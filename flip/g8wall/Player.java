package flip.g8wall;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.HashSet;

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

	// Stage variable: if the wall has been initialized
	private boolean wall_init = false;
	// Stage variable: if the wall has finished
	private boolean wall_complete = false;
	// points that's used to build a wall
	private HashMap<Integer, Point> wall_point = new HashMap<Integer, Point>();
	// the last layer of the wall
	private HashMap<Integer, Point> back_wall_point = new HashMap<Integer, Point>();
	// points that's used to attach behind a wall point
	private HashMap<Integer, Integer> attack_points = new HashMap<Integer, Integer>();
	// points beyond the wall
	private HashMap<Integer, Point> runner_points = new HashMap<Integer, Point>();
	// indicates if a coin should be not be moved, like coins forming a wall
	private HashSet<Integer> ignored_piece = new HashSet<Integer>();
	// temporary variable to store current destination of each piece
	private ArrayList<Pair<Integer, Point>> piece_to_dest = new ArrayList<Pair<Integer, Point>>();

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
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
		
		int greedy_num_trial = 100;
		double dist_inner = 20.0 + ((double)n)/30.0;
		int num_trials = 40;
		int i = 0;
		
		// sort by x
		List<Map.Entry<Integer,Point>> pointsByX = new ArrayList<Entry<Integer, Point>>(player_pieces.entrySet());
		pointsByX.sort((Map.Entry<Integer, Point> p1, Map.Entry<Integer, Point> p2) -> (p1.getValue().x > p2.getValue().x) ? 1 : -1);

		//if n < 30, use greedy
		if(n <= 30) {
			 for(int index = n-1; index >= 0 && moves.size()!=num_moves; index--) {
				 int piece_id = pointsByX.get(index).getKey();
				 Point curr_position = pointsByX.get(index).getValue();
				 if((isplayer1 && curr_position.x<-dist_inner) || (!isplayer1 && curr_position.x>dist_inner))
					 continue;
				 Point new_position = new Point(curr_position);
				 new_position.x = isplayer1 ? new_position.x - diameter_piece : new_position.x + diameter_piece;
				 
				 Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
				 if(check_validity(move, player_pieces, opponent_pieces)) {
					 moves.add(move);
					 if((isplayer1 && curr_position.x<-dist_inner) || (!isplayer1 && curr_position.x>dist_inner)) {
						 continue;
					 }
					 double x = isplayer1 ? new_position.x - diameter_piece : new_position.x + diameter_piece;
					 move = new Pair<Integer, Point>(piece_id, new Point(x, new_position.y));
					 if(moves.size()!=num_moves && check_validity_(move, player_pieces, opponent_pieces)) {
						 moves.add(move);
					 }
				 }
			 }
			 
			 i = 0;
			 while(moves.size()!=num_moves && i<greedy_num_trial)
			 {
			 	Integer piece_id = random.nextInt(n);
			 	Point curr_position = player_pieces.get(piece_id);
			 	Point new_position = new Point(curr_position);

			 	double theta = -Math.PI/2 + Math.PI * random.nextDouble();
			 	double delta_x = diameter_piece * Math.cos(theta);
			 	double delta_y = diameter_piece * Math.sin(theta);
			 	new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
			 	new_position.y += delta_y;
			 	Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
			 	if(check_validity(move, player_pieces, opponent_pieces)) {
			 		moves.add(move);
			 		if((isplayer1 && curr_position.x<-dist_inner) || (!isplayer1 && curr_position.x>dist_inner)) {
						 continue;
					 }
					 double x = isplayer1 ? new_position.x - diameter_piece : new_position.x + diameter_piece;
					 move = new Pair<Integer, Point>(piece_id, new Point(x, new_position.y));
					 if(moves.size()!=num_moves && check_validity_(move, player_pieces, opponent_pieces)) {
						 moves.add(move);
					 }
			 	}
			 		
			 	i++;
			 }
			 i = 0;
			 while(moves.size()!=num_moves && i<greedy_num_trial)
			 {
			 	Integer piece_id = random.nextInt(n);
			 	Point curr_position = player_pieces.get(piece_id);
			 	if((isplayer1 && curr_position.x<-dist_inner) || (!isplayer1 && curr_position.x>dist_inner)) {
					 continue;
				 }
			 	Point new_position = new Point(curr_position);

			 	double theta = -Math.PI/2 + Math.PI * random.nextDouble();
			 	double delta_x = diameter_piece * Math.cos(theta);
			 	double delta_y = diameter_piece * Math.sin(theta);
			 	new_position.x = isplayer1 ? new_position.x + delta_x : new_position.x - delta_x;
			 	new_position.y -= delta_y;
			 	Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
			 	if(check_validity(move, player_pieces, opponent_pieces)) {
			 		moves.add(move);
			 		if((isplayer1 && curr_position.x<-dist_inner) || (!isplayer1 && curr_position.x>dist_inner)) {
						 continue;
					 }
					 double x = isplayer1 ? new_position.x - diameter_piece : new_position.x + diameter_piece;
					 move = new Pair<Integer, Point>(piece_id, new Point(x, new_position.y));
					 if(moves.size()!=num_moves && check_validity_(move, player_pieces, opponent_pieces)) {
						 moves.add(move);
					 }
			 	}
			 		
			 	i++;
			 }
			 
			 for(int piece_id = 0; piece_id < n && moves.size()!=num_moves; piece_id++) {
				 
				 Point curr_position = player_pieces.get(piece_id);
				 Point new_position = new Point(curr_position);
				 new_position.x = isplayer1 ? new_position.x - diameter_piece : new_position.x + diameter_piece;
				 
				 Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
				 if(check_validity(move, player_pieces, opponent_pieces))
				 	moves.add(move);
			 }
			 
			 return moves;
		}
		// if n >= 30 use wall
		else {
			// if has runner, it goes first
			if(runner_points.size() != 0) {
				// try straight forward
				for(int index = n-1; index >= 0 && moves.size()!=num_moves; index--) {
					 int runner_id = pointsByX.get(index).getKey();
					 if(!runner_points.containsKey(runner_id))
						 continue;
					 Point curr_position = pointsByX.get(index).getValue();
					 if((isplayer1 && curr_position.x<-dist_inner) || (!isplayer1 && curr_position.x>dist_inner)) {
						 runner_points.remove(runner_id);
						 continue;
					 }
					 if((isplayer1 && curr_position.x<-dist_inner) || (!isplayer1 && curr_position.x>dist_inner))
						 continue;
					 Point new_position = new Point(curr_position);
					 new_position.x = isplayer1 ? new_position.x - diameter_piece : new_position.x + diameter_piece;
					 
					 Pair<Integer, Point> move = new Pair<Integer, Point>(runner_id, new_position);
					 if(check_validity(move, player_pieces, opponent_pieces)) {
						 moves.add(move);
						 
						 double x = isplayer1 ? new_position.x - diameter_piece : new_position.x + diameter_piece;
						 move = new Pair<Integer, Point>(runner_id, new Point(x, new_position.y));
						 if(moves.size()!=num_moves && check_validity_(move, player_pieces, opponent_pieces)) {
							 moves.add(move);
						 }
					 }
				}
				// try different angle 30 times
				int j = 0;
				while(moves.size() != num_moves && j < num_trials) {
					Integer[] keys = runner_points.keySet().toArray(new Integer[0]);
					Integer piece_id = keys[random.nextInt(keys.length)];
				 	Point curr_position = player_pieces.get(piece_id);
				 	if((isplayer1 && curr_position.x<-dist_inner) || (!isplayer1 && curr_position.x>dist_inner)) {
						 runner_points.remove(piece_id);
						 continue;
					 }
				 	Point new_position = new Point(curr_position);

				 	double theta = -Math.PI/2 + Math.PI * random.nextDouble();
				 	double delta_x = diameter_piece * Math.cos(theta);
				 	double delta_y = diameter_piece * Math.sin(theta);
				 	new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
				 	new_position.y += delta_y;
				 	Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
				 	if(check_validity(move, player_pieces, opponent_pieces))
				 		moves.add(move);
				 	j++;
				}
			}

			// Set current wall building objective
			if (!wall_init) {
				ArrayList<Point> wall = find_wall_points(isplayer1 ? 21 : -21, 1.45, 1.75); // positions for wall
				for (Point point : wall) {
					Integer id = closest(point, player_pieces, wall_point);
					piece_to_dest.add(new Pair<Integer, Point>(id, point));
					wall_point.put(id, point);
					//System.out.println("point" + point.x + "," + point.y);
				}
				wall_init = true;
			}

			// Adaptive wall building
			Point enemy_runner_pos = opponent_pieces.get(find_runner(opponent_pieces, isplayer1));
			piece_to_dest.sort(
				(Pair<Integer, Point> p1, Pair<Integer, Point> p2) -> 
				distance(enemy_runner_pos, p1.getValue()) > distance(enemy_runner_pos, p2.getValue()) ? 1 : -1
			);

			for (Pair<Integer, Point> pair : piece_to_dest) {
				if (!wall_complete && ignored_piece.size() == wall_point.size()) {
					wall_complete = true;
					back_wall_point = new HashMap<Integer, Point>(wall_point);
				}
				if (wall_complete) break;

				Integer id = pair.getKey();
				Point dest = pair.getValue(); 

				// mark the coins already in wall position, skip those already in position
				if (ignored_piece.contains(id)) continue;
				if (Board.almostEqual(distance(player_pieces.get(id), dest), 0.0)) {
					ignored_piece.add(id);
					continue;
				}

				if (moves.size() >= num_moves || i >= num_trials) 
					break;
				
				Point temp = find_move_exact(player_pieces.get(id), dest, 1); // find the next move
				Pair<Integer, Point> move = new Pair<Integer, Point>(id, temp);
				if(check_validity(move, player_pieces, opponent_pieces))
					moves.add(move);
				else { // Try move the coin in other direction
					temp = find_move_exact(player_pieces.get(id), dest, -1);
					move = new Pair<Integer, Point>(id, temp);
					if (check_validity(move, player_pieces, opponent_pieces)) 
						moves.add(move);
					else { // try random moves
						temp = player_pieces.get(id);
						temp.x += isplayer1 ? -diameter_piece : diameter_piece;
						move = new Pair<Integer, Point>(id, temp);
						if (check_validity(move, player_pieces, opponent_pieces))
							moves.add(move);
					}
				}
				++i;
			}

			if (!wall_complete && ignored_piece.size() == wall_point.size()) {
				wall_complete = true;
				back_wall_point = new HashMap<Integer, Point>(wall_point);
			}

			// start attacking
			if (wall_complete || i >= num_trials){
				// try to send runners
				if(attack_points.size() != 0 && num_moves - moves.size() == 2) {
					for(int attack_index: attack_points.keySet()) {
						int wall_index = attack_points.get(attack_index);
						Point temp = new Point(player_pieces.get(wall_index));
						double x = isplayer1 ? temp.x-diameter_piece : temp.x+diameter_piece;
						Pair<Integer, Point> move = new Pair<Integer, Point>(wall_index, new Point(x, temp.y));
						if(check_validity(move, player_pieces, opponent_pieces)) {
							moves.add(move);
							runner_points.put(wall_index, temp);
							wall_point.remove(wall_index);
							move = new Pair<Integer, Point>(attack_index, temp);
							moves.add(move);
							wall_point.put(attack_index, temp);
							for(int p: player_pieces.keySet()) {
								Point pp = player_pieces.get(p);
								if(Math.abs(pp.y - temp.y) < 1E-7 && ((isplayer1 && pp.x > temp.x) || (!isplayer1 && pp.x < temp.x))) {
									attack_points.remove(p);
									ignored_piece.remove(p);
									back_wall_point.remove(p);
								}
							}
							attack_points.remove(attack_index);
							ignored_piece.add(attack_index);
							back_wall_point.put(attack_index, temp);
							return moves;
						}
					}
				}
				// try to send attackers
				if(moves.size() < num_moves) {
					if(!isplayer1)
						Collections.reverse(pointsByX);
					
				    for(Map.Entry<Integer, Point> p: pointsByX) {
				    		if(ignored_piece.contains(p.getKey()))
				    			continue;
				    		Integer wall_index = closestY(p.getValue(), back_wall_point, (HashSet<Integer>)null); 
						Point attack_pos = new Point(back_wall_point.get(wall_index));
						attack_pos.x -= isplayer1 ? -diameter_piece : diameter_piece;
						while(moves.size() < num_moves) {
							Point temp = find_move_exact(p.getValue(), attack_pos, 1);
		
							Pair<Integer, Point> move = new Pair<Integer, Point>(p.getKey(), temp);
							if(check_validity(move, player_pieces, opponent_pieces)) {
								moves.add(move);
								if (Board.almostEqual(distance(temp, attack_pos), 0.0)) {
									ignored_piece.add(p.getKey());
									attack_points.put(p.getKey(), wall_index);
									back_wall_point.remove(wall_index);
									back_wall_point.put(p.getKey(), attack_pos);
									continue;
								}
								if(moves.size() < num_moves) {
									temp = find_move_exact(temp, attack_pos, 1);
									move = new Pair<Integer, Point>(p.getKey(), temp);
									moves.add(move);
									if (Board.almostEqual(distance(temp, attack_pos), 0.0)) {
										ignored_piece.add(p.getKey());
										attack_points.put(p.getKey(), wall_index);
										back_wall_point.remove(wall_index);
										back_wall_point.put(p.getKey(), attack_pos);
										continue;
									}
								}
								
							}
							else {
								temp = find_move_exact(temp, attack_pos, -1);
								move = new Pair<Integer, Point>(p.getKey(), temp);
								if (check_validity(move, player_pieces, opponent_pieces)) {
									moves.add(move);
									if (Board.almostEqual(distance(temp, attack_pos), 0.0)) {
										ignored_piece.add(p.getKey());
										attack_points.put(p.getKey(), wall_index);
										back_wall_point.remove(wall_index);
										back_wall_point.put(p.getKey(), attack_pos);
										continue;
									}
								}
									
							}
						}
				    }
			    }
			}
			return moves;
		}
	}

	// Find the index of the coin with largest/smallest x-axis (for player1 is largest)
	public Integer find_runner(HashMap<Integer, Point> pieces, boolean isplayer1) {
		Integer index = 0;
		double x_axis = pieces.get(index).x;
		for (int i : pieces.keySet()) {
			if (isplayer1) {
				if (pieces.get(i).x > x_axis) {
					x_axis = pieces.get(i).x;
					index = i;
				}
			}
			else {
				if (pieces.get(i).x < x_axis) {
					x_axis = pieces.get(i).x;
					index = i;
				}
			}
		}
		return index;
	}

	// Find the distance between two points
	public double distance(Point x, Point y) {
		double y_diff = x.y - y.y;
		double x_diff = x.x - y.x;
		double dist = Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
		return dist;
	}

	// Find a valid move that moves a coin exactly to the destination
	// when there are two possible moves, set choice to -1 will give different result, choice default to 1 
	// TODO: more advanced path finding algorithm, avoid obstacles, refractor
	// WARNING: this function one only works when start and dest are both on the left or right side
	public Point find_move_exact(Point start, Point dest, int choice) {
		if (isplayer1) { // Very questionable logic here
			start = new Point(start);
			dest = new Point(dest);
			start.x *= -1;
			dest.x *= -1;
		}
		Point result;
		double length = this.diameter_piece;
		double y_diff = dest.y - start.y;
		double x_diff = dest.x - start.x;
		double dist = Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
		if (Board.almostEqual(dist, length)) {
			result = new Point(dest);
		}
		else if (dist <= 2 * length) { 
			double newX = start.x;
			double newY = start.y;
			double vector1_x, vector1_y, vector2_x, vector2_y;
			vector1_x = 0.5 * (dest.x - start.x);
			vector1_y = 0.5 * (dest.y - start.y);
			double vector1_length = dist / 2;
			double vector2_length = Math.sqrt(Math.pow(length, 2) - Math.pow(vector1_length, 2));

			vector2_x = choice*(-(vector1_y / vector1_length) * vector2_length);
			vector2_y = choice*((vector1_x / vector1_length) * vector2_length);

			newX += (vector1_x + vector2_x);
			newY += (vector1_y + vector2_y);
			result = new Point(newX, newY);
		}
		else {
			double ratio = length / dist;
			double newX = start.x + x_diff * ratio;
			double newY = start.y + y_diff * ratio;
			result = new Point(newX, newY);
		}
		if (isplayer1) { // Very questionable logic here :(
			result.x *= -1;
		}
		return result;
	}

	// Find two moves that moves a coin to the destination
	// public ArrayList<Pair<Integer, Point>> find_two_moves(Point start, Point dest, Integer index, 
	// 	   HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
	// 	ArrayList<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
	// 	Point temp = find_move_exact(start, dest, 1);
	// 	Pair<Integer, Point> move = new Pair<Integer, Point>(index, temp);
	// 	if(check_validity(move, player_pieces, opponent_pieces)) {
	// 		moves.add(move);
	// 		temp = find_move_exact(temp, dest, 1);
	// 		move = new Pair<Integer, Point>(attacker, temp);
	// 			}
	// 			else {
	// 				temp = find_move_exact(player_pieces.get(attacker), attack_pos, -1);
	// 				move = new Pair<Integer, Point>(attacker, temp);
	// 				if (check_validity(move, player_pieces, opponent_pieces))
	// 					moves.add(move);
	// 				else {
	// 					// TODO: random
	// 				}
	// 			}
	// }

	// Find the id of a coin that's closest to a point, ignoring some visited
	public Integer closest(Point point, HashMap<Integer, Point> pieces, HashMap<Integer, Point> ignore) {
		double min_distance = Double.MAX_VALUE;
		Integer result_id = null;
		for (Integer i: pieces.keySet()) {
			if (ignore != null && ignore.containsKey(i)) 
				continue;
			Integer id = i;
			Point p = pieces.get(id);
			double distance = Math.pow(point.x - p.x, 2) + Math.pow(point.y - p.y, 2);
			if (distance < min_distance) {
				min_distance = distance;
				result_id = id;
			} 
		}
		return result_id;
	}

	public Integer closest(Point point, HashMap<Integer, Point> pieces, HashSet<Integer> ignore) {
		double min_distance = Double.MAX_VALUE;
		Integer result_id = null;
		for (Integer i: pieces.keySet()) {
			if (ignore != null && ignore.contains(i)) 
				continue;
			Integer id = i;
			Point p = pieces.get(id);
			double distance = Math.pow(point.x - p.x, 2) + Math.pow(point.y - p.y, 2);
			if (distance < min_distance) {
				min_distance = distance;
				result_id = id;
			} 
		}
		return result_id;
	}
	
	public Integer closestY(Point point, HashMap<Integer, Point> pieces, HashSet<Integer> ignore) {
		double min_distance = Double.MAX_VALUE;
		Integer result_id = null;
		for (Integer i: pieces.keySet()) {
			if (ignore != null && ignore.contains(i)) 
				continue;
			Integer id = i;
			Point p = pieces.get(id);
			double distance = Math.abs(point.y - p.y);
			if (distance < min_distance) {
				min_distance = distance;
				result_id = id;
			} 
		}
		return result_id;
	}
	
	// Find a list of points at x axis that can form a vertical wall
	// spacing is the space between each coin
	public ArrayList<Point> find_wall_points(double x, double spacing, double start) {
		ArrayList<Point> result = new ArrayList<Point>();
		double d = this.diameter_piece;
		double y_max = 20.0;
		double y_min = -20.0;
		double curr_y = y_min + d/2 + start;
		for (;curr_y + d/2 <= y_max; curr_y += d + spacing) {
			result.add(new Point(x, curr_y));
		}
		return result;
	}

	// Find a random valid move, (can be invalid)
	public Pair<Integer, Point> get_random_move(Integer piece_id, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
		int attemps = 0;
		int max_attemps = 1;
		while (attemps < max_attemps) {
			Point curr_position = player_pieces.get(piece_id);
			Point new_position = new Point(curr_position);
			double theta = 2 * Math.PI * random.nextDouble();
			double delta_x = diameter_piece * Math.cos(theta);
			double delta_y = diameter_piece * Math.sin(theta);
			new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
			new_position.y += delta_y;
			Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
			if(check_validity(move, player_pieces, opponent_pieces))
				return move;
			attemps += 1;
		}
		Point temp = player_pieces.get(piece_id);
		temp.x += isplayer1 ? -diameter_piece : diameter_piece;
		return new Pair<Integer, Point>(piece_id, temp);
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
	
	public boolean check_validity_(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        boolean valid = true;
       
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;

    }
}

