/*
	Project: Flip
	Course: Programming & Problem Solving
	Year : 2019
	Instructor: Prof. Kenneth Ross
	URL: http://www.cs.columbia.edu/~kar/4444f19/

	Author: Vaibhav Darbari
	Simulator Version: 1.0
	
*/
package flip.sim;

import java.util.List;
import javafx.util.Pair; 
import flip.sim.Point;
import flip.sim.Board;
import java.util.HashMap; 


public interface Player {
    // Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
    public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece);

    // Gets the moves from the player. Number of moves is specified by first parameter.
    public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1);
}