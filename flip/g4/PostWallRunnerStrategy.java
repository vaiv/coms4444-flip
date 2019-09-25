package flip.g4;

import java.util.List;
import java.util.HashMap;
import javafx.util.Pair;

import flip.sim.Log;
import flip.sim.Board;
import flip.sim.Point;

import flip.g4.Player;
import flip.g4.Utilities;

// Abstracted class for wall building
class PostWallRunnerStrategy{

    private Player  mPlayer;
    public Integer runner;
    private boolean isPlayer1;

    public boolean JOURNEY_COMPLETE;

    public PostWallRunnerStrategy(Player mPlayer, Integer runner){
        this.runner    = runner;
        this.mPlayer   = mPlayer;
        this.isPlayer1 = mPlayer.isPlayer1;

        this.JOURNEY_COMPLETE = this.checkRunnerReachedEnd();
    }

    private boolean checkRunnerReachedEnd(){
        Point runnerLoc = this.mPlayer.pieceStore.myPieces.get(this.runner);
        return (this.isPlayer1) ? runnerLoc.x < -25.0 : runnerLoc.x > 25.0;
    }
    
    public void getRunnerMove(List<Pair<Integer, Point>> moves, Integer numMoves){
        int initial_move_count = moves.size();
        // Simple runner strategy
        int prev = moves.size() - 1;

        while(moves.size() > prev && moves.size() < numMoves){
            Pair<Integer, Point> move;
            prev = moves.size();

            if((move = Utilities.getForwardMove(
                this.runner, this.mPlayer)) != null){
                    this.mPlayer.pieceStore.movePiece(move);
                    moves.add(move);
                }
            
            else if((move = Utilities.getRandomMove(
                this.runner, this.mPlayer, 90)) != null) {
                    this.mPlayer.pieceStore.movePiece(move);
                    moves.add(move);
                }

            else if((move = Utilities.getRandomMove(
                this.runner, this.mPlayer, 180)) != null){
                    this.mPlayer.pieceStore.movePiece(move);
                    moves.add(move);
                }

            else if((move = Utilities.getRandomMove(
                this.runner, this.mPlayer, 270)) != null){
                    this.mPlayer.pieceStore.movePiece(move);
                    moves.add(move);
                }
                
            if((this.JOURNEY_COMPLETE = this.checkRunnerReachedEnd())) break;
        }
    }
}
