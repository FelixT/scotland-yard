Scotland Yard Project Report
====

## Details

- strictly 3 pages
- at least 1 page summary on what has been done
- at most 2 pages reflecting on achievements
- brief critical reflection
- "The report does not have to cover all things implemented in detail, but should be seen as an overview of the work judged in conjunction with code and presentation - teams can use it during the presentation as an aid. The report should be concise, clear and to the point"

## Summary on what has been done

First we perform checks on the game info we have been given, making sure that it's a valid configuration for starting the game.
We make sure the configuration for every player has every ticket item in their ticket map, which was accomplished by creating a set with each type of ticket in, and checking this is equal the keys from the ticket map of every player, converted to a set. This was done using tickets.keySet().containsAll(allTicketSet). We also make sure that only Mr X has any 'secret' or 'double' tickets, that there are no two players are in the same location or have the same colour, and also that other arguments are not null or empty.

Provided none of the above errors occur, we create a 
we add each player object to a LinkedHashMap, mapping from colours to ScotlandYardPlayer objects. This is useful as we often work in terms of player colours and need to find their player object based on that.

LinkedHashMap is used over a regular HashMap as it guaratees the order of its items if it is iterated through, which is needed for determining the next player in our code.

- The valid moves function...
- A basic AI for Mr X which, for each turn it has, picks the move it can make which places it as far as possible from its last known position, based on the assumption this is where detectives will attempt to head to. It does this by...

## Achievements

- OO concepts?
- polymorphism used by moves: allows us to return e.g. a set containing 'Moves' while each of these might be PassMove, TicketMove or DoubleMove. Each of these inherits the Move class
- 'consumer' used for accept (callbacks)
- visitor model
- dynamic dispach (but i don't think we used this?)
- DRY principle
- abstraction
- model view controller?
- used LinkedHashMap
- encapsulation/information

## (brief) reflection

- However, there are some things which retrospectively could have been done better.
The code uses 'instanceof' is used in the function 'isGameOver', whereas ... would have provided a more object-oriented solution.
- Additionally, the AI given is 
Additionally, most of the project ended up in the class ScotlandYardModel, causing to contain many functions and be long as a result. To improve this the class could be split over multiple classes.