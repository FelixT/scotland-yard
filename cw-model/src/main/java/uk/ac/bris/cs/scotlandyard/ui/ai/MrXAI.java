package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.Set;
import java.util.function.Consumer;

// TODO name the AI
@ManagedAI("MrXAI")
public class MrXAI implements PlayerFactory {

	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private static class MyPlayer implements Player, MoveVisitor {

		private int location;
		private int furthestDistance;
		private Move furthestMove;

		public void visit(TicketMove move) {

			// Get the distance of the potential move and Mr X's last known location.
			int distance = Math.abs(location - move.destination());

			if(distance >= furthestDistance) {

				furthestDistance = distance;
				furthestMove     = move;

			}

		}

		public void visit(DoubleMove move) {

			// Get the distance of the potential move and Mr X's last known location.
			int distance = Math.abs(location - move.finalDestination());

			if(distance >= furthestDistance) {

				furthestDistance = distance;
				furthestMove     = move;

			}

		}

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {

			// Reset values.
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
