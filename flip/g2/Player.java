package flip.g2;

import java.util.List;
import java.util.HashMap;
import javafx.util.Pair;
import java.util.ArrayList;

import flip.sim.Point;
import flip.sim.Board;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class Player implements flip.sim.Player {

    private final static double N_THRESHOLD = 11;
    private final static double GOAL_LINE = 21.5;

    private final static double WALL_POSITION = -22.0;
    private final static double WALL_HOLDING_PRIORITY = -1.0;
    private final static double WALL_FORMATION_PRIORITY = 2.0;
    private final static double RUNNER_PRIORITY = 4.0;
    private final static double WALL_BREAKER_PRIORITY = 0.0;
    private final static double REPLACEMENT_PRIORITY = 3.0;

    private int seed = 42;
    private boolean isPlayer1;
    private double greedyProb;
    private Integer n;
    private Double pieceDiameter;
    private DiscreteBoard dBoard;
    private Queue<Destination> destinations;
    private final Set<Integer> wallHoldingPieces;
    private final Set<Integer> wallFormationPieces;
    private Integer runnerPiece;

    public Player() {
        wallHoldingPieces = new HashSet<>();
        wallFormationPieces = new HashSet<>();
        runnerPiece = -1;
    }

    public HashMap<Integer, Point> flip(HashMap<Integer, Point> pieces) {
        HashMap<Integer, Point> flippedPieces = new HashMap<>();
        pieces.forEach((key, value) -> flippedPieces.put(key, new Point(-value.x, value.y)));
        return flippedPieces;
    }

    /**
     *
     * @param pieces initial location of the pieces for the player.
     * @param n number of pieces available.
     * @param t total turns available.
     * @param isPlayer1 true if this player is the first to move
     * @param pieceDiameter diameter of each piece
     */
    @Override
    public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isPlayer1, double pieceDiameter) {
        this.n = n;
        this.isPlayer1 = isPlayer1;
        this.pieceDiameter = pieceDiameter;
        this.dBoard = new DiscreteBoard(120, 40, 2);

        if (isPlayer1) {
            pieces = this.flip(pieces);
        }

        setupStrategy(pieces);
    }

    /**
     *
     * @param numMoves number of movements to return.
     * @param playerPieces location of this player's pieces.
     * @param opponentPieces location of the opponent's pieces.
     * @param isPlayer1 true if this player is/was the first one to move.
     * @return list of moves to play
     */
    @Override
    public List<Pair<Integer, Point>> getMoves(
            Integer numMoves,
            HashMap<Integer, Point> playerPieces,
            HashMap<Integer, Point> opponentPieces,
            boolean isPlayer1) {

        if (isPlayer1) {
            playerPieces = this.flip(playerPieces);
            opponentPieces = this.flip(opponentPieces);
        }

        List<Pair<Integer, Point>> moves = new ArrayList<>();

        detectWall(playerPieces, opponentPieces);

        for (int i = 0; i < numMoves; i++) {
            final Destination d = destinations.peek();
            if (d == null) {
                passARunner(playerPieces, opponentPieces);
                i--;
                continue;
            } else {
                //if the next location is a wall position - 
//                if (wallFormationPieces.contains(d.id)){
//                    ArrayList<Point> wallPositions = getWallPositions();
//                    //get new priorities and adjust based on that
//                    HashMap<Point, Double> wallPriorities = getWallPriorities(opponentPieces, wallPositions);
//                    double wallPriority = wallPriorities.get(d.position);
//                    Destination walldest = destinations.poll();
//                    destinations.add(new Destination(
//                        WALL_FORMATION_PRIORITY + wallPriority,
//                        walldest.id,
//                        walldest.position));
//                }
            }
            /*if (wallHoldingPieces.contains(d.id)) {
                destinations.poll();
                i--;
                continue;
            }*/
            //System.out.println(destinations);
            //System.out.println("*** " + d);
            final Pair<Integer, Point> piecePair = new Pair<>(d.id, playerPieces.get(d.id));
            dBoard.reset();
            dBoard.recordOpponentPieces(opponentPieces);
            dBoard.recordPlayerPiecesIdx(playerPieces);
            //dBoard.findPath(new Point(0, 0), new Point(5, 0));
            final List<Point> path = dBoard.findPath(piecePair.getValue(), d.position);
            if (path == null) {
                System.out.println("No path");
                destinations.poll();
                i--;
                continue;
            }
            double theta;
            if (getDistance(d.position, piecePair.getValue()) > 2 * pieceDiameter && n > N_THRESHOLD) {
                double directTheta = Math.atan2(d.position.y - piecePair.getValue().y, d.position.x - piecePair.getValue().x);
                if (!checkValidity(new Pair<>(piecePair.getKey(), this.getNewPosition(piecePair.getValue(), directTheta)), playerPieces, opponentPieces)) {
                    theta = this.getBestAngleToMove(piecePair, path.get(1), playerPieces, opponentPieces);
                } else {
                    theta = directTheta;
                }
            } else {
                theta = this.getBestAngleToMove(piecePair, d.position, playerPieces, opponentPieces);
            }
            final Pair<Integer, Point> move = this.getPositionToMove(piecePair, playerPieces, opponentPieces, theta);

            // lower the priority of destinations not making progress
            if (d.previousPosition != null && getDistance(d.previousPosition, move.getValue()) < pieceDiameter
                    && getDistance(d.position, move.getValue()) > 0.01) {
                destinations.poll();
                d.priority += 1;
                if (d.priority < 20) {
                    d.previousPosition = null;
                    destinations.offer(d);
                }
                i--;
                continue;
            }
            d.previousPosition = piecePair.getValue();
            moves.add(move);
            playerPieces.put(move.getKey(), move.getValue());
            updateQueue(playerPieces);
        }

        if (isPlayer1) {
            final List<Pair<Integer, Point>> reflectedMoves = new ArrayList<>();
            for (Pair<Integer, Point> move : moves) {
                final Pair<Integer, Point> rMove = new Pair<>(move.getKey(), new Point(-move.getValue().x, move.getValue().y));
                reflectedMoves.add(rMove);
            }
            moves = reflectedMoves;
        }
        //System.out.println("***" + moves);
        return moves;
    }

    //look through each wall piece, and then look down that
    //row and see if there is a nearby opponent piece
    //if there is, record distance to her, and move on
    //find the closest nearby opponent
    //return a hashmap from the wallposition to the priority it should have. 
    protected HashMap<Point, Double> getWallPriorities(HashMap<Integer, Point> opponentPieces, ArrayList<Point> wallPositions) {
        HashMap<Point, Double> wallPriorities = new HashMap<>();
        Point[] ySortedOpponents = opponentPieces.values().stream().sorted((p1, p2) -> Double.compare(p1.y, p2.y)).toArray(Point[]::new);
        Point bestPoint = wallPositions.get(0);
        int count = 0;
        for (Point wallPoint : wallPositions) {
            boolean found = false;
            int ptIdx = -1;
            boolean nodistance = false;
            while (found == false && ptIdx < (ySortedOpponents.length)) {
                ptIdx++;
                if (Math.abs(wallPoint.y - ySortedOpponents[ptIdx].y) < 2) {
                    found = true;
                } else if (ptIdx >= (ySortedOpponents.length)) {
                    nodistance = true;
                }
            }
            if (nodistance) {
                double distance = getDistance(wallPoint, ySortedOpponents[ptIdx]);
                wallPriorities.put(wallPoint, distance / 70.0 + (count / 1000));
            } else {
                //if there was no nearby opponent coin, make sure that piece is placed later.
                wallPriorities.put(wallPoint, (count + 3) / 20.0);
            }
            count++;
        }
        return wallPriorities;
    }

    protected Pair<Integer, List<Point>> findBestRunner(HashMap<Integer, Point> playerPieces,
            HashMap<Integer, Point> opponentPieces) {
        if (wallHoldingPieces.size() > 0) {
            //double minX = 120.0;
            //double cumY = 0.0;
            //for (int pidx : wallHoldingPieces) {
            //    minX = Math.min(playerPieces.get(pidx).x, minX);
            //    cumY = cumY + playerPieces.get(pidx).y;
            //}
            //Point target = new Point(minX - pieceDiameter, cumY / (double) wallHoldingPieces.size());
            final Point breachPoint = playerPieces.get(wallHoldingPieces.iterator().next());
            final Point target = new Point(breachPoint.x - pieceDiameter, breachPoint.y);
            HashMap<Integer, Point> filteredPieces = new HashMap<>();
            for (Entry<Integer, Point> p : playerPieces.entrySet()) {
                if (p.getValue().x < GOAL_LINE) {
                    filteredPieces.put(p.getKey(), p.getValue());
                }
            }
            return shortestPathToTarget(filteredPieces, opponentPieces, target);
        } else if (!wallFormationPieces.isEmpty()) {
            final Point target = new Point(Math.random() * 10 + 30, Math.random() * 40 - 20);
            return shortestPathToTarget(playerPieces, opponentPieces, target);
        }

        return null;
    }

    protected Pair<Integer, List<Point>> findBestReplacement(HashMap<Integer, Point> playerPieces,
            HashMap<Integer, Point> opponentPieces, Point target) {
        final HashMap<Integer, Point> playerPiecesNotOnWall = new HashMap<>();
        for (Integer i = 0; i < playerPieces.size(); i++) {
            if (!wallFormationPieces.contains(i)) {
                playerPiecesNotOnWall.put(i, playerPieces.get(i));
            }
        }
        //System.out.println("***Number of non wall pieces " + playerPiecesNotOnWall.size() + "/" + wallFormationPieces.size() + "/" + playerPieces.size());
        return shortestPathToTarget(playerPiecesNotOnWall, opponentPieces, new Point(target.x - pieceDiameter, target.y));
    }

    protected void passARunner(HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces) {
        final Pair<Integer, List<Point>> bestRunner = findBestRunner(playerPieces, opponentPieces);
        if (n <= N_THRESHOLD) {
            for (Entry<Integer, Point> p : playerPieces.entrySet()) {
                final Integer id = p.getKey();
                final Point position = p.getValue();
                if (position.x < GOAL_LINE) {
                    destinations.add(new Destination(Math.abs(position.x), id, new Point(GOAL_LINE, position.y + Math.random() * 5)));
                }
            }
        }
        if (bestRunner != null) {
            Pair<Integer, List<Point>> bestReplacement = findBestReplacement(playerPieces, opponentPieces,
                    playerPieces.get(bestRunner.getKey()));
            if (bestReplacement != null) {
                //System.out.println("Runner " + bestRunner);
                //System.out.println("Replacement " + bestReplacement);

                final Point runnerPos = playerPieces.get(bestRunner.getKey());

                /*createDestinationsFromPath(
                        bestReplacement.getKey(),
                        bestReplacement.getValue(),
                        REPLACEMENT_PRIORITY,
                        0.001
                );*/
                destinations.add(new Destination(
                        REPLACEMENT_PRIORITY,
                        bestReplacement.getKey(),
                        new Point(runnerPos.x - pieceDiameter, runnerPos.y)));
                destinations.add(new Destination(
                        (REPLACEMENT_PRIORITY + RUNNER_PRIORITY) / 2 + 0.01,
                        bestRunner.getKey(),
                        new Point(runnerPos.x + pieceDiameter, runnerPos.y)));
                destinations.add(new Destination(
                        (REPLACEMENT_PRIORITY + RUNNER_PRIORITY) / 2 + 0.02,
                        bestReplacement.getKey(),
                        new Point(runnerPos.x, runnerPos.y)));
                wallFormationPieces.add(bestReplacement.getKey());

                final Point goalLocation = new Point(Math.random() * 10 + 30, Math.random() * 40 - 20);
                if (this.isThereAWall(playerPieces, opponentPieces)) {
                    final Integer oldWallHolderId = wallHoldingPieces.iterator().next();
                    final Point breachPoint = playerPieces.get(oldWallHolderId);
                    final Point target = new Point(breachPoint.x - pieceDiameter, breachPoint.y);
                    destinations.add(new Destination(
                            (REPLACEMENT_PRIORITY + RUNNER_PRIORITY) / 2 + 0.03,
                            bestRunner.getKey(),
                            target));

                    wallHoldingPieces.remove(oldWallHolderId);
                    wallHoldingPieces.add(bestRunner.getKey());
                    destinations.add(new Destination(
                            RUNNER_PRIORITY + 1,
                            oldWallHolderId,
                            new Point(breachPoint.x + pieceDiameter, breachPoint.y)));

                    destinations.add(new Destination(
                            RUNNER_PRIORITY + 1.01,
                            bestRunner.getKey(),
                            new Point(breachPoint.x, breachPoint.y)));

                    destinations.add(new Destination(
                            RUNNER_PRIORITY + 1.02,
                            oldWallHolderId,
                            goalLocation));
                } else {
                    destinations.add(new Destination(
                            RUNNER_PRIORITY,
                            bestRunner.getKey(),
                            goalLocation));
                }
            }
        }
    }

    protected void createDestinationsFromPath(Integer piece, List<Point> path, double priority, double priorityIncrement) {
        for (Point p : path) {
            destinations.add(new Destination(priority, piece, p));
            priority += priorityIncrement;
        }
    }

    protected void setupStrategy(HashMap<Integer, Point> pieces) {
        final Comparator<Destination> dc = (Destination d1, Destination d2) -> d1.priority.compareTo(d2.priority);
        this.destinations = new PriorityQueue(n, dc);

        if (n > N_THRESHOLD) {
            // pick a runner
            HashMap<Integer, Point> cPieces = new HashMap<>(pieces);
            Integer runner = getCloser(new Point(20, 0), cPieces);
            runnerPiece = runner;
            cPieces.remove(runner);
            double runnerY = 0.5 * pieces.get(runner).y;
            destinations.add(new Destination(WALL_BREAKER_PRIORITY, runner, new Point(10, runnerY)));

            setupWall(cPieces, pieces);
        } else {
            for (Entry<Integer, Point> p : pieces.entrySet()) {
                Integer id = p.getKey();
                Point position = p.getValue();
                destinations.add(new Destination(Math.abs(position.x), id, new Point(GOAL_LINE, position.y)));
            }
        }
    }

    //choose pieces for wall positions
    //will iterate throught the wall positions and record the wall position with the nearest
    //piece, the piece that was nearest, remove these from both lists, and then
    //find the next
    protected void setupWall(
            HashMap<Integer, Point> cPieces,
            HashMap<Integer, Point> pieces) {

        ArrayList<Point> wallPositions = getWallPositions();
        final int nWallCoins = wallPositions.size();
        for (int i = 0; i < nWallCoins; i++) {
            double shortestDistance = Double.POSITIVE_INFINITY;
            //default to the third wall point and the closest coin to it
            Point wallPointToFill = wallPositions.get(0);
            Integer closest = getCloser(wallPointToFill, cPieces);

            for (Point wallPoint : wallPositions) {
                final Integer closestToHole = getCloser(wallPoint, cPieces);
                Point closestPiecePoint = pieces.get(closestToHole);
                double distance = getDistance(wallPoint, closestPiecePoint);
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    wallPointToFill = wallPoint;
                    closest = closestToHole;
                }
            }
            //System.out.println("selected wall position at: " + wallPointToFill);
            //need to adjust priority based on where the opponent piece is. 

            destinations.add(new Destination(WALL_FORMATION_PRIORITY + (i / 60.0), closest, wallPointToFill));
            cPieces.remove(closest);
            wallPositions.remove(wallPointToFill);
            wallFormationPieces.add(closest);
        }
    }

    //first need to create a list of wall positions
    protected ArrayList<Point> getWallPositions() {
        ArrayList<Point> wallPositions = new ArrayList<Point>();
        final double wallOffset = 40.0 / 11;
        for (int i = 0; i < 11; i++) {
            Point wallPoint = new Point(WALL_POSITION, wallOffset * (i + 0.5) - 20);
            wallPositions.add(wallPoint);
        }
        return wallPositions;
    }

    protected Integer getCloser(Point point, HashMap<Integer, Point> pieces) {
        double bestDistance = Double.POSITIVE_INFINITY;
        Integer closerPoint = null;
        for (Entry<Integer, Point> p : pieces.entrySet()) {
            final double d = getDistance(p.getValue(), point);
            if (d < bestDistance) {
                closerPoint = p.getKey();
                bestDistance = d;
            }
        }
        return closerPoint;
    }

    protected void updateQueue(HashMap<Integer, Point> playerPieces) {
        final Destination d = destinations.peek();
        final Point curPos = playerPieces.get(d.id);
        final double distance = Math.hypot(d.position.y - curPos.y, d.position.x - curPos.x);
        //System.out.println("***distance " + distance + " " + d.position + ", " + curPos);
        if (distance < pieceDiameter / 10) {
            //System.out.println("***Removing " + destinations.poll() + " distance " + distance);
            destinations.poll();
            if (d.priority == WALL_HOLDING_PRIORITY) {
                wallHoldingPieces.add(d.id);
            }
        }
    }

    protected Pair<Integer, List<Point>> shortestPathToTarget(HashMap<Integer, Point> playerPieces,
            HashMap<Integer, Point> opponentPieces, Point target) {
        dBoard.reset();
        dBoard.recordOpponentPieces(opponentPieces);
        dBoard.recordPlayerPiecesIdx(playerPieces);

        return dBoard.findClosestPiece(target);
    }

    protected void detectWall(HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces) {
        try {
            if (runnerPiece == null || runnerPiece == -1) {
                return;
            }
            dBoard.reset();
            dBoard.recordOpponentPieces(opponentPieces);
            Double crowdedX = dBoard.getCrowdedColumn(-22, 23);
            if (crowdedX != null) {
                //System.out.println("***Crowded " + crowdedX);
                Point p = playerPieces.get(runnerPiece);
                final double cY = dBoard.findBestHole(crowdedX, p.y, p.x);
                final Point blockPoint = new Point(crowdedX + pieceDiameter / 2, cY);
                //System.out.println("***" + cY);
                final Integer closerId = getCloser(blockPoint, playerPieces);
                if ((destinations.isEmpty() || destinations.peek().priority != WALL_HOLDING_PRIORITY && destinations.peek().id != runnerPiece) && wallHoldingPieces.isEmpty()) {
                    final Point closerPosition = playerPieces.get(closerId);
                    final Point altBlockPoint = new Point(closerPosition.x + pieceDiameter / 2, closerPosition.y);
                    //System.out.println("***Block wall, move " + closerId + " to " + blockPoint);
                    //destinations.add(new Destination(WALL_HOLDING_PRIORITY, closerId, 
                    //        Math.abs(crowdedX - closerPosition.x) > pieceDiameter / 2 ? blockPoint : altBlockPoint));
                    destinations.add(new Destination(WALL_HOLDING_PRIORITY, closerId, blockPoint));
                    wallHoldingPieces.add(closerId);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected boolean isThereAWall(HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces) {
        try {
            dBoard.reset();
            dBoard.recordOpponentPieces(opponentPieces);
            dBoard.recordOpponentPieces(playerPieces);
            return dBoard.isThereAFullColumn(WALL_POSITION + pieceDiameter, 25);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected boolean checkValidity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        boolean valid = true;

        // check if move is adjacent to previous position.
        if (!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), pieceDiameter)) {
            System.out.println("No Adjacent");
            return false;
        }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        if (!valid) {
            //System.out.println("Collision with own pieces");
        }
        valid = valid && !Board.check_collision(opponent_pieces, move);
        if (!valid) {
            //System.out.println("Collision with opponent pieces");
        }

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;

    }

    /**
     * Return the list of pieces that should be considered when deciding which
     * one to move
     *
     * @param playerPieces
     * @return A filtered list of pieces
     */
    protected HashMap<Integer, Point> filterPiecesToMove(HashMap<Integer, Point> playerPieces) {
        // filter out pieces that are already in the opponents area
        double min, max, eps = 1E-7;
        if (this.isPlayer1) {
            min = -60.0;
            max = -20.0;
        } else {
            min = 20.0;
            max = 60.0;
        }
        HashMap<Integer, Point> piecesToMove = new HashMap<>();
        playerPieces.entrySet().stream().filter((entry)
                -> (entry.getValue().x - this.pieceDiameter / 2 + eps < min || entry.getValue().x + this.pieceDiameter / 2 - eps > max)
        ).forEachOrdered((entry) -> {
            piecesToMove.put(entry.getKey(), entry.getValue());
        });
        return piecesToMove;
    }

    protected void alliedPiecesBehind(HashMap<Integer, Point> playerPieces, ArrayList<ArrayList<Double>> playerInputs) {
        Point[] xSorted = playerPieces.values().stream().sorted((p1, p2) -> Double.compare(p1.x, p2.x)).toArray(Point[]::new);
        for (int i = 0; i < playerPieces.size(); i++) {
            Point p = playerPieces.get(i);
            boolean pieceBehind = false;
            int first = 0;
            int last = playerPieces.size();
            int mid = (first + last) / 2;
            while (first <= last) {
                if (xSorted[mid].x + pieceDiameter < p.x) {
                    first = mid + 1;
                } else if (xSorted[mid] == p || xSorted[mid].x >= p.x) {
                    last = mid - 1;
                } else {
                    // found another piece in the range, next we go left and
                    // right checking if they collide in the y-axis
                    int j = mid;
                    do {
                        if (Math.abs(xSorted[j].y - p.y) <= pieceDiameter) {
                            pieceBehind = true;
                            break;
                        }
                        j++;
                    } while (xSorted[j].x < p.x);
                    j = mid--;
                    while (xSorted[j].x > p.x - pieceDiameter) {
                        if (Math.abs(xSorted[j].y - p.y) <= pieceDiameter) {
                            pieceBehind = true;
                            break;
                        }
                        j++;
                    }
                    break;
                }
                mid = (first + last) / 2;
            }
            playerInputs.get(i).add(pieceBehind ? 1.0 : -1.0);
        }
    }

    protected double getDistanceToTarget(Point point) {
        return Math.max(20 - point.x, 0.0);
    }

    /**
     * Computes the distances from each point to the target line in the board
     *
     * @param playerPieces
     * @return A map with the distance of each point passed
     */
    protected HashMap<Integer, Double> getDistancesToTarget(HashMap<Integer, Point> playerPieces) {
        HashMap<Integer, Double> distanceMap = new HashMap<>();
        playerPieces.forEach((key, value) -> distanceMap.put(key, Math.max(20 - value.x, 0.0)));
        return distanceMap;
    }

    /**
     * Given a piece decides to what position to move it (implicitly the angle)
     *
     * @param piece
     * @param playerPieces
     * @param opponentPieces
     * @param theta
     * @return the position where the piece will be after moving
     */
    protected Pair<Integer, Point> getPositionToMove(
            Pair<Integer, Point> piece, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, double theta) {
        double window = 0.0;

        while (window < Math.PI) {
            for (int i = -1; i < 2; i += 2) {
                double theta_adj = theta + i * window;
                Point newPos = getNewPosition(piece.getValue(), theta_adj);
                //System.out.println("Move " + piece.getKey() + " to " + newPos);
                Pair<Integer, Point> newMove = new Pair(piece.getKey(), newPos);
                if (this.checkValidity(newMove, playerPieces, opponentPieces)) {
                    return newMove;
                } else {
                    //System.out.println("No valid");
                }
            }
            window += 0.01;
        }

        return null;
    }

    private double getBestAngleToMove(
            Pair<Integer, Point> piece,
            Point tarPos,
            HashMap<Integer, Point> playerPieces,
            HashMap<Integer, Point> opponentPieces) {
        final Point curPos = piece.getValue();
        final double distance = getDistance(tarPos, curPos);
        final double angle = Math.atan2(tarPos.y - curPos.y, tarPos.x - curPos.x);

        if (Math.abs(pieceDiameter - distance) < 0.01 || distance >= 2 * pieceDiameter) {
            return angle;
        }

        final double oAngle = Math.acos(distance / 2 / pieceDiameter);

        final double theta1 = angle + oAngle;
        final double theta2 = angle - oAngle;

        Point newPos = getNewPosition(curPos, theta1);
        Pair<Integer, Point> move = new Pair<>(piece.getKey(), newPos);

        return checkValidity(move, playerPieces, opponentPieces) ? theta1 : theta2;
    }

    protected Point getNewPosition(Point pos, double angle) {
        final double deltaX = pieceDiameter * Math.cos(angle);
        final double deltaY = pieceDiameter * Math.sin(angle);
        return new Point(pos.x + deltaX, pos.y + deltaY);
    }

    protected double getDistance(Point p1, Point p2) {
        return Math.hypot(p2.y - p1.y, p2.x - p1.x);
    }
}

class Destination {

    Double priority;
    Integer id;
    Point position;
    Point previousPosition;

    public Destination(Double priority, Integer id, Point position) {
        this.priority = priority;
        this.id = id;
        this.position = position;
    }

    @Override
    public String toString() {
        return "Destination{" + "priority=" + priority + ", id=" + id + ", position=" + position + '}';
    }

}
