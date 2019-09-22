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
class SuperSmallNStrategy{
    private int debugCount = 0;
    // Store details about the game
    private Player mPlayer;

    public Integer n;
    public Random random;
    public Double diameter_piece;

    public SuperSmallNStrategy(Player mPlayer, HashMap<Integer, Point> pieces){
        this.n = mPlayer.n;
        this.mPlayer = mPlayer;
        this.random  = mPlayer.random;
        this.diameter_piece = mPlayer.diameter_piece;
    }

    public List<Pair<Integer, Point>> getSuperSmallNMove(
        List<Pair<Integer, Point>> moves, Integer numMoves ) {

            HashMap<Integer, Point> playerPieces = this.mPlayer.playerPieces;
            HashMap<Integer, Point> opponentPieces = this.mPlayer.opponentPieces;
            boolean isPlayer1 = this.mPlayer.isPlayer1;

            // while (moves.size() < numMoves) {
                
            // }

            return moves;
       }
       
}
