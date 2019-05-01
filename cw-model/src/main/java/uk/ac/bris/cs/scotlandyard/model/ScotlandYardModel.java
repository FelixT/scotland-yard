package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private List<Spectator> spectators = new ArrayList<>();
	private int round = 0;
	private Colour currentPlayer = BLACK;
	private int lastMrX = 0;
	private boolean wasmrx = false;
	private Map<Colour, ScotlandYardPlayer> colourMap = new HashMap<>();
	private Set<Colour> winners = new HashSet<>();

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

		this.rounds = Objects.requireNonNull(rounds);
		this.graph = Objects.requireNonNull(graph);

		if (rounds.isEmpty())
			throw new IllegalArgumentException("Empty rounds");

		if (graph.isEmpty())
			throw new IllegalArgumentException("Empty map");

		if (mrX.colour != BLACK)
			throw new IllegalArgumentException("MrX should be black");

		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();

		for (PlayerConfiguration configuration : restOfTheDetectives)
			configurations.add(Objects.requireNonNull(configuration));

		configurations.add(0, firstDetective);
		configurations.add(0, mrX);

		Set<Ticket> allticketset = new HashSet<>();
		allticketset.add(Ticket.TAXI);
		allticketset.add(Ticket.BUS);
		allticketset.add(Ticket.UNDERGROUND);
		allticketset.add(Ticket.DOUBLE);
		allticketset.add(Ticket.SECRET);

		Set<Integer> locationset = new HashSet<>();
		Set<Colour> colourset = new HashSet<>();

		for (PlayerConfiguration configuration : configurations) {

			if (locationset.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");

			if (colourset.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");

			locationset.add(configuration.location);
			colourset.add(configuration.colour);

			if (!configuration.tickets.keySet().containsAll(allticketset))
				throw new IllegalArgumentException("Each player must have each ticket type even if their values are zero");

			if (configuration.colour != BLACK) {

				// check detectives have the right tickets
				if (configuration.tickets.get(DOUBLE) != 0)
					throw new IllegalArgumentException("Detectives should not have double tickets");

				if (configuration.tickets.get(SECRET) != 0)
					throw new IllegalArgumentException("Detectives should not have secret tickets");

			}

			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets);
			players.add(Objects.requireNonNull(player));

		}

		for (ScotlandYardPlayer player : players) {
			colourMap.put(player.colour(), player);
		}

	}

	private void checkGameOver() {
        if(isGameOver())
            for (Spectator spectator : spectators)
                spectator.onGameOver(this, getWinningPlayers());
    }

	@Override
    public void visit(PassMove move) {
        System.out.println("Pass move");
        nextPlayer();
        System.out.println("Next player " + currentPlayer);

        System.out.println("On move made");
        for (Spectator spectator: spectators)
            spectator.onMoveMade(this, move);

        if (wasmrx) {
            round++;
            System.out.println("Increased round");
            System.out.println("On round started");

            for (Spectator spectator : spectators)
                spectator.onRoundStarted(this, round);

        }
    }

	@Override
	public void visit(TicketMove move) {
		System.out.println("Ticket move, " + move.ticket());

		for (ScotlandYardPlayer player : players) {
			if (player.colour() == currentPlayer) {
				player.location(move.destination());
				// decrease number of tickets of player
				int ticketsleft = player.tickets().get(move.ticket()) - 1;
				player.tickets().replace(move.ticket(), ticketsleft);
				if(currentPlayer != BLACK) {
					// detectives transfer their tickets to mr x
					for (ScotlandYardPlayer mrx : players) {
						if (mrx.colour() == BLACK) {
							int newtickets = mrx.tickets().get(move.ticket()) + 1;
							mrx.tickets().replace(move.ticket(), newtickets);
						}
					}
				}
			}
		}
		nextPlayer();
		System.out.println("Next player " + currentPlayer);

		System.out.println("On move made");
		for (Spectator spectator : spectators)
			spectator.onMoveMade(this, move);
		checkGameOver();

		if (wasmrx) {
			round++;
			System.out.println("Increased round");
			System.out.println("On round started");

			for (Spectator spectator : spectators)
				spectator.onRoundStarted(this, round);

		}

	}

	@Override
	public void visit(DoubleMove move) {
		System.out.println("double move");

		TicketMove firstmove = move.firstMove();
		TicketMove secondmove = move.secondMove();

		if(rounds.size() > round) {
            // check if going from reveal to non reveal
            if (!rounds.get(round) && rounds.get(round + 1)) {
                // if it is, set the first destination to 0
                firstmove = new TicketMove(move.firstMove().colour(), move.firstMove().ticket(), 0);
            }
            // check if going from non reveal to reveal
            if (rounds.get(round) && !rounds.get(round + 1)) {
                // if it is, set the second destination to the destination of the first
                secondmove = new TicketMove(move.secondMove().colour(), move.secondMove().ticket(), move.firstMove().destination());
            }
            // if both rounds are hidden, set both to 0
            if (!rounds.get(round) && !rounds.get(round + 1)) {
                firstmove = new TicketMove(move.firstMove().colour(), move.firstMove().ticket(), 0);
                secondmove = new TicketMove(move.secondMove().colour(), move.secondMove().ticket(), 0);
            }
        }
        DoubleMove specmove = new DoubleMove(currentPlayer, firstmove, secondmove);



        // perform logic to do moves

		nextPlayer();
		System.out.println("Next player " + currentPlayer);

		System.out.println("on move made");

		for (Spectator spectator : spectators)
			spectator.onMoveMade(this, specmove);


		round++;
		System.out.println("Increased round");
		System.out.println("On round started");

		for (Spectator spectator : spectators)
			spectator.onRoundStarted(this, round);

		System.out.println("on first move made");

		for (Spectator spectator : spectators)
			spectator.onMoveMade(this, firstmove);

		round++;
		System.out.println("Increased round");
		System.out.println("On round started");

		for (Spectator spectator : spectators)
			spectator.onRoundStarted(this, round);


		for (ScotlandYardPlayer player : players) {
			if (player.colour() == BLACK) {
				// decrease number of tickets of player
				int ticketsleft = player.tickets().get(move.firstMove().ticket()) - 1;
				player.tickets().replace(move.firstMove().ticket(), ticketsleft);
				player.location(move.firstMove().destination());

			}
		}

        checkGameOver();

		System.out.println("on second move made");

		for (Spectator spectator : spectators)
			spectator.onMoveMade(this, secondmove);

		for (ScotlandYardPlayer player : players) {
			if (player.colour() == BLACK) {
				player.location(move.secondMove().destination());
				// decrease number of tickets of player
				int ticketsleft = player.tickets().get(move.secondMove().ticket()) - 1;
				player.tickets().replace(move.secondMove().ticket(), ticketsleft);
				int doubleticketsleft = player.tickets().get(DOUBLE) - 1;
				player.tickets().replace(DOUBLE, doubleticketsleft);
			}
		}

        checkGameOver();
	}

	@Override
	public void accept(Move move) {
		System.out.println("Consumer.accept");

		wasmrx = (currentPlayer == BLACK);

		if (move == null)
			throw new NullPointerException("Move can't be null");

		Set<Move> validMoves = validMoves(currentPlayer);

		//if (!validMoves.contains(move)) {
			//throw new IllegalArgumentException("Invalid move");
		//}

		move.visit(this);
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		if(spectators.contains(spectator))
			throw new IllegalArgumentException("Spectator already exists");
		else
			spectators.add(Objects.requireNonNull(spectator));
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		if(spectator == null)
			throw new NullPointerException("Spectator can't be null");
		else if(!spectators.contains(spectator))
			throw new IllegalArgumentException("Spectator has never been added");
		else
			spectators.remove(spectator);
	}

	private boolean noDetectiveOnSpace(int space) {
		for (ScotlandYardPlayer player : players)
			if (player.colour() != BLACK && player.location() == space)
				return false;
		return true;
	}

	private Set<Move> validMoves(Colour colour) {

		ScotlandYardPlayer player = colourMap.get(colour);
		Node<Integer> playerNode = graph.getNode(player.location());
		Set<Move> moves = new HashSet<>();
		Move moveToAdd;

		// TODO
		// testDetectiveMovesOmittedIfNotEnoughTickets fails because mr X isn't
		// given taxi->86 as an option, not stated reason.

		for (Edge<Integer, Transport> edge : graph.getEdgesFrom(playerNode)) {

			Ticket ticket = Ticket.fromTransport(edge.data());
			int destination = edge.destination().value();

			if ((player.hasTickets(ticket) || player.hasTickets(SECRET)) && noDetectiveOnSpace(destination)) {

				if (player.hasTickets(ticket)) {
					moveToAdd = new TicketMove(colour, ticket, destination);
					moves.add(moveToAdd);
				}

				if (player.hasTickets(SECRET)) {
					moveToAdd = new TicketMove(colour, SECRET, destination);
					moves.add(moveToAdd);
				}

				if (player.hasTickets(DOUBLE) && round < 21) {

					for (Edge<Integer, Transport> edge2: graph.getEdgesFrom(edge.destination())) {

						TicketMove move1 = new TicketMove(colour, ticket, destination);
						Ticket ticket2   = Ticket.fromTransport(edge2.data());
						int destination2 = edge2.destination().value();

						if (noDetectiveOnSpace(destination2)) {

							if ((ticket == ticket2 && player.hasTickets(ticket, 2)) || (ticket !=
									ticket2 && player.hasTickets(ticket) && player.hasTickets(ticket2))) {

								TicketMove move2 = new TicketMove(colour, ticket2, destination2);
								moveToAdd = new DoubleMove(colour, move1, move2);
								moves.add(moveToAdd);

							}

							if (player.hasTickets(SECRET)) {

								TicketMove move1sec = new TicketMove(colour, SECRET, destination);
								TicketMove move2 = new TicketMove(colour, ticket2, destination2);
								TicketMove move2sec = new TicketMove(colour, SECRET, destination2);

								if (ticket != SECRET && ticket2 != SECRET && player.hasTickets(ticket)) {
									moveToAdd = new DoubleMove(colour, move1, move2sec);
									moves.add(moveToAdd);
								}

								if (ticket != SECRET && ticket2 != SECRET && player.hasTickets(ticket2)) {
									moveToAdd = new DoubleMove(colour, move1sec, move2);
									moves.add(moveToAdd);
								}

								if (player.hasTickets(SECRET, 2)) {
									moveToAdd = new DoubleMove(colour, move1sec, move2sec);
									moves.add(moveToAdd);
								}
							}
						}
					}
				}
			}
		}

		if (player.colour() != BLACK && moves.isEmpty()) {
			moveToAdd = new PassMove(colour);
			moves.add(moveToAdd);
		}

		System.out.println(moves);
		return moves;

	}

	private void nextPlayer() {
		// get current player index
		int playerIndex = 0;

		for (ScotlandYardPlayer player : players) {
			if (player.colour() != currentPlayer)
				playerIndex++;
			else
				break;
		}

		int nextPlayer = (playerIndex + 1) % players.size();

		// get colour of next player
		currentPlayer = players.get(nextPlayer).colour();

	}

	@Override
	public void startRotate() {

		if(isGameOver()) {
			throw new IllegalStateException("Can't start new round when the game is already over");
		}

		System.out.println("Start rotate");
		System.out.println("Round " + round);
		System.out.println("Current player " + currentPlayer);

        Set<Move> moves = validMoves(currentPlayer);

		System.out.println("Make move");

		ScotlandYardPlayer player = colourMap.get(currentPlayer);
		player.player().makeMove(this, player.location(), moves, this);

		System.out.println("On rotation complete");

		for (Spectator spectator: spectators)
			spectator.onRotationComplete(this);
	}

	@Override
	public Collection<Spectator> getSpectators() {
		return unmodifiableList(spectators);
	}

	@Override
	public List<Colour> getPlayers() {

		List<Colour> playerColours = new ArrayList<>();

		for (ScotlandYardPlayer player : players)
			playerColours.add(Objects.requireNonNull(player.colour()));

		return Collections.unmodifiableList(playerColours);

	}

	@Override
	public Set<Colour> getWinningPlayers() {
        isGameOver();
		return unmodifiableSet(winners);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {

		ScotlandYardPlayer player = colourMap.get(colour);

		if (colour == BLACK) {
			if(round != 0 && rounds.get(round - 1)) { // if its a reveal round give location
				lastMrX = player.location();
				return Optional.of(player.location());
			} else  // otherwise return 0
				return Optional.of(lastMrX);
		}

		if (player == null) return Optional.empty();
		return Optional.of(player.location());
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		ScotlandYardPlayer player = colourMap.get(colour);
		if (player == null) return Optional.empty();
		return Optional.of(player.tickets().get(ticket));
	}

	@Override
	public boolean isGameOver() {
	    winners.clear();
		boolean playerinmrxposition = false;
		ScotlandYardPlayer mrx = colourMap.get(BLACK);
		int mrxposition = mrx.location();
		boolean nomoves = true;
		boolean mrxstuck = validMoves(BLACK).isEmpty();

		for (ScotlandYardPlayer player : players) {
			// check if detective in same position as mr x
			if (player.colour() != BLACK && player.location() == mrxposition) {
				playerinmrxposition = true;
			}
			// check if any detective has any moves remaining
            Move pass = new PassMove(player.colour());
            if(player.colour() != BLACK && !validMoves(player.colour()).contains(pass))
                nomoves = false;
		}
		// check if max rounds exceeded
		boolean roundsexceeded = (round >= rounds.size());

		if(playerinmrxposition || mrxstuck) {
			// all detectives win
			for (ScotlandYardPlayer player : players)
				if (player.colour() != BLACK)
					winners.add(player.colour());
		} else if(roundsexceeded || nomoves) {
            winners.add(BLACK);
        }

		return (nomoves || roundsexceeded || playerinmrxposition || mrxstuck);
	}

	@Override
	public Colour getCurrentPlayer() {
		return currentPlayer;
	}

	@Override
	public int getCurrentRound() {
		return round;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
        return new ImmutableGraph<>(graph);
	}

}
