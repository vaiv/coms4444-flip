// UPDATED WITH N/3 STRAT
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
import flip.g4.PieceStore;
import flip.g4.Utilities;

// Abstracted class for wall building
class SmallNStrategy{
    private int debugCount = 0;
    // Store details about the game
    private Player mPlayer;

    public Integer n;
    public Random random;
    public Double diameter_piece;

    public SmallNStrategy(Player mPlayer, HashMap<Integer, Point> pieces){
        this.n = mPlayer.n;
        this.mPlayer = mPlayer;
        this.random  = mPlayer.random;
        this.diameter_piece = mPlayer.diameter_piece;
    }

    // x coordinate
    public boolean inSoftEndZone( Point piece, boolean isPlayer1 ) {
        return (isPlayer1? -1 : 1) * piece.x > 20 + 1.75 * diameter_piece + (n / 9) * (diameter_piece / 2);
    }
    
    // x coordinate
    public boolean inEndZone( Point piece, boolean isPlayer1 ) {
        return (isPlayer1? -1 : 1) * piece.x > 20 + diameter_piece/2;
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
            if (!inEndZone(pieces.get(pair.getKey()), arePiecesPlayer1)) {
     //       if (!inSoftEndZone(pieces.get(pair.getKey()), arePiecesPlayer1)) {
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


    // NEW FUNCTION FOR PROGRESSIVELY LESS FORWARD MOVE OF ONE PIECE AS OPTIONS ARE EXHAUSTED
    private Pair<Integer, Point> getForwardPieceMove( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1 ) {
            // get all our pieces from closest to farthest
    //        List<Pair<Integer,Point>> orderedXProgress = isFront? rankXProgressOutsideEndzone(playerPieces, isPlayer1):
    //                                                                            rankXProgress(playerPieces, isPlayer1);

    //          ORIGINAL
    //        List<Pair<Integer,Point>> orderedXProgress = rankXProgressOutsideEndzone(playerPieces, isPlayer1);
        List<Pair<Integer,Point>> orderedXProgress = rankXProgress(playerPieces, isPlayer1);

        Pair<Integer,Point> pair = orderedXProgress.get(0);
        Point oldPosition = pair.getValue();
        for (int trial_num = 0; trial_num < 360; trial_num++) {
            // select random angle (that enlarges with trial_num)
            double theta = ((random.nextDouble() > 0.5)? -1 : 1) *trial_num *Math.PI/180;
            double dx = Math.cos(theta) * (isPlayer1? -1 : 1) * diameter_piece, dy = Math.sin(theta) * diameter_piece;
            Point newPosition = new Point(oldPosition.x + dx, oldPosition.y + dy);
            Pair<Integer,Point> move = new Pair<Integer,Point>(pair.getKey(), newPosition);
            if (Utilities.check_validity(move, playerPieces, opponentPieces)) return move;
        }
        return null;        
    }
    
    // NEW FUNCTION FOR PROGRESSIVELY LESS FORWARD MOVE OF ONE PIECE AS OPTIONS ARE EXHAUSTED
    private Pair<Integer, Point> getBackwardPieceMove( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1 ) {
            // get all our pieces from closest to farthest
    //        List<Pair<Integer,Point>> orderedXProgress = isFront? rankXProgressOutsideEndzone(playerPieces, isPlayer1):
    //                                                                            rankXProgress(playerPieces, isPlayer1);

    //          ORIGINAL
        List<Pair<Integer,Point>>         orderedXProgress = rankXProgressOutsideEndzone(playerPieces, isPlayer1);
        if (orderedXProgress.size() == 0 || Math.random() < 0.1) orderedXProgress = rankXProgress(playerPieces, isPlayer1);
        //List<Pair<Integer,Point>>         inSoftEndZoneList = getInSoftEndZoneList(orderedXProgress, isPlayer1); // inSoftEndZoneList.size()

        for (int angle = 0; angle < 180; angle++) {
            // choose random direction
            double theta = ((random.nextDouble() > 0.5)? -1 : 1) *angle *Math.PI/180;
            double dx = Math.cos(theta) * (isPlayer1? -1 : 1) * diameter_piece, dy = Math.sin(theta) * diameter_piece;

            for (int i = orderedXProgress.size() /* - inSoftEndZoneList.size() */- 1; i >= 0; i--) {
                Pair<Integer,Point> pair = orderedXProgress.get(i);
                Point oldPosition = pair.getValue();
                
                // start by checking random direction
                Point newPosition = new Point(oldPosition.x + dx, oldPosition.y + dy);
                Pair<Integer,Point> move = new Pair<Integer,Point>(pair.getKey(), newPosition);
                if (Utilities.check_validity(move, playerPieces, opponentPieces)) return move;

                // checking opposite direction (but with same angle)
                newPosition = new Point(oldPosition.x + dx, oldPosition.y - dy);
                move = new Pair<Integer,Point>(pair.getKey(), newPosition);
                if (Utilities.check_validity(move, playerPieces, opponentPieces)) return move;
            }
        }

        return null;        
    }
    
    //getInSoftEndZoneList(orderedXProgress, isPlayer1)
    //private Pair<Integer, Point> getInSoftEndZoneList( List<Pair<Integer,Point>> orderedXProgress, boolean isPlayer1 ) {
        
    //}

    private Pair<Integer, Point> getNewForwardPieceMove( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1 ) {
            // get all our pieces from closest to farthest
    //        List<Pair<Integer,Point>> orderedXProgress = isFront? rankXProgressOutsideEndzone(playerPieces, isPlayer1):
    //                                                                            rankXProgress(playerPieces, isPlayer1);

    //          ORIGINAL
        List<Pair<Integer,Point>>         orderedXProgress = rankXProgressOutsideEndzone(playerPieces, isPlayer1);
        if (orderedXProgress.size() == 0 || Math.random() < 0.1) orderedXProgress = rankXProgress(playerPieces, isPlayer1);

        for (int angle = 0; angle < 180; angle++) {
            // choose random direction
            double theta = ((random.nextDouble() > 0.5)? -1 : 1) *angle *Math.PI/180;
            double dx = Math.cos(theta) * (isPlayer1? -1 : 1) * diameter_piece, dy = Math.sin(theta) * diameter_piece;

            for (int i = 0; i < orderedXProgress.size() - 1; i++) {
                Pair<Integer,Point> pair = orderedXProgress.get(i);
                Point oldPosition = pair.getValue();
                
                // start by checking random direction
                Point newPosition = new Point(oldPosition.x + dx, oldPosition.y + dy);
                Pair<Integer,Point> move = new Pair<Integer,Point>(pair.getKey(), newPosition);
                if (Utilities.check_validity(move, playerPieces, opponentPieces)) return move;

                // checking opposite direction (but with same angle)
                newPosition = new Point(oldPosition.x + dx, oldPosition.y - dy);
                move = new Pair<Integer,Point>(pair.getKey(), newPosition);
                if (Utilities.check_validity(move, playerPieces, opponentPieces)) return move;
            }
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

    
    // ###################################### NEEDS TO BE BUILT
      // add moves to the move list and at the same time updates players
      private void updateMoveList(HashMap<Integer, Point> playerPieces, Pair<Integer, Point> move, List<Pair<Integer, Point>> moves ) {
        if (move != null) moves.add(move);
        // update players
        //System.out.println("playerPieces.get(move.getKey()).x BEFORE: " + playerPieces.get(move.getKey()).x);
        Point piecePosition = playerPieces.get(move.getKey());
        Point new_position = move.getValue();
        piecePosition.x = new_position.x;
        piecePosition.y = new_position.y;

        //playerPieces.get(move.getKey()).x = move.getValue().x;
        //playerPieces.get(move.getKey()).y = move.getValue().y;
        //System.out.println("playerPieces.get(move.getKey()).x AFTER: " + playerPieces.get(move.getKey()).x);
    }
  
  
    public List<Pair<Integer, Point>> getSmallNMove(
        List<Pair<Integer, Point>> moves, Integer numMoves ) {

            HashMap<Integer, Point> playerPieces = this.mPlayer.playerPieces;
            HashMap<Integer, Point> opponentPieces = this.mPlayer.opponentPieces;
            boolean isPlayer1 = this.mPlayer.isPlayer1;

            if (n < 12) {
                while (moves.size() < numMoves) {
                    Pair<Integer, Point> move = null;
                    while (rankXProgressOutsideEndzone(playerPieces, isPlayer1).size() > n - n/5) { //n - n/3 ************ CHANGE
                        //System.out.println("here!");
                        move = getNewForwardPieceMove(playerPieces, opponentPieces, isPlayer1);
                        updateMoveList(playerPieces, move, moves);
                    }
                    move = getBackwardPieceMove(playerPieces, opponentPieces, isPlayer1);
                    updateMoveList(playerPieces, move, moves);


                    // move the closest piece that can move forward
            //        move = getForwardMove(playerPieces, opponentPieces, isPlayer1, true);
            //        updateMoveList(playerPieces, move, moves);

                    // move the farthest away piece that can move forward
            //        move = getForwardMove(playerPieces, opponentPieces, isPlayer1, false);
            //        updateMoveList(playerPieces, move, moves);

                    // choose best forwardish direction as next option 
                    //             move = getBestForwardishMove(playerPieces, opponentPieces, isPlayer1, true);
                    //             if (move != null) moves.add(move);             
                    //             move = getBestForwardishMove(playerPieces, opponentPieces, isPlayer1, false);
                    //             if (move != null) moves.add(move);             

                    // choose valid random forwardish to less forward directions as next options
                    // Can first optimize by looking at only angles you haven't already looked at
                    // Ideally, would have an improved function [ getBestForwardishMove(), not yet built ] that finds the best move
            //         move = getRandomMove(playerPieces, opponentPieces, isPlayer1, 90);
            //        updateMoveList(playerPieces, move, moves);
            //        move = getRandomMove(playerPieces, opponentPieces, isPlayer1, 180);
            //        updateMoveList(playerPieces, move, moves);             

            //        move = getRandomMove(playerPieces, opponentPieces, isPlayer1, 270);
            //        updateMoveList(playerPieces, move, moves);             
                }
            }
            // for 12 < n <= 15 // just backward move (to move wall) since you're implementing a runner wall
            else while (moves.size() < numMoves) {
                Pair<Integer, Point> move = null;
                move = getBackwardPieceMove(playerPieces, opponentPieces, isPlayer1);
                updateMoveList(playerPieces, move, moves);          
            }

            return moves;
       }
       
}
