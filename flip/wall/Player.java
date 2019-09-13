package flip.wall;

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

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		 List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

		 int num_trials = 30;
		 int i = 0;

		 while(moves.size()!= num_moves && i<num_trials)
		 {
		 	Integer piece_id = random.nextInt(n);
		 	Point curr_position = player_pieces.get(piece_id);
		 	Point new_position = new Point(curr_position);

		 	double theta = -Math.PI/2 + Math.PI * random.nextDouble();
		 	double delta_x = diameter_piece * Math.cos(theta);
		 	double delta_y = diameter_piece * Math.sin(theta);

		 	Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
		 	// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
		 	// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

		 	new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
			new_position.y += delta_y; 

			Point temp = next_move(curr_position, new Point(0, 0));

		 	Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, temp);

		 	Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
		 	// System.out.println("distance from previous position is " + dist.toString());
		 	// Log.record("distance from previous position is " + dist.toString());

		 	if(check_validity(move, player_pieces, opponent_pieces))
		 		moves.add(move);
		 	i++;
		 }
		 
		 return moves;
	}

	// Find a valid move that moves a coin closer to destination
	// TODO: more advanced path finding
	public Point next_move(Point start, Point dest) {
		double length = this.diameter_piece;
		double y_diff = dest.y - start.y;
		double x_diff = dest.x - start.x;
		if (Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2)) <= length) {
			return new Point(dest);
		}
		double ratio = length / Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
		double newX = start.x + x_diff * ratio;
		double newY = start.y + y_diff * ratio;
		return new Point(newX, newY);
	}

	// Find a list of points at x axis that can form a wall

	// spacing is the space between each coin
	public ArrayList<Point> wall_points(double x, double spacing) {
		ArrayList<Point> result = new ArrayList<Point>();
		double d = this.diameter_piece;
		double y_max = 20.0;
		double y_min = -20.0;
		double curr_y = y_min + d / 2;
		for (;curr_y + d <= y_max; curr_y += d + spacing) {
			result.add(new Point(x, curr_y));
		}
		return result;
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

