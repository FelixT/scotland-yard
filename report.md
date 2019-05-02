Scotland Yard Project Report
====

## Details

- strictly 3 pages
- at least 1 page summary on what has been done
- at most 2 pages reflecting on achievements
- brief critical reflection
- "The report does not have to cover all things implemented in detail, but should be seen as an overview of the work judged in conjunction with code and presentation - teams can use it during the presentation as an aid. The report should be concise, clear and to the point"

## Summary on what has been done

- implemented the spec? lol
- First we perform checks on the game info we have been given
- The valid moves function...
- A basic AI for Mr X which, for each turn it has, picks the move it can make which places it as far as possible from its last known position, based on the assumption this is where detectives will attempt to head to. It does this by...

## Achievemens

- OO concepts?
- polymorphism used by moves: allows us to return e.g. a set containing 'Moves' while each of these might be PassMove, TicketMove or DoubleMove. Each of these inherits the Move class
- 'consumer' used for accept (callbacks)
- visitor model
- dynamic dispach (but i don't think we used this?)
- DRY principle
- abstraction
- model view controller?
- used LinkedHashMap

## (brief) reflection

- instanceof = bad
- ai sucks
- should split the model into different files ideally