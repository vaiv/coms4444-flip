package flip.g4;

import java.util.HashMap;
import javafx.util.Pair;

import flip.sim.Log;
import flip.sim.Point;

import flip.g4.Player;
import flip.g4.Utilities;

public class PieceStore{
    public Integer n;
    public Player mPlayer;
    public Double diameter;
    public boolean isPlayer1;

    public HashMap<Integer, Point> myPieces;
    public HashMap<Integer, Point> oppPieces;

    HashMap<Integer, Pair<Point, Point>> movedPieces;

    public PieceStore(Player mPlayer, HashMap<Integer, Point> pieces){
        this.n         = mPlayer.n;
        this.mPlayer   = mPlayer;
        this.isPlayer1 = mPlayer.isPlayer1;
        this.diameter  = mPlayer.diameter_piece;

        this.myPieces  = new HashMap<>();
        this.oppPieces = new HashMap<>();

        for(Integer idx: pieces.keySet()){
            Point loc = pieces.get(idx);
            this.myPieces.put(idx, new Point(loc));
            this.oppPieces.put(idx, new Point(loc.x * -1, loc.y));
        }
    }

    public void movePiece(Pair<Integer, Point> move){
        this.myPieces.replace(move.getKey(), move.getValue());
    }

    public void updatePieces(
        HashMap<Integer, Point> playerPieces,
        HashMap<Integer, Point> opponentPieces){
        
        this.movedPieces = new HashMap<Integer, Pair<Point, Point>>();
        for(Integer idx: this.oppPieces.keySet()){
            Point oldLoc = this.oppPieces.get(idx);
            Point newLoc = opponentPieces.get(idx);

            if(!oldLoc.equals(newLoc)){
                movedPieces.put(idx, new Pair<Point, Point>(oldLoc, newLoc));
                this.oppPieces.replace(idx, newLoc);
            }
        }

        for(Integer idx: this.myPieces.keySet()){
            Point oldLoc = this.myPieces.get(idx);
            Point newLoc= playerPieces.get(idx);

            if(oldLoc.x != newLoc.x || oldLoc.y != newLoc.y){
                // Log.log(String.format("INCORRECT POSITION %d [%s] [%s]",
                    // idx, oldLoc.toString(), newLoc.toString())
                // );

                this.myPieces.replace(idx, newLoc);
            }
        }
    }
}