package flip.ntkplayer;
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
	private ArrayList<Integer> offensePieces = new ArrayList<Integer>();
	private ArrayList<Integer> defensePieces = new ArrayList<Integer>();


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

		for(int pieceId : pieces.keySet() ) {
			if( pieceId < n / 2) {
				try{
					this.offensePieces.add(pieceId);
				}
				catch(Exception e){
					System.out.println(e);
				}
			}
			else {
				try{
					this.defensePieces.add(pieceId);
				}
				catch(Exception e){
					System.out.println(e);
				}

			}
		}
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{

		if( (new Random()).nextInt(100) > 50 ) {
			// RETURN OFFENSIVE STRATEGY
			return getOffensiveMoves(num_moves, player_pieces, opponent_pieces, isplayer1);
		}
		else {
			// TODO: RETURN DEFENSIVE STRATEGY
			return getOffensiveMoves(num_moves, player_pieces, opponent_pieces, isplayer1);
		}
	}

	public List<Pair<Integer, Point>> getOffensiveMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		 List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

		 int num_trials = 30;
		 int i = 0;

		 while(moves.size()!= num_moves && i<num_trials)
		 {

			// Picking a random offensive piece
		 	int piece_idx = random.nextInt(offensePieces.size());
			Integer piece_id = offensePieces.get(piece_idx);

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
		 	Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);

		 	Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
		 	// System.out.println("distance from previous position is " + dist.toString());
		 	// Log.record("distance from previous position is " + dist.toString());

		 	if(check_validity(move, player_pieces, opponent_pieces)){
		 		moves.add(move);
		 		System.out.print("offense size ");
		 		System.out.println(offensePieces.size());
		 		if((isplayer1 && new_position.x < -25) || (!isplayer1 && new_position.x > 25)){
		 			offensePieces.remove(piece_idx);
		 			System.out.print("piece_idx ");
		 			System.out.println(piece_idx);
		 			int d_piece_idx = random.nextInt(defensePieces.size());
					Integer d_piece_id = defensePieces.get(d_piece_idx);
					offensePieces.add(d_piece_id);
		 		}
		 	}
		 	i++;
		 }

		 return moves;
	}

	// return the next piece to move to build a Wall
	public int getNextWallPiece(HashMap<Integer, Point> player_pieces){
		//arraylist of points as wall coordinates
		Point pos = wall.get(0);
		double minDist = Double.POSITIVE_INFINITY;
		double minPiece = -1;
		for(Integer id : wallPieces){
			Point piece = player_pieces.get(id);
			double dist = calculateDistance(pos.x, pos.y, piece.x, piece.y);
			if(dist < minDist){
				minDist = dist;
				minPiece = id;
			}
		}
		return minPiece;
	}

	public double calculateDistance(double x1, double y1, double x2, double y2){
		return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
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
