package flip.ghj;

import java.util.List;
import java.util.Random;
import java.util.HashMap;
import javafx.util.Pair;
import java.util.ArrayList;

import flip.sim.Point;
import flip.sim.Board;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Player implements flip.sim.Player {

    private int seed = 42;
    private Random random;
    private boolean isPlayer1;
    private Integer n;
    private Double pieceDiameter;

    public Player() {
        random = new Random(seed);
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
        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

        HashMap<Integer, Point> movablePieces = this.filterPiecesToMove(playerPieces);
        HashMap<Integer, Double> disToTarget = this.getDistanceToTarget(movablePieces, isPlayer1);
        HashMap<Integer, Double> disToObs = this.getDistanceToFirstObstacle(movablePieces, opponentPieces);
        HashMap<Integer, Double> scores = new HashMap<>();

        for (HashMap.Entry<Integer, Point> entry : movablePieces.entrySet()) {
            scores.put(entry.getKey(), 1 * disToTarget.get(entry.getKey()) - 0.5 * disToObs.get(entry.getKey()));
        }

        List<Integer> piecesToMove = this.pickPiecesToMove(numMoves, movablePieces, scores, opponentPieces);
        for (Integer id : piecesToMove) {
            Point newPosition = this.getPositionToMove(new Pair<>(id, movablePieces.get(id)), opponentPieces);
            Pair<Integer, Point> move = new Pair<>(id, newPosition);
            if (newPosition != null && this.checkValidity(move, playerPieces, opponentPieces)) {
                moves.add(move);
            }
        }

        // degrade to random agent
        int num_trials = 30;
        int i = 0;

        Integer[] mpIds = movablePieces.keySet().toArray(new Integer[0]);
        while (moves.size() != numMoves && i < num_trials) {
            //Integer piece_id = random.nextInt(mpIds.length);
            Integer piece_id = random.nextInt(n);

            Point curr_position = playerPieces.get(piece_id);
            Point new_position = new Point(curr_position);

            double theta = -Math.PI / 2 + Math.PI * random.nextDouble();
            double delta_x = pieceDiameter * Math.cos(theta);
            double delta_y = pieceDiameter * Math.sin(theta);

            Double val = (Math.pow(delta_x, 2) + Math.pow(delta_y, 2));
            // System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
            // Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

            new_position.x = isPlayer1 ? new_position.x - delta_x : new_position.x + delta_x;
            new_position.y += delta_y;
            Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);

            Double dist = Board.getdist(playerPieces.get(move.getKey()), move.getValue());
            // System.out.println("distance from previous position is " + dist.toString());
            // Log.record("distance from previous position is " + dist.toString());

            if (checkValidity(move, playerPieces, opponentPieces)) {
                moves.add(move);
            }
            i++;
        }

        return moves;
    }

    public boolean checkValidity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        boolean valid = true;

        // check if move is adjacent to previous position.
        if (!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), pieceDiameter)) {
            return false;
        }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

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
        for (HashMap.Entry<Integer, Point> entry : playerPieces.entrySet()) {
            if (entry.getValue().x - this.pieceDiameter / 2 + eps < min || entry.getValue().x + this.pieceDiameter / 2 - eps > max) {
                piecesToMove.put(entry.getKey(), entry.getValue());
            }
        }
        return piecesToMove;
    }

    /**
     * Computes the distances from each point to the target line in the board
     *
     * @param playerPieces
     * @param isPlayer1
     * @return A map with the distance of each point passed
     */
    protected HashMap<Integer, Double> getDistanceToTarget(HashMap<Integer, Point> playerPieces, boolean isPlayer1) {
        double goal_position = isPlayer1 ? -20 : 20;
        HashMap<Integer, Double> distanceMap = new HashMap<>();
        playerPieces.forEach((key, value) -> distanceMap.put(key, Math.abs(goal_position - value.x)));
        return distanceMap;
    }

    /**
     * Computes the distances from each point to the first obstacle to move
     * forward
     *
     * @param playerPieces
     * @param opponentPieces
     * @return A map with the distances corresponding to each point
     */
    protected HashMap<Integer, Double> getDistanceToFirstObstacle(HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces) {
        HashMap<Integer, Double> distanceMap = new HashMap<>();
        playerPieces.forEach((key, value) -> distanceMap.put(key, 0.0));
        return distanceMap;
    }

    /**
     * Given a piece decides to what position to move it (implicitly the angle)
     *
     * @param piece
     * @param opponentPieces
     * @return the position where the piece will be after moving
     */
    protected Point getPositionToMove(Pair<Integer, Point> piece, HashMap<Integer, Point> opponentPieces) {
        Point newPosition = new Point(piece.getValue());
        newPosition.x = isPlayer1 ? newPosition.x - this.pieceDiameter : newPosition.x + this.pieceDiameter;
        return newPosition;
    }

    /**
     * Given a list of candidates pieces to move with their 'scores', decides
     * the pieces to move
     *
     * @param number
     * @param playerPieces
     * @param scores
     * @return the list of IDs of the pieces to move
     */
    protected List<Integer> pickPiecesToMove(int number, HashMap<Integer, Point> playerPieces, HashMap<Integer, Double> scores, HashMap<Integer, Point> opponentPieces) {
        LinkedHashMap<Integer, Double> sortedByScore = this.sortHashMapByValues(scores);
        List<Integer> selected = new ArrayList<>();
        int i = 0;
        for (HashMap.Entry<Integer, Double> entry : sortedByScore.entrySet()) {
            if (this.checkValidity(new Pair<>(entry.getKey(), this.getPositionToMove(new Pair<>(entry.getKey(), playerPieces.get(entry.getKey())), opponentPieces)), playerPieces, opponentPieces)) {
                selected.add(entry.getKey());
                i++;
                if (i == number) {
                    break;
                }
            }
        }
        return selected;
    }

    public LinkedHashMap<Integer, Double> sortHashMapByValues(
            HashMap<Integer, Double> passedMap) {
        List<Integer> mapKeys = new ArrayList<>(passedMap.keySet());
        List<Double> mapValues = new ArrayList<>(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        LinkedHashMap<Integer, Double> sortedMap
                = new LinkedHashMap<>();

        Iterator<Double> valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Double val = valueIt.next();
            Iterator<Integer> keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                Integer key = keyIt.next();
                Double comp1 = passedMap.get(key);
                Double comp2 = val;

                if (comp1.equals(comp2)) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }
        return sortedMap;
    }
}
