package flip.g6;
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
	private static final double THRESHOLD = 0.7;
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


	// TODO: Need to modify this code - see Overleaf document to determine how to choose a certain approach
	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {

		List<Pair<Integer, Point>> moves = new ArrayList<>();
		
		for(int i = 0; i < num_moves; i++) {			
			double randomValue = Math.random();
			
			Aggressive aggressive = new Aggressive(player_pieces, opponent_pieces, isplayer1, n, diameter_piece);
			ObstacleAvoidance obstacleAvoidance = new ObstacleAvoidance(player_pieces, opponent_pieces, isplayer1, n, diameter_piece);
			ObstacleCreation obstacleCreation = new ObstacleCreation(player_pieces, opponent_pieces, isplayer1, n, diameter_piece);
			
			if(i == 0) {
				if(randomValue <= THRESHOLD) {
					if(aggressive.isPossible())
						moves.add(aggressive.getMove());
					else if(obstacleAvoidance.isPossible())
						moves.add(obstacleAvoidance.getMove());
					else if(obstacleCreation.isPossible())
						moves.add(obstacleCreation.getMove());
					/* TODO: add cases where behavior for coins need to change when 
					 * none of the above cases apply - can implement within 
					 * individual class files. THis is a "hybrid" move.
					 */
				}
				else {
					if(obstacleAvoidance.isPossible())
						moves.add(obstacleAvoidance.getMove());
					else if(aggressive.isPossible())
						moves.add(aggressive.getMove());
					else if(obstacleCreation.isPossible())
						moves.add(obstacleCreation.getMove());
					/* TODO: add cases where behavior for coins need to change when 
					 * none of the above cases apply - can implement within 
					 * individual class files. This is a "hybrid" move.
					 */
				}
			}
			else {
				if(randomValue <= THRESHOLD) {
					if(obstacleCreation.isPossible())
						moves.add(obstacleCreation.getMove());
					else if(aggressive.isPossible())
						moves.add(aggressive.getMove());
					else if(obstacleAvoidance.isPossible())
						moves.add(obstacleAvoidance.getMove());
					/* TODO: add cases where behavior for coins need to change when 
					 * none of the above cases apply - can implement within 
					 * individual class files. This is a "hybrid" move.
					 */
				}
				else {
					if(obstacleAvoidance.isPossible())
						moves.add(obstacleAvoidance.getMove());
					else if(aggressive.isPossible())
						moves.add(aggressive.getMove());
					else if(obstacleCreation.isPossible())
						moves.add(obstacleCreation.getMove());
					/* TODO: add cases where behavior for coins need to change when 
					 * none of the above cases apply - can implement within 
					 * individual class files. This is a "hybrid" move.
					 */
				}
			}
		}			
		return moves;
	}
}