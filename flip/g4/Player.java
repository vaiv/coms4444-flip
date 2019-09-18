// g4 deliverable for 9/18
package flip.g4;

 

import java.util.Arrays;
import java.util.ArrayList;
import flip.sim.Board;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import flip.sim.Log;
import javafx.util.Pair;
import flip.sim.Point;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.lang.*;




// ******************    Player Class   ******************
public class Player implements flip.sim.Player
{
    private int seed = 42;
    private Random random;
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
        int n, double t, boolean isPlayer1, double diameter_piece)
    {
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
        boolean isPlayer1 )
    {
        if (playerPieces.size() < 12) return getMoves12(numMoves, playerPieces, opponentPieces, isPlayer1);

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
                Pair<Integer,Point> move = getForwardPieceMove(playerPieces, opponentPieces, isPlayer1);
                // if move is available, and piece is the runner
//                if (move != null && ) moves.add(move);
                if (move != null) moves.add(move);
                //this.mRunnerStrategy.getRunnerMove(moves, numMoves);
            } catch (Exception e) {
                Log.log(e.toString());
            }
        }
        return moves;
    }
    
    public List<Pair<Integer, Point>> getMoves12( Integer numMoves, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1 ) {
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
}

// ******************    HungarianAlgorithm Class   ****************** 

/* Copyright (c) 2012 Kevin L. Stern
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * An implementation of the Hungarian algorithm for solving the assignment
 * problem. An instance of the assignment problem consists of a number of
 * workers along with a number of jobs and a cost matrix which gives the cost of
 * assigning the i'th worker to the j'th job at position (i, j). The goal is to
 * find an assignment of workers to jobs so that no job is assigned more than
 * one worker and so that no worker is assigned to more than one job in such a
 * manner so as to minimize the total cost of completing the jobs.
 * <p>
 * 
 * An assignment for a cost matrix that has more workers than jobs will
 * necessarily include unassigned workers, indicated by an assignment value of
 * -1; in no other circumstance will there be unassigned workers. Similarly, an
 * assignment for a cost matrix that has more jobs than workers will necessarily
 * include unassigned jobs; in no other circumstance will there be unassigned
 * jobs. For completeness, an assignment for a square cost matrix will give
 * exactly one unique worker to each job.
 * <p>
 * 
 * This version of the Hungarian algorithm runs in time O(n^3), where n is the
 * maximum among the number of workers and the number of jobs.
 * 
 * @author Kevin L. Stern
 */
class HungarianAlgorithm {
  private final double[][] costMatrix;
  private final int rows, cols, dim;
  private final double[] labelByWorker, labelByJob;
  private final int[] minSlackWorkerByJob;
  private final double[] minSlackValueByJob;
  private final int[] matchJobByWorker, matchWorkerByJob;
  private final int[] parentWorkerByCommittedJob;
  private final boolean[] committedWorkers;

  /**
   * Construct an instance of the algorithm.
   * 
   * @param costMatrix
   *          the cost matrix, where matrix[i][j] holds the cost of assigning
   *          worker i to job j, for all i, j. The cost matrix must not be
   *          irregular in the sense that all rows must be the same length; in
   *          addition, all entries must be non-infinite numbers.
   */
  public HungarianAlgorithm(double[][] costMatrix) {
    this.dim = Math.max(costMatrix.length, costMatrix[0].length);
    this.rows = costMatrix.length;
    this.cols = costMatrix[0].length;
    this.costMatrix = new double[this.dim][this.dim];
    for (int w = 0; w < this.dim; w++) {
      if (w < costMatrix.length) {
        if (costMatrix[w].length != this.cols) {
          throw new IllegalArgumentException("Irregular cost matrix");
        }
        for (int j = 0; j < this.cols; j++) {
          if (Double.isInfinite(costMatrix[w][j])) {
            throw new IllegalArgumentException("Infinite cost");
          }
          if (Double.isNaN(costMatrix[w][j])) {
            throw new IllegalArgumentException("NaN cost");
          }
        }
        this.costMatrix[w] = Arrays.copyOf(costMatrix[w], this.dim);
      } else {
        this.costMatrix[w] = new double[this.dim];
      }
    }
    labelByWorker = new double[this.dim];
    labelByJob = new double[this.dim];
    minSlackWorkerByJob = new int[this.dim];
    minSlackValueByJob = new double[this.dim];
    committedWorkers = new boolean[this.dim];
    parentWorkerByCommittedJob = new int[this.dim];
    matchJobByWorker = new int[this.dim];
    Arrays.fill(matchJobByWorker, -1);
    matchWorkerByJob = new int[this.dim];
    Arrays.fill(matchWorkerByJob, -1);
  }

  /**
   * Compute an initial feasible solution by assigning zero labels to the
   * workers and by assigning to each job a label equal to the minimum cost
   * among its incident edges.
   */
  protected void computeInitialFeasibleSolution() {
    for (int j = 0; j < dim; j++) {
      labelByJob[j] = Double.POSITIVE_INFINITY;
    }
    for (int w = 0; w < dim; w++) {
      for (int j = 0; j < dim; j++) {
        if (costMatrix[w][j] < labelByJob[j]) {
          labelByJob[j] = costMatrix[w][j];
        }
      }
    }
  }

  /**
   * Execute the algorithm.
   * 
   * @return the minimum cost matching of workers to jobs based upon the
   *         provided cost matrix. A matching value of -1 indicates that the
   *         corresponding worker is unassigned.
   */
  public int[] execute() {
    /*
     * Heuristics to improve performance: Reduce rows and columns by their
     * smallest element, compute an initial non-zero dual feasible solution and
     * create a greedy matching from workers to jobs of the cost matrix.
     */
    reduce();
    computeInitialFeasibleSolution();
    greedyMatch();

    int w = fetchUnmatchedWorker();
    while (w < dim) {
      initializePhase(w);
      executePhase();
      w = fetchUnmatchedWorker();
    }
    int[] result = Arrays.copyOf(matchJobByWorker, rows);
    for (w = 0; w < result.length; w++) {
      if (result[w] >= cols) {
        result[w] = -1;
      }
    }
    return result;
  }

  /**
   * Execute a single phase of the algorithm. A phase of the Hungarian algorithm
   * consists of building a set of committed workers and a set of committed jobs
   * from a root unmatched worker by following alternating unmatched/matched
   * zero-slack edges. If an unmatched job is encountered, then an augmenting
   * path has been found and the matching is grown. If the connected zero-slack
   * edges have been exhausted, the labels of committed workers are increased by
   * the minimum slack among committed workers and non-committed jobs to create
   * more zero-slack edges (the labels of committed jobs are simultaneously
   * decreased by the same amount in order to maintain a feasible labeling).
   * <p>
   * 
   * The runtime of a single phase of the algorithm is O(n^2), where n is the
   * dimension of the internal square cost matrix, since each edge is visited at
   * most once and since increasing the labeling is accomplished in time O(n) by
   * maintaining the minimum slack values among non-committed jobs. When a phase
   * completes, the matching will have increased in size.
   */
  protected void executePhase() {
    while (true) {
      int minSlackWorker = -1, minSlackJob = -1;
      double minSlackValue = Double.POSITIVE_INFINITY;
      for (int j = 0; j < dim; j++) {
        if (parentWorkerByCommittedJob[j] == -1) {
          if (minSlackValueByJob[j] < minSlackValue) {
            minSlackValue = minSlackValueByJob[j];
            minSlackWorker = minSlackWorkerByJob[j];
            minSlackJob = j;
          }
        }
      }
      if (minSlackValue > 0) {
        updateLabeling(minSlackValue);
      }
      parentWorkerByCommittedJob[minSlackJob] = minSlackWorker;
      if (matchWorkerByJob[minSlackJob] == -1) {
        /*
         * An augmenting path has been found.
         */
        int committedJob = minSlackJob;
        int parentWorker = parentWorkerByCommittedJob[committedJob];
        while (true) {
          int temp = matchJobByWorker[parentWorker];
          match(parentWorker, committedJob);
          committedJob = temp;
          if (committedJob == -1) {
            break;
          }
          parentWorker = parentWorkerByCommittedJob[committedJob];
        }
        return;
      } else {
        /*
         * Update slack values since we increased the size of the committed
         * workers set.
         */
        int worker = matchWorkerByJob[minSlackJob];
        committedWorkers[worker] = true;
        for (int j = 0; j < dim; j++) {
          if (parentWorkerByCommittedJob[j] == -1) {
            double slack = costMatrix[worker][j] - labelByWorker[worker]
                - labelByJob[j];
            if (minSlackValueByJob[j] > slack) {
              minSlackValueByJob[j] = slack;
              minSlackWorkerByJob[j] = worker;
            }
          }
        }
      }
    }
  }

  /**
   * 
   * @return the first unmatched worker or {@link #dim} if none.
   */
  protected int fetchUnmatchedWorker() {
    int w;
    for (w = 0; w < dim; w++) {
      if (matchJobByWorker[w] == -1) {
        break;
      }
    }
    return w;
  }

  /**
   * Find a valid matching by greedily selecting among zero-cost matchings. This
   * is a heuristic to jump-start the augmentation algorithm.
   */
  protected void greedyMatch() {
    for (int w = 0; w < dim; w++) {
      for (int j = 0; j < dim; j++) {
        if (matchJobByWorker[w] == -1 && matchWorkerByJob[j] == -1
            && costMatrix[w][j] - labelByWorker[w] - labelByJob[j] == 0) {
          match(w, j);
        }
      }
    }
  }

  /**
   * Initialize the next phase of the algorithm by clearing the committed
   * workers and jobs sets and by initializing the slack arrays to the values
   * corresponding to the specified root worker.
   * 
   * @param w
   *          the worker at which to root the next phase.
   */
  protected void initializePhase(int w) {
    Arrays.fill(committedWorkers, false);
    Arrays.fill(parentWorkerByCommittedJob, -1);
    committedWorkers[w] = true;
    for (int j = 0; j < dim; j++) {
      minSlackValueByJob[j] = costMatrix[w][j] - labelByWorker[w]
          - labelByJob[j];
      minSlackWorkerByJob[j] = w;
    }
  }

  /**
   * Helper method to record a matching between worker w and job j.
   */
  protected void match(int w, int j) {
    matchJobByWorker[w] = j;
    matchWorkerByJob[j] = w;
  }

  /**
   * Reduce the cost matrix by subtracting the smallest element of each row from
   * all elements of the row as well as the smallest element of each column from
   * all elements of the column. Note that an optimal assignment for a reduced
   * cost matrix is optimal for the original cost matrix.
   */
  protected void reduce() {
    for (int w = 0; w < dim; w++) {
      double min = Double.POSITIVE_INFINITY;
      for (int j = 0; j < dim; j++) {
        if (costMatrix[w][j] < min) {
          min = costMatrix[w][j];
        }
      }
      for (int j = 0; j < dim; j++) {
        costMatrix[w][j] -= min;
      }
    }
    double[] min = new double[dim];
    for (int j = 0; j < dim; j++) {
      min[j] = Double.POSITIVE_INFINITY;
    }
    for (int w = 0; w < dim; w++) {
      for (int j = 0; j < dim; j++) {
        if (costMatrix[w][j] < min[j]) {
          min[j] = costMatrix[w][j];
        }
      }
    }
    for (int w = 0; w < dim; w++) {
      for (int j = 0; j < dim; j++) {
        costMatrix[w][j] -= min[j];
      }
    }
  }

  /**
   * Update labels with the specified slack by adding the slack value for
   * committed workers and by subtracting the slack value for committed jobs. In
   * addition, update the minimum slack values appropriately.
   */
  protected void updateLabeling(double slack) {
    for (int w = 0; w < dim; w++) {
      if (committedWorkers[w]) {
        labelByWorker[w] += slack;
      }
    }
    for (int j = 0; j < dim; j++) {
      if (parentWorkerByCommittedJob[j] != -1) {
        labelByJob[j] -= slack;
      } else {
        minSlackValueByJob[j] -= slack;
      }
    }
  }
}

// ******************    PieceStore Class   ****************** 
class PieceStore{
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

// ****************** RunnerStrategy Class ****************** 
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
            Point runnerLoc = this.mPlayer.playerPieces.get(this.runner);

             Pair<Integer, Point> move = new Pair<Integer, Point>(this.runner, 
                new Point(runnerLoc.x+((this.isPlayer1)?-2:2), runnerLoc.y
            ));

             if(Utilities.check_validity(move,
                this.mPlayer.playerPieces, this.mPlayer.opponentPieces))
                moves.add(move);
        }

         else if (this.status == RunnerStatus.RUNNER_PASSED_WALL){
            // Dash to end
        }

         RunnerStatus prev = this.status;
        this.updateRunnerStatus();
        if (this.status != prev){
            Log.log("RUNNER STATUS UPDATED FROM " + prev.toString() + 
                    " TO " + this.status.toString());
        }
    }
}

// ******************    WallStrategy Class   ******************
// Abstracted class for wall building
class WallStrategy{
    private int debugCount = 0;
    // Store details about the game
    private Player mPlayer;

     // Wall co-ordinates and number of pieces
    public int       numWallPieces;
    public double    wallXLocation;
    public Point[]   idealWallLocations;

     // Index of pieces that can build wall the fastest
    private Integer[] fastestWallBuilders;

     // Status flags for wall completion
    public boolean   WALL_COMPLETED;
    // private Integer[] movesLeft;
    public Integer   totalMovesLeft;
    public Integer   runner;

     // Statistics for fun
    public Integer numMovesRequired;

     public WallStrategy(Player mPlayer, HashMap<Integer, Point> pieces){
        this.mPlayer = mPlayer;
        this.numMovesRequired = 0;
        this.calculateWallPositions();

         // Calculate number of ideal runners
        HashSet<Integer> runners = new HashSet<>();
        Pair<Integer, Integer[]> p = this.calculateWallStrategy(pieces,runners);

         List<Integer> idxs = Utilities.rankXProgress(pieces, mPlayer.isPlayer1);
        runners.add(idxs.get(0));
        Pair<Integer, Integer[]> q = this.calculateWallStrategy(pieces,runners);

         if (p.getKey() - q.getKey() <= 5){ // Arbitrary decision
            this.totalMovesLeft = q.getKey();
            this.fastestWallBuilders = q.getValue();
            this.runner = idxs.get(0); // TODO
            // this.mPlayer.myPlayers.runners.add(); Add runners
        } else {
            this.totalMovesLeft = p.getKey();
            this.fastestWallBuilders = p.getValue();
        }

         Point[] tmp = new Point[this.numWallPieces];
        for(int i=0; i<this.numWallPieces; i++){
            tmp[i] = this.mPlayer.playerPieces.get(this.fastestWallBuilders[i]);
        }

         // Log.log(Arrays.toString(tmp));
    }

     /**
     * Initializes this.numWallPieces and idealWallLocations
     */
    private void calculateWallPositions(){
        // Generate co-ordinates for ideal wall
        this.numWallPieces      = 11;
        this.wallXLocation      = (this.mPlayer.isPlayer1)? 21.5: -21.5;
        this.idealWallLocations = new Point[this.numWallPieces];

         for(int i=0; i<this.numWallPieces; i++){
            // Calculations done assuming perfect placement 
            // For 1cm coins on 40cm board
            this.idealWallLocations[i] = new Point(
                this.wallXLocation,
                2*i*Math.sqrt(3) + Math.sqrt(3) - 19
            );
        }

     }

     /**
     * Figure out the distance from each coin to ideal wall locations
     * 
     * @param pieces
     * @return
     */
    private double[][] calculateCostMatrix(
        HashMap<Integer, Point> mPieces, Set<Integer> ignored){

         double[][] costMatrix = new double[this.mPlayer.n][this.numWallPieces];
        for (int i=0; i < this.mPlayer.n; i++){
            for (int j=0; j < this.numWallPieces; j++)
                if (ignored.contains(i))
                    costMatrix[i][j] = 40.0;
                else 
                    costMatrix[i][j] = Utilities.numMoves(
                        this.idealWallLocations[j], mPieces.get(i));
        }
        return costMatrix;
    }

     /**
     * 
     * @param playerPieces
     * @param ignored
     * @return
     */
    private Pair<Integer, Integer[]> calculateWallStrategy(
        HashMap<Integer, Point> mPieces, Set<Integer> ignored){

         double[][] costMatrix = this.calculateCostMatrix(mPieces, ignored);

         // Solve hungarian algorithm for minimum placement in O(n^3)
        HungarianAlgorithm solver = new HungarianAlgorithm(costMatrix);
        int[] solution = solver.execute(); //TODO: Change to Integer[]

         // Parse calculated solution and expected wall cost
        Double    wallCost = 0.0;
        Integer[] bestPieceMatching = new Integer[11];

         for(int pieceID=0; pieceID < this.mPlayer.n; pieceID++){
            if(solution[pieceID] >= 0){
                wallCost += costMatrix[pieceID][solution[pieceID]];
                bestPieceMatching[solution[pieceID]] = pieceID;
            }
        }

         return new Pair<Integer, Integer[]>(
            wallCost.intValue(), bestPieceMatching);
    }

     /**
     * 
     * @param opponentPieces
     * @return
     */
    private Integer[] getWallPriority(HashMap<Integer, Point> opponentPieces){
        Integer[] distanceToWall = new Integer[11];
        for(int i=0; i<11; i++)
            distanceToWall[i] = Integer.MAX_VALUE;

         // Iterate through opponent pieces to prioritize wall
        for(int j=0; j<this.mPlayer.n; j++){
            for (int i=0; i<11; i++){
                Double numMoves = Utilities.numMoves(
                    opponentPieces.get(j), this.idealWallLocations[i]);

                 if (numMoves < distanceToWall[i])
                    distanceToWall[i] = numMoves.intValue();
            }
        }

         return Utilities.argsort(distanceToWall);
    }

     public void getWallMove(List<Pair<Integer, Point>> moves, Integer numMoves){
        this.debugCount ++;

         int idx = 0;
        Integer[] idxPriority = this.getWallPriority(this.mPlayer.opponentPieces);

         boolean flag = true;
        while (moves.size() < numMoves && idx < this.numWallPieces){
            Integer pieceID = idxPriority[idx];

             Pair<Integer, Point> move = Utilities.getNextMove(
                this.mPlayer.playerPieces.get(this.fastestWallBuilders[pieceID]),
                this.idealWallLocations[pieceID],
                this.fastestWallBuilders[pieceID],
                this.mPlayer.playerPieces,
                this.mPlayer.opponentPieces
            );

             if(move != null){
                moves.add(move);
                // movesLeft[pieceID]--;
                this.totalMovesLeft--;
                this.mPlayer.playerPieces.replace(
                    this.fastestWallBuilders[pieceID], move.getValue());
                // TODO: Update player location store
            } else idx++;
        }

         this.numMovesRequired += moves.size();
        if (moves.size()==0) {
            this.WALL_COMPLETED = true;
            Log.log(String.format(
                "WALL COMPLETED in %d moves", this.numMovesRequired));
        }
    }
}

// ******************    UtilitiesClass   ****************** 
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
}
