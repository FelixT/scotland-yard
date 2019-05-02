package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.Set;
import java.util.function.Consumer;

@ManagedAI("Trev")
public class Trev implements PlayerFactory {

	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private static class MyPlayer implements Player, MoveVisitor {

		private int location;
		private int furthestDistance;
		private Move furthestMove;

		/**
		 * @param move is a PassMove, which will only be the furthestMove if there are no other moves
		 */
		public void visit(PassMove move) {
			if (furthestDistance == 0)
				furthestMove = move;
		}

		/**
		 * Checks the distance between the last known location of MrX and the destination of the move
		 * @param move is a TicketMove
		 */
		public void visit(TicketMove move) {

			int distance = Math.abs(location - move.destination());

			if(distance >= furthestDistance) {

				furthestDistance = distance;
				furthestMove     = move;

			}

		}

		/**
		 * Checks the distance between the last known location of MrX and the final destination of the move
		 * @param move is a DoubleMove
		 */
		public void visit(DoubleMove move) {

			int distance = Math.abs(location - move.finalDestination());

			if(distance >= furthestDistance) {

				furthestDistance = distance;
				furthestMove     = move;

			}

		}

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {

			// Reset values every move
			furthestDistance = 0;
			if(view.getPlayerLocation(Colour.BLACK).isPresent()) // This should always be the case.
                this.location = view.getPlayerLocation(Colour.BLACK).get(); // Get position known to detectives.

            for(Move move : moves)
            	move.visit(this);

            // Mr X chooses the position furthest from Mr X's last position known to detectives.
			callback.accept(furthestMove);

		}
	}
}
