package flip.g4;

import java.util.HashMap;
import javafx.util.Pair;

import flip.sim.Point;

public class PieceStore{
    private HashMap<Integer, Point> pieceList;

    public PieceStore(){}
    public PieceStore(HashMap<Integer, Point> pieces){
        this.pieceList = pieces;
    }

    public HashMap<Integer, Pair<Point, Point>> findMovedPieces(
        HashMap<Integer, Point> pieces){
        
        HashMap<Integer, Pair<Point, Point>> movedPieces = new HashMap<>();
        for(Integer idx: this.pieceList.keySet()){
            Point oldLoc = this.pieceList.get(idx);
            Point newLoc = pieces.get(idx);

            if(!oldLoc.equals(newLoc)){
                movedPieces.put(idx, new Pair<>(oldLoc, newLoc));
                this.pieceList.replace(idx, newLoc);
            }
        }
        return movedPieces;
    }
}