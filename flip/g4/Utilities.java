package flip.g4;

import javafx.util.Pair; 

import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;

import flip.sim.Log;
import flip.sim.Board;
import flip.sim.Point;

class Utilities{
    public static Double numMoves(Point a, Point b){
        double dist = Board.getdist(a, b);
        return Board.almostEqual(dist, 2) ? 1: Math.max(2, Math.ceil(dist/2));
    }

    /**
     * Returns indexes of most advanced pieces
     * @param pieces
     * @param arePiecesPlayer1
     * @return
     */
    public static List<Integer> rankXProgress(
        HashMap<Integer, Point> pieces, boolean arePiecesPlayer1 ) {

        int sign = arePiecesPlayer1? 1 : -1;
        List<Integer> indices = new ArrayList<>(pieces.keySet());
        indices.sort(new Comparator<Integer>() {
            public int compare( Integer i1, Integer i2 ) {
                return (pieces.get(i1).x > pieces.get(i2).x)? sign : -sign;
            }
        });
        return indices;
    }
    
    public static Integer[] argsort(Integer[] distanceToWall){
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

    public static boolean check_validity( Pair<Integer, Point> move,
        HashMap<Integer, Point> playerPieces,
        HashMap<Integer, Point> opponentPieces ) {

        // Check if move has been initialized
        if (move.getKey() == null || move.getValue() == null)
            return false;

        // Check if distance is 2
        if(!Board.almostEqual(2, 
            Board.getdist(playerPieces.get(move.getKey()), move.getValue())))
            return false;

        // check for collisions
        if (Board.check_collision(playerPieces, move) || 
            Board.check_collision(opponentPieces, move) )
            return false;
        
        // check within bounds
        return Board.check_within_bounds(move);
    }

    public static Pair<Integer, Point> getNextMove(Point a, Point b, 
        Integer pieceID,
        HashMap<Integer, Point> playerPieces,
        HashMap<Integer, Point> opponentPieces){

        Pair<Integer, Point> move;
        double dist = Board.getdist(a, b);
        
        if (Board.almostEqual(dist, 0))
            return null;

        else if (Board.almostEqual(dist, 2)){
            move = new Pair<Integer, Point>(pieceID, b);
            if(check_validity(move, playerPieces, opponentPieces))
                return move;
            else
                return null;
        }
        
        else if (dist < 4) {
            double x1 = 0.5 * (b.x + a.x);
            double y1 = 0.5 * (b.y + a.y);

            double sqrt_const = Math.sqrt(16/(dist*dist)-1) / 2;
            double x2 = sqrt_const * (b.y - a.y);
            double y2 = sqrt_const * (a.x - b.x);

            move = new Pair<Integer, Point>(pieceID, new Point(x1+x2, y1+y2));
            if(check_validity(move, playerPieces, opponentPieces)){
                return move;
            }
            
            move = new Pair<Integer, Point>(pieceID, new Point(x1-x2, y1-y2));
            if(check_validity(move, playerPieces, opponentPieces)){
                return move;
            }
            
            return null;
        }

        else{
            move = new Pair<Integer, Point>(pieceID,  new Point(
                a.x + 2 * (b.x - a.x) / dist,
                a.y + 2 * (b.y - a.y) / dist
            ));

            if(check_validity(move, playerPieces, opponentPieces))
                return move;

            return null;
        }
    }

    public static Pair<Integer, Point> getForwardMove(
        Integer pieceID, Player mPlayer) {
        
        double dx = (mPlayer.isPlayer1? -1 : 1) * 2.0;
        Point oldPosition = mPlayer.playerPieces.get(pieceID);

        Pair<Integer, Point> move = new Pair<Integer, Point>(pieceID, new Point(
            oldPosition.x + dx, oldPosition.y
        ));

        if (check_validity(move, mPlayer.playerPieces, mPlayer.opponentPieces))
            return move;
        return null;        
    }

    public static Pair<Integer, Point> getRandomMove(
        Integer pieceID, Player mPlayer, double spread) {
        
        double theta = (mPlayer.random.nextDouble() -0.5) * spread * Math.PI/180;
        double dx = Math.cos(theta) * (mPlayer.isPlayer1?-1:1) * mPlayer.diameter_piece;
        double dy = Math.sin(theta) * mPlayer.diameter_piece;
        Point oldPosition = mPlayer.playerPieces.get(pieceID);
        
        Pair<Integer, Point> move = new Pair<Integer, Point>(pieceID, new Point(
            oldPosition.x + dx, oldPosition.y + dy
        ));

        if (check_validity(move, mPlayer.playerPieces, mPlayer.opponentPieces))
            return move;
        return null;        
    }
}