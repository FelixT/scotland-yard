package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;

// TODO name the AI
@ManagedAI("AI")
public class MyAI implements PlayerFactory {

	// TODO create a new player here
	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	// TODO A sample player that selects a random move
	private static class MyPlayer implements Player, MoveVisitor {

		private int location;
		private final Random random = new Random();
		int furthestDistance = 0;
		Move furthestMove = new PassMove(Colour.BLACK);

		public void visit(TicketMove move) {
			// get the distance away the move would be
			int distance = Math.abs(location - move.destination());

			if(distance > furthestDistance) {
				furthestDistance = distance;
				furthestMove = move;
			}
		}

		public void visit(DoubleMove move) {
			// get the distance away the move would be
			int distance = Math.abs(location - move.finalDestination());

			if(distance > furthestDistance) {
				furthestDistance = distance;
				furthestMove = move;
			}
		}

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {
			// reset furthest distance
			furthestDistance = 0;
			furthestMove = new PassMove(Colour.BLACK);
            this.location = location;

            for(Move move : moves)
            	move.visit(this);

			callback.accept(furthestMove);

		}
	}
}
