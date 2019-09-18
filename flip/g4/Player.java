// updated for 9/18 deliverable - adding the wall
package flip.g4;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Collections;
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
import flip.g4.PieceStore;
import flip.g4.WallStrategy;

public class Player implements flip.sim.Player
{
    private int seed = 42;
    public Random random;
    public boolean isPlayer1;
    public Integer n;
    public Double diameter_piece;

    // Strategy data structures
    public HashMap<Integer, Point> playerPieces;
    public HashMap<Integer, Point> opponentPieces;
    public WallStrategy   mWallStrategy;
    public RunnerStrategy mRunnerStrategy;

    public Player(){
        random = new Random(seed);
    }

    // Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available. (default 30 in Makefile)
    // t: Total turns available.
    public void init(HashMap<Integer, Point> pieces, 
        int n, double t, boolean isPlayer1, double diameter_piece) {
        this.n = n;
        this.isPlayer1 = isPlayer1;
        this.diameter_piece = diameter_piece; // default 2
        
        // wall
        this.playerPieces  = pieces;
        this.mWallStrategy = new WallStrategy(this, pieces);
        Log.log("WALL STRATEGY INITIALIZED");

        this.mRunnerStrategy = new RunnerStrategy(this, pieces,
            this.mWallStrategy.runner);
        
    }

    // x coordinate
    public boolean inSoftEndZone( Point piece, boolean isPlayer1 ) {
        return (isPlayer1? -1 : 1) * piece.x > 20 + 1.75 * diameter_piece + (n / 9) * (diameter_piece / 2);
    }

    // used for getting only pieces for building a wall
    public boolean inSoftNeutralZone( Point piece, boolean isPlayer1 ) {
        //return (isPlayer1? -1 : 1) * piece.x > 20 + 1.75 * diameter_piece + (n / 9) * (diameter_piece / 2);
        return (isPlayer1? 1 : -1) * piece.x > 19 * diameter_piece;
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
    
    // returns our (or our opponent's) sorted pieces list WITHOUT pieces that have already crossed the endzone
    private List<Pair<Integer,Point>> rankXProgressOutsideSoftNeutral( HashMap<Integer,Point> pieces, boolean arePiecesPlayer1 ) {
        List<Pair<Integer,Point>> orderedPieces = rankXProgress(pieces, arePiecesPlayer1);
        List<Pair<Integer,Point>> outsideNeutralSoftPieces = new ArrayList<Pair<Integer,Point>>();

        int sign = arePiecesPlayer1? -1 : 1;
        // use indices.toArray() as a deep copy to avoid changing indices inside for loop
        for (Pair<Integer,Point> pair : orderedPieces) {
            if (!inSoftNeutralZone(pieces.get(pair.getKey()), arePiecesPlayer1)) {
//    	    if ((pieces.get(pair.getKey()).x *sign) < (20 + diameter_piece/2)) {
                outsideNeutralSoftPieces.add(pair);
            }
        }
        
        /*
        // debugging printer
        System.out.print("Pairs:  ");
        for (Pair<Integer,Point> pair : outsideEndzonePieces) System.out.print(" (" + pair.getKey() + ", " + pair.getValue().x + ")");
        System.out.println();
        */
        
        return outsideNeutralSoftPieces;
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

            if (Utilities.check_validity(move, playerPieces, opponentPieces)) return move;
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
            if (Utilities.check_validity(move, playerPieces, opponentPieces)) return move;
        }
        return null;  
    }
    
    public List<Pair<Integer, Point>> getMoves( Integer numMoves,
        HashMap<Integer, Point> playerPieces,
        HashMap<Integer, Point> opponentPieces,
        boolean isPlayer1 ) {

        this.playerPieces   = playerPieces;
        this.opponentPieces = opponentPieces;

        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

        // Runner needs to pass the wall first
        if(this.mRunnerStrategy.status==RunnerStatus.RUNNER_SET){
            try {
                this.mRunnerStrategy.getRunnerMove(moves, numMoves);
            } catch (Exception e) {
                Log.log(e.toString());
            }
        }

        // Wall Strategy
        if (!this.mWallStrategy.WALL_COMPLETED){
            try {
                this.mWallStrategy.getWallMove(moves, numMoves);
            } catch (Exception e) {
                Log.log(e.toString());
                System.out.println(e.getLocalizedMessage());
            }
        }

        // Post wall runner strategy: RUN
        if(this.mRunnerStrategy.status==RunnerStatus.RUNNER_PASSED_WALL){
            try {
                this.mRunnerStrategy.getRunnerMove(moves, numMoves);
            } catch (Exception e) {
                Log.log(e.toString());
            }
        }

        // Post wall offensive strategy

        return moves;
    }
}