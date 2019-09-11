package flip.g6;

import flip.sim.Point;
import javafx.util.Pair;

public interface Move {
	
	public boolean isPossible(); // Returns if any of the coins selected for a strategy can perform that strategy
	public Pair<Integer, Point> getMove(); // Returns normal move for the specific strategy
	public Pair<Integer, Point> getHybridMove(); // Returns move for a different strategy
}
