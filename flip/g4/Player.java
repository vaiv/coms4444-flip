// updated for 9/16 deliverable
package flip.g4;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import javafx.util.Pair; 
import java.util.ArrayList;
import java.lang.*;

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
    // n: Number of pieces available. (default 30 in Makefile)
    // t: Total turns available.
	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
	{
		this.n = n;
		this.isplayer1 = isplayer1;
		this.diameter_piece = diameter_piece; // default 2
	}

    // x coordinate
	public boolean inSoftEndZone( Point piece, boolean isPlayer1 ) {
	    return (isPlayer1? -1 : 1) * piece.x > 20 + 1.75 * diameter_piece + (n / 9) * (diameter_piece / 2);
	}


    // Returns a list of ALL our player pieces (or opponent's pieces) in order of closest to farthest along x
    private static List<Pair<Integer,Point>> rankXProgress( HashMap<Integer, Point> pieces, boolean arePiecesPlayer1 ) {
    	int sign = arePiecesPlayer1? 1 : -1;
    	List<Integer> indices = new ArrayList<>(pieces.keySet());
    	indices.sort(new Comparator<Integer>() {
            public int compare( Integer i1, Integer i2 ) {
                return (pieces.get(i1).x > pieces.get(i2).x)? sign : -sign;
            }
        });

        // debugging printer: prints (id, x)
        /*
        System.out.print("Pairs:  ");
        for (Integer i : indices) System.out.print(" (" + i + ", " + pieces.get(i).x + ")");
        System.out.println();
        */
        List<Pair<Integer,Point>> orderedPieces = new ArrayList<Pair<Integer,Point>>();
        for (Integer i : indices) {
            orderedPieces.add(new Pair<Integer, Point>(i, pieces.get(i)));
        }
        
        return orderedPieces;
    }
    
    
    // returns our (or our opponent's) sorted pieces list WITHOUT pieces that have already crossed the endzone
    private List<Pair<Integer,Point>> rankXProgressOutsideEndzone( HashMap<Integer,Point> pieces, boolean arePiecesPlayer1 ) {
    	List<Pair<Integer,Point>> orderedPieces = rankXProgress(pieces, arePiecesPlayer1);
        List<Pair<Integer,Point>> outsideEndzonePieces = new ArrayList<Pair<Integer,Point>>();

    	int sign = arePiecesPlayer1? -1 : 1;
    	// use indices.toArray() as a deep copy to avoid changing indices inside for loop
    	for (Pair<Integer,Point> pair : orderedPieces) {
    	    if (!inSoftEndZone(pieces.get(pair.getKey()), arePiecesPlayer1)) {
//    	    if ((pieces.get(pair.getKey()).x *sign) < (20 + diameter_piece/2)) {
    	        outsideEndzonePieces.add(pair);
    	    }
    	}
    	
    	/*
    	// debugging printer
    	System.out.print("Pairs:  ");
        for (Pair<Integer,Point> pair : outsideEndzonePieces) System.out.print(" (" + pair.getKey() + ", " + pair.getValue().x + ")");
        System.out.println();
        */
        
        return outsideEndzonePieces;
    }
    

    
    // Unused
    // Return a list of our pieces (in order of closeness to endzone) without us or an opponent's piece blocking straight path (for one move)
    // used to create list of our unblocked pieces in order of closeness to endzone
    private List<Pair<Integer,Point>> findOurUnblockedPieces( HashMap<Integer,Point> ourPieces, HashMap<Integer, Point> opponentPieces, boolean areWePlayer1 ) {
    	List<Pair<Integer,Point>> ourSortedPieces = rankXProgress(ourPieces, areWePlayer1);
    	List<Pair<Integer,Point>> opponentsSortedPieces = rankXProgress(opponentPieces, !areWePlayer1);

        // make a copy of pieces arraylist
        List<Pair<Integer,Point>> freePiecesIgnoringOpp = new ArrayList<Pair<Integer,Point>>();
        for (Pair<Integer,Point> piece : ourSortedPieces) freePiecesIgnoringOpp.add(piece);

        // remove pieces that are blocked by you
    	for (int i = 0; i < ourSortedPieces.size(); i++) {
    	    Point piece_i = ourSortedPieces.get(i).getValue();

    	    for (int j = i +1; j < ourSortedPieces.size(); j++) {
        	    Point piece_j = ourSortedPieces.get(j).getValue();
    	        
    	        // if your y path is blocked AND x path is blocked by yourself
                if (Math.abs(piece_i.y - piece_j.y) < diameter_piece && Math.abs(piece_i.x - piece_j.x) < diameter_piece) {
                    freePiecesIgnoringOpp.remove(ourSortedPieces.get(j));
                }
    	    }
    	}
    	
    	// make a copy of smaller pieces arraylist
        List<Pair<Integer,Point>> freePieces = new ArrayList<Pair<Integer,Point>>();
        for (Pair<Integer,Point> piece : freePiecesIgnoringOpp) freePieces.add(piece);
    	
    	// remove pieces that are blocked by opponent
	    for (int i = 0; i < freePiecesIgnoringOpp.size(); i++) {
            //*
            Point piece_i = freePiecesIgnoringOpp.get(i).getValue();

    	    for (int j = i +1; j < opponentsSortedPieces.size(); j++) {
        	    Point piece_j = opponentsSortedPieces.get(j).getValue();
    	        
    	        // if your y path is blocked AND x path is blocked by opponent
                if (Math.abs(piece_i.y - piece_j.y) < diameter_piece && Math.abs(piece_i.x - piece_j.x) < diameter_piece) {
                    freePieces.remove(opponentsSortedPieces.get(j));
                }
    	    }
    	    //*
	    }
    	
        // Testing
    	//System.out.println("freePieces: " + freePieces);
    	return freePieces;
    }
    

    
    // Unused
    // Return a list of opponent's pieces without an opponent's piece in their "lane" (sharing same y)
    private List<Pair<Integer,Point>> findUnblockedOpponents( HashMap<Integer, Point> opponentPieces, boolean areWePlayer1 ) {
    	boolean arePiecesPlayer1 = areWePlayer1;
    	List<Pair<Integer,Point>> sortedOpponentPieces = rankXProgress(opponentPieces, !arePiecesPlayer1);

        // make a copy of pieces arraylist
        List<Pair<Integer,Point>> freePieces = new ArrayList<Pair<Integer,Point>>();
        for (Pair<Integer,Point> piece : sortedOpponentPieces) freePieces.add(piece);

        // remove pieces that are blocked
    	for (int i = 0; i < sortedOpponentPieces.size(); i++) {
    	    Point piece_i = sortedOpponentPieces.get(i).getValue();

    	    for (int j = i +1; j < sortedOpponentPieces.size(); j++) {
        	    Point piece_j = sortedOpponentPieces.get(j).getValue();
    	        
                if (Math.abs(piece_i.y - piece_j.y) < diameter_piece) {
                    freePieces.remove(sortedOpponentPieces.get(j));
                }
    	    }
    	}
        // Testing
    	System.out.println("freePieces: " + freePieces);
    	return freePieces;
    }
    
    // Unused
    // Returns list of 11 ideal pieces for building wall
    private List<Pair<Integer,Point>> findIdealWallPieces( HashMap<Integer,Point> playerPieces, boolean isplayer1 ) {
    	// Find 11 pieces where x is the most similar (Find location with least absolute offset)
    	// Then, count number of steps to build that ideal wall	    
    	List<Pair<Integer,Point>> orderedXProgress = rankXProgress(playerPieces, isplayer1);
    	int idealNumPiecesForWall = 11;
    	
    	List<Pair<Integer,Point>> idealWallPieces = new ArrayList<Pair<Integer, Point>>();
    	int idealWallPiecesStartingIndex = 0;
    	
    	// by default, set absOffset arbitrarily high
    	Double absOffset = 1000.;
    	for (int i = 0; i < playerPieces.size() -(idealNumPiecesForWall -1); i++) {
    	    Double newAbsOffset = 0.;
    	    for (int j = 0; j < idealNumPiecesForWall -1; j++) {
    	        Point piece_1 = orderedXProgress.get(i+j).getValue();
    	        Point piece_2 = orderedXProgress.get(i+j +1).getValue();
    	        newAbsOffset += Math.abs( piece_1.x - piece_2.x );
    	    }
    	    if (newAbsOffset < absOffset) {
    	        absOffset = newAbsOffset;
    	        idealWallPiecesStartingIndex = i;
    	    }
    	}
        
        for (int i = idealWallPiecesStartingIndex; i < idealNumPiecesForWall; i++) {
            idealWallPieces.add(orderedXProgress.get(i));
        }
        // Testing
    	//System.out.println("idealWallPieces: " + idealWallPieces);
    	return idealWallPieces;
    }
    
    // Unfinished function in progress
    // Returns the minimum number of moves needed to form a viable wall ignoring opponent pieces
    	// Note: Can be used for either yourself or opponent!
    	//Note: Use and assume pieceTowardsPoint works
    /*
    private int stepsToWall( HashMap<Pair<Integer,Point>> playerPieces, boolean isplayer1 ) {
        // in progress: would use findIdealWallPieces()
        int stepsToWall = 0;
        return stepsToWall;
    }
    */

    private Pair<Integer, Point> getForwardMove( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1, boolean isFront ) {
        // get all our pieces from closest to farthest
//        List<Pair<Integer,Point>> orderedXProgress = isFront? rankXProgressOutsideEndzone(playerPieces, isPlayer1):
//                                                                            rankXProgress(playerPieces, isPlayer1);
        List<Pair<Integer,Point>> orderedXProgress = rankXProgressOutsideEndzone(playerPieces, isPlayer1);

        // one by one, check validity if we move it forward
        int max_index = orderedXProgress.size() -1;
        for (int i = 0; i <= max_index; i++) {
            Pair<Integer,Point> pair = orderedXProgress.get(isFront? i : (max_index -i));
            Point oldPosition = pair.getValue();
            double dx = (isPlayer1? -1 : 1) * diameter_piece;
            Point newPosition = new Point(oldPosition.x + dx, oldPosition.y);
            Pair<Integer,Point> move = new Pair<Integer,Point>(pair.getKey(), newPosition);

            if (check_validity(move, playerPieces, opponentPieces)) return move;
        }
        return null;        
    }
    
    private Pair<Integer, Point> getRandomMove( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1, double spread ) {
        int MAX_TRIALS = 100;

        // get list of all my piece ids
    	List<Integer> ids = new ArrayList<>(playerPieces.keySet());

        for (int trial_num = 0; trial_num < MAX_TRIALS; trial_num++) {
            // select random piece
            int id = ids.get(random.nextInt(ids.size()));
		 	Point oldPosition = playerPieces.get(id);

            // if already in soft endzone, don't use it
            if (inSoftEndZone(oldPosition, isPlayer1)) continue;

		 	// select random angle
		 	double theta = (random.nextDouble() -0.5) *spread *Math.PI/180;
		 	double dx = Math.cos(theta) * (isPlayer1? -1 : 1) * diameter_piece, dy = Math.sin(theta) * diameter_piece;
		 	Point newPosition = new Point(oldPosition.x + dx, oldPosition.y + dy);
            
            Pair<Integer,Point> move = new Pair<Integer,Point>(id, newPosition);
            if (check_validity(move, playerPieces, opponentPieces)) return move;
        }
        return null;  
    }
    
	public List<Pair<Integer, Point>> getMoves( Integer numMoves, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1 ) {
		 List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

         while (moves.size() < numMoves) {
             Pair<Integer, Point> move = null;

             // move the closest piece that can move forward
             move = getForwardMove(playerPieces, opponentPieces, isPlayer1, true);
             if (move != null) moves.add(move);
             
             // move the farthest away piece that can move forward
             move = getForwardMove(playerPieces, opponentPieces, isPlayer1, false);
             if (move != null) moves.add(move);

             // choose best forwardish direction as next option 
//             move = getBestForwardishMove(playerPieces, opponentPieces, isPlayer1, true);
//             if (move != null) moves.add(move);             
//             move = getBestForwardishMove(playerPieces, opponentPieces, isPlayer1, false);
//             if (move != null) moves.add(move);             

             // choose valid random forwardish to less forward directions as next options
             // Can first optimize by looking at only angles you haven't already looked at
             // Ideally, would have an improved function [ getBestForwardishMove(), not yet built ] that finds the best move
             move = getRandomMove(playerPieces, opponentPieces, isPlayer1, 90);
             if (move != null) moves.add(move);             

             move = getRandomMove(playerPieces, opponentPieces, isPlayer1, 180);
             if (move != null) moves.add(move);             

             move = getRandomMove(playerPieces, opponentPieces, isPlayer1, 270);
             if (move != null) moves.add(move);             
         }

         return moves;
     }

    /*   OLD
         //System.out.println("player_pieces: " + player_pieces);
		 int num_trials = 30;
		 int i = 0;

		 while (i<num_trials)
		 {
		 	// create list of your pieces in order of closeness to endzone
		 	List<Pair<Integer,Point>> orderedUnfinishedXProgress = rankXProgressOutsideEndzone(player_pieces, isplayer1);
		 	// create list of your unblocked pieces in order of closeness to endzone
		 	List<Pair<Integer,Point>> ourOrderedUnblockedPieces = findOurUnblockedPieces(player_pieces, opponent_pieces, isplayer1);
		 	
		 	// default integer piece id is random, and will resort to this when better options exhausted
		 	Integer piece_id = random.nextInt(n); 
		 	
		 	// when at least half the number of pieces are unfinished, focus on getting closest unblocked ones to other side
		 	if (orderedUnfinishedXProgress.size() >= player_pieces.size()/2) {
		 	    
		 	    // if there are unblocked unfinished pieces:
		 	    if (ourOrderedUnblockedPieces.size() > 0) {
		 	        // find piece id closest to finish line that is also unblocked
		 	        //Pair pair = ourOrderedUnblockedPieces.get(0);
		 	        piece_id = ourOrderedUnblockedPieces.get(0).getKey();
    		 	    //piece_id = pair.getKey();
    		 	    Point curr_position = player_pieces.get(piece_id);
                    i++;
                    continue;
		 	    }
		 	}
		 	
		 	Point curr_position = player_pieces.get(piece_id);
		 	
		 	//System.out.println("curr_position: " + curr_position);
		 	if(inEndZone(curr_position.x, isplayer1)) {
		 		i++;
		 		continue;
		 	}
		 	
		 	// if not in endzone, and no clear path, set a random piece to original random path settings
		 	Point new_position = new Point(curr_position);
            
            

		 	double theta = -Math.PI/2 + Math.PI * random.nextDouble();
		 	if((isplayer1 && curr_position.x > 20 )|| (!isplayer1 && curr_position.x < -20)) {
		 		theta = 0;
		 	}
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

		 	if (check_validity(move, player_pieces, opponent_pieces)) {
		 	    if isMovesQuotaReached(num_moves, moves, move) return moves;
		 	}
		 	i++;
		 }
		 // Testing functions:
		 //List<Pair<Integer,Point>> orderedUnfinishedXProgress = rankXProgressOutsideEndzone(player_pieces, isplayer1);
		 //List<Pair<Integer,Point>> orderedUnfinishedXProgress = rankXProgressOutsideEndzone(opponent_pieces, !isplayer1);
		 //List<Pair<Integer,Point>> orderedXProgress = rankXProgress(player_pieces, isplayer1);
		 //List<Pair<Integer,Point>> orderedXProgress = rankXProgress(opponent_pieces, !isplayer1);
		 
		 //List<Pair<Integer,Point>> unblockedOpponents = findUnblockedOpponents(opponent_pieces, isplayer1);
		 //List<Pair<Integer,Point>> idealWallPieces = findIdealWallPieces(player_pieces, isplayer1);
		 return moves;
	}*/

	public boolean check_validity( Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces ) {
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
