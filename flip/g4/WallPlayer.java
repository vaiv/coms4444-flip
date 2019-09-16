package flip.g4;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import javafx.util.Pair; 
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.*;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;
import flip.g4.HungarianAlgorithm;

public class WallPlayer implements flip.sim.Player
{
	private int seed = 42;
	private Random random;
	private boolean isplayer1;
	private Integer n;
    private Double diameter_piece;

    private HashMap<Integer, Point> my_pieces;
    private Point[]   ideal_wall_pieces;
    private Integer[] ideal_piece_match;
    private int count;

	public WallPlayer() {
        random = new Random(seed);
    }

	// Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available. (default 30 in Makefile)
    // t: Total turns available.
	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
	{
        this.n = n;
        this.count = 0;
		this.isplayer1 = isplayer1;
        this.diameter_piece = diameter_piece; // default 2
        this.my_pieces = pieces;

        this.ideal_piece_match = this.calculateWallStrategy(pieces, isplayer1);
	}

    private Integer[] calculateWallStrategy(HashMap<Integer, Point> playerPieces, boolean isPlayer1){
        // Generate co-ordinates for ideal wall
        // TODO: Deviate from "ideal" wall to "approx" wall to reduce moves
        int num_wall_pieces = 11;
        this.ideal_wall_pieces = new Point[num_wall_pieces];

        Log.log(String.valueOf(isPlayer1) + playerPieces.toString());

        for(int i=0; i<num_wall_pieces; i++){
            // Calculations done assuming perfect placement for 1cm coins on 40cm board
            ideal_wall_pieces[i] = new Point((isPlayer1)? 21.5: -21.5, 2*i*Math.sqrt(3) + Math.sqrt(3) - 19);
        }

        // Figure out the distance from each coin to the ideal wall piece location
        double[][] cost_matrix = new double[this.n][num_wall_pieces];
        for(int i=0; i<num_wall_pieces; i++){
            for(int j=0; j<this.n; j++){
                double dist = Math.hypot(
                    ideal_wall_pieces[i].x - playerPieces.get(j).x,
                    ideal_wall_pieces[i].y - playerPieces.get(j).y
                );
                
                cost_matrix[j][i] = Board.almostEqual(dist, 2) ? 1: Math.max(2, Math.ceil(dist/2));
            }
        }

        // Solve hungarian algorithm for minimum placement in O(n^3)
        HungarianAlgorithm solver = new HungarianAlgorithm(cost_matrix);
        int[] solution = solver.execute();
        Log.log("CURRENT : Hungarian Algorithm solved " + Arrays.toString(solution));

        // Store calculated solution
        Integer[] ideal_piece_match = new Integer[11];
        for(int pieceID=0; pieceID<this.n; pieceID++){
            if(solution[pieceID]>=0) ideal_piece_match[solution[pieceID]] = pieceID;
        }

        Log.log("CURRENT : Ideal piece match. " + Arrays.toString(ideal_piece_match));
        return ideal_piece_match;
    }

    private Integer[] getWallPriority(HashMap<Integer, Point> opponentPieces){
        Integer[] distanceToWall = new Integer[11];
        for(int i=0; i<11; i++)
            distanceToWall[i] = Integer.MAX_VALUE;

        // Iterate through opponent pieces to prioritize wall
        for(int j=0; j<this.n; j++){
            for (int i=0; i<11; i++){
                double dist = Math.hypot(
                    opponentPieces.get(j).x-ideal_wall_pieces[i].x,
                    opponentPieces.get(j).y-ideal_wall_pieces[i].y);
                
                Integer numMoves = (Board.almostEqual(dist, 2)) ? 1: (int) Math.max(2, Math.ceil(dist/2));
                if (numMoves < distanceToWall[i])
                    distanceToWall[i] = numMoves;
            }
        }

        // argsort() function
        // Copied from https://stackoverflow.com/questions/4859261/get-the-indices-of-an-array-after-sorting
        // Will re-implement later
        class ArrayIndexComparator implements Comparator<Integer> {
            private final Integer[] array;
            public ArrayIndexComparator(Integer[] array) { this.array = array;}
            public Integer[] createIndexArray() {
                Integer[] indexes = new Integer[array.length];
                for (int i = 0; i < array.length; i++)
                    indexes[i] = i; // Autoboxing
                return indexes;
            }
            @Override
            public int compare(Integer index1, Integer index2){
                // Autounbox from Integer to int to use as array indexes
                return array[index1].compareTo(array[index2]);
            }
        }

        ArrayIndexComparator comparator = new ArrayIndexComparator(distanceToWall);
        Integer[] indexes = comparator.createIndexArray();
        Arrays.sort(indexes, comparator);

        return indexes;
    }

	public List<Pair<Integer, Point>> getWallMove( Integer numMoves,
        HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1) {

        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
        Integer[] bestWallPieces = this.getWallPriority(opponentPieces);
        this.count++;

        for(int i=0; i<11 && moves.size()<2; i++){
            // If piece is in place
            Point wallPiece = ideal_wall_pieces[bestWallPieces[i]];
            int  idealPiece = ideal_piece_match[bestWallPieces[i]];
            Point myPiece   = playerPieces.get(idealPiece);

            double dist = Math.hypot(wallPiece.x-myPiece.x, wallPiece.y-myPiece.y);
            if (dist <= 1e-2) // Check if the piece is in place
                continue;
            else if(Board.almostEqual(dist, 2)) {
                moves.add(new Pair<Integer,Point>(idealPiece, wallPiece));
            }
            else if (dist < 4) {
                double x1 = 0.5 * (wallPiece.x + myPiece.x);
                double y1 = 0.5 * (wallPiece.y + myPiece.y);

                double sqrt_const = Math.sqrt(16/(dist*dist)-1) / 2;
                double x2 = sqrt_const * (wallPiece.y - myPiece.y);
                double y2 = sqrt_const * (myPiece.x - wallPiece.x);

                Pair<Integer, Point> move = new Pair<Integer, Point>( idealPiece, new Point(x1+x2, y1+y2));

                if(check_validity(move, playerPieces, opponentPieces)){
                    moves.add(move);

                    Pair<Integer, Point> move2 = new Pair<Integer, Point>( idealPiece, new Point(wallPiece.x, wallPiece.y));
                    if(check_validity(move2, playerPieces, opponentPieces))
                        moves.add(move2);
                } else{
                    move = new Pair<Integer, Point>( idealPiece, new Point(x1-x2, y1-y2));
                    if(check_validity(move, playerPieces, opponentPieces)){
                        moves.add(move);

                        Pair<Integer, Point> move2 = new Pair<Integer, Point>( idealPiece, new Point(wallPiece.x, wallPiece.y));
                        if(check_validity(move2, playerPieces, opponentPieces))
                            moves.add(move2);
                    }
                }
            } else {
                Pair<Integer, Point> move = new Pair<Integer, Point>(idealPiece, new Point(
                    myPiece.x + 2 * (wallPiece.x - myPiece.x) / dist,
                    myPiece.y + 2 * (wallPiece.y - myPiece.y) / dist
                ));

                if(check_validity(move, playerPieces, opponentPieces)){
                    moves.add(move);
                
                    if (dist > 6 && moves.size()==1){
                        Pair<Integer, Point> move2 = new Pair<Integer, Point>(idealPiece, new Point(
                            myPiece.x + 4 * (wallPiece.x - myPiece.x) / dist,
                            myPiece.y + 4 * (wallPiece.y - myPiece.y) / dist
                        ));
                        
                        if(check_validity(move2, playerPieces, opponentPieces))
                            moves.add(move2);
                    }
                }
            }
        }

        if (moves.size()==0 && this.count < 1000){
            Log.log("WALL COMPLETED IN " + String.valueOf(this.count) + " MOVES");
            this.count = 1000;
        }
        return moves;
    }

    public List<Pair<Integer, Point>> getMoves( Integer numMoves,
        HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1) {
        
        // Create an ENUM of Strategies
        // Logic for selecting the correct strategy goes here
        return getWallMove(numMoves, playerPieces, opponentPieces, isPlayer1);
    
    }


    public boolean check_validity( Pair<Integer, Point> move,
        HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces ) {

        // check for collisions
        if(Board.check_collision(player_pieces, move) || Board.check_collision(opponent_pieces, move))
            return false;
        
        // check within bounds
        return Board.check_within_bounds(move);
    }
}