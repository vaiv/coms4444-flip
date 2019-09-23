package flip.g4;

import java.util.List;
import java.util.HashMap;
import javafx.util.Pair;

import flip.sim.Log;
import flip.sim.Board;
import flip.sim.Point;

import flip.g4.Player;
import flip.g4.Utilities;

enum RunnerStatus {
    RUNNER_SET,
    RUNNER_PASSED_WALL,
    RUNNER_BLOCKED,
    RUNNER_REACHED_END,
    RUNNER_NONE
}

// Abstracted class for wall building
class RunnerStrategy{

    private Player  mPlayer;
    private Integer runner;
    private boolean isPlayer1;

    public RunnerStatus status;

    public RunnerStrategy(Player mPlayer, HashMap<Integer, Point> pieces, Integer runner){
        this.runner    = runner;
        this.mPlayer   = mPlayer;
        this.isPlayer1 = mPlayer.isPlayer1;

        this.updateRunnerStatus();
        Log.log("RUNNER INITIALIZED WITH STATUS "+String.valueOf(this.status) + 
            " FOR PIECE " + String.valueOf(runner)
        );
    }

    private void updateRunnerStatus(){
        if(this.runner == null)
            this.status = RunnerStatus.RUNNER_NONE;
        else if(this.checkRunnerReachedEnd())
            this.status = RunnerStatus.RUNNER_REACHED_END;
        else if(this.checkRunnerPassedWall())
            this.status = RunnerStatus.RUNNER_PASSED_WALL;
        else
            this.status = RunnerStatus.RUNNER_SET;
    }
    
    private boolean checkRunnerPassedWall(){
        Point runnerLoc = this.mPlayer.playerPieces.get(this.runner);

        if(this.isPlayer1)
            return runnerLoc.x < this.mPlayer.mWallStrategy.wallXLocation - 2.0;
        else
            return runnerLoc.x > this.mPlayer.mWallStrategy.wallXLocation + 2.0;
    }

    private boolean checkRunnerReachedEnd(){
        Point runnerLoc = this.mPlayer.playerPieces.get(this.runner);
        return (this.isPlayer1) ? runnerLoc.x < -21.0 : runnerLoc.x > 21.0;
    }
    
    public void getRunnerMove(List<Pair<Integer, Point>> moves, Integer numMoves){
        if(this.status == RunnerStatus.RUNNER_SET){
            // Try passing wall
            Pair<Integer, Point> move;
            if((move = Utilities.getForwardMove(this.runner, this.mPlayer)) != null)
                moves.add(move);
        }

        else if (this.status == RunnerStatus.RUNNER_PASSED_WALL){
            // Simple runner strategy
            int prev = moves.size() - 1;

            while(moves.size() > prev && moves.size() < numMoves){
                Pair<Integer, Point> move;
                prev = moves.size() - 1;

                if((move = Utilities.getForwardMove(
                    this.runner, this.mPlayer)) != null) moves.add(move);
                
                else if((move = Utilities.getRandomMove(
                    this.runner, this.mPlayer, 90)) != null)  moves.add(move);

                else if((move = Utilities.getRandomMove(
                    this.runner, this.mPlayer, 180)) != null) moves.add(move);

                else if((move = Utilities.getRandomMove(
                    this.runner, this.mPlayer, 270)) != null) moves.add(move);
            }
        }

        RunnerStatus prev = this.status;
        this.updateRunnerStatus();
        if (this.status != prev){
            Log.log("RUNNER STATUS UPDATED FROM " + prev.toString() + 
                    " TO " + this.status.toString());
        }
    }
}
