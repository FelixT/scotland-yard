package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;

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
			// get the distance of the potential move and mrX's last known location
			int distance = Math.abs(location - move.destination());

			if(distance > furthestDistance) {
				furthestDistance = distance;
				furthestMove = move;
			}
		}

		public void visit(DoubleMove move) {
			// get the distance of the potential move and mrX's last known location
			int distance = Math.abs(location - move.finalDestination());

			if(distance > furthestDistance) {
				furthestDistance = distance;
				furthestMove = move;
			}
		}

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {
			// reset values
			furthestDistance = 0;
			furthestMove = new PassMove(Colour.BLACK);
			if(view.getPlayerLocation(Colour.BLACK).isPresent()) // this should always be the case
                this.location = view.getPlayerLocation(Colour.BLACK).get(); // get position known to detectives

            for(Move move : moves)
            	move.visit(this);

            // mr X chooses the position furthest from mr x's last position known to detectives
			callback.accept(furthestMove);

		}
	}
}
