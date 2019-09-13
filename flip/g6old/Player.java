package flip.g6old;
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
	//private int seed = 50;
	private Random random;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;


	public Player()
	{
		random = new Random();
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
		 Point opponentCentroid = getPlayerCentroid(opponent_pieces);
		 Point teamCentroid = getPlayerCentroid(player_pieces);
		 double angleBetweenPalyerCenteroids = 0;
		 if(player_pieces.size() > 5) {
			 getAngle(teamCentroid, opponentCentroid);
		 }
		 double maxInteriorDistance = player_pieces.size()/10 + 3;
		 double maxCentroidDistance = player_pieces.size()/10;
		 
		 while(moves.size()!= num_moves && i<num_trials)
		 {
		 	Integer piece_id = random.nextInt(n);
		 	Point curr_position = player_pieces.get(piece_id);
		 	Point new_position = new Point(curr_position);
		 	
		 	if((isplayer1 && curr_position.x < -(20 + maxInteriorDistance)) || (!isplayer1 && curr_position.x > (20 + maxInteriorDistance))) {
		 		// Player 1
		 		continue;
		 	}
		 	
		 	double theta = -Math.PI/2 + Math.PI * random.nextDouble();
		 	double delta_x = 0;
		 	double delta_y = 0;
		 	if(Math.abs(opponentCentroid.x - teamCentroid.x) > maxCentroidDistance && (opponentCentroid.x > teamCentroid.x) && !isplayer1) {
		 		delta_x = diameter_piece * Math.cos(angleBetweenPalyerCenteroids);
			 	delta_y = diameter_piece * Math.sin(angleBetweenPalyerCenteroids);
		 	}else if(Math.abs(opponentCentroid.x - teamCentroid.x) > maxCentroidDistance && opponentCentroid.x < teamCentroid.x  && isplayer1) {
		 		delta_x = diameter_piece * Math.cos(-angleBetweenPalyerCenteroids);
			 	delta_y = diameter_piece * Math.sin(-angleBetweenPalyerCenteroids);
		 	}else {
		 		delta_x = diameter_piece * Math.cos(theta);
			 	delta_y = diameter_piece * Math.sin(theta);
		 	}

		 	Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
		 	// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
		 	// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

		 	new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
		 	new_position.y += delta_y;
		 	Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);

		 	Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
		 	// System.out.println("distance from previous position is " + dist.toString());
		 	// Log.record("distance from previous position is " + dist.toString());

		 	if(check_validity(move, player_pieces, opponent_pieces))
		 		moves.add(move);
		 	i++;
		 }
		 
		 return moves;
	}
	
	
	/**
	 * Get centroid of opponent points
	 * @param opponent_pieces
	 * @return
	 */
	public Point getPlayerCentroid(HashMap<Integer, Point> pieces)  {
		double centroidX = 0, centroidY = 0;

			for(Integer i : pieces.keySet()) {
				Point point = pieces.get(i);
				centroidX += point.x;
				centroidY += point.y;
			}
		return new Point(centroidX / pieces.size(), centroidY / pieces.size());
	}

	/**
	 * Get angle between two points
	 * @param target
	 * @return
	 */
	public float getAngle(Point origin, Point target) {
	    float angle = (float) Math.toDegrees(Math.atan2(target.y - origin.y, target.x - origin.x));

	    if(angle < 0){
	        angle += 360;
	    }

	    return angle;
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
