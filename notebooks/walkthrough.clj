^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns walkthrough
  {:nextjournal.clerk/toc true}
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk-viewer]
            [nextjournal.clerk.test :as clerk.test]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            ;;            [boardgames.checkers :as checkers]
            [boardgames.utils :as utils]
            [boardgames.test-utils :as t]

            [flow-storm.clerk :as clerk-storm]))

;;
;; # Exercise in modeling grid-board games
;;
;; I started this little "project" after the following two events happened the
;; same week:
;;
;; * I was helping Software Engineering students with an exercise in modeling
;; the game of Chess and variations of it. Most of them using Java or Kotlin.
;;
;; * I was reading chapter 2.4 "Abstracting a Domain" from the book "Software
;; Design for Flexibility" (by Chris Hanson and Gerald Jay Sussman). Which,
;; coincidentally, models the game of Checkers and asks to extend it to Chess as
;; an exercise.[^sdff_book]
;;
;; [^sdff_book]: &nbsp; ![image](https://mit-press-us.imgix.net/covers/9780262045490.jpg?auto=format&w=298&dpr=2&q=80)
;;
;; ### Problem statement
;;
;; Model the game of chess.  Make the model flexible so that chess variations
;; can also be implemented (for example, defining special pieces with new
;; movements). Design it so that a core of the model can be used to implement
;; other board games, such as Checkers, Tic-Tac-Toe and the like.
;;
;; This repo is one posible solution, and this notebook is a walkthrough of the implementation.

;; ### Goal
;;
;; The goal of this exercise is purely pedagogical: to learn and teach some
;; software design concepts, covering the basics of topics such us:
;;
;; * Functional programming
;; * Literate programming
;; * Interactive (and "moldable") development
;; * Data-orientation
;; * Stratified design
;; * Simplicity
;; * Immutability
;; * Clojure
;;

;; ### Some references
;;
;;  * This page you are reading is a [Clerk](https://book.clerk.vision) _notebook_
;;  * Book: SICP (Structure and Interpretation of Computer Programs) by Abelson and Sussman
;;  * Book: Software Design for Flexibility (Chris Hanson and Gerald Jay Sussman)
;;  * Paul Graham's [bottom-up design essay](http://www.paulgraham.com/progbot.html)
;;  * Lisp: A language for stratified design
;;  * "Grokking Simplicity" by Eric Normand. A book on functional programming for beginners. I highly recommend it to learn the important practical concepts before delving into types and category theory.
;;  * [Thinking the Clojure Way](https://www.youtube.com/watch?v=vK1DazRK_a0) a great talk by Rafal Dittwald. It's all in JavaScript despite the title.
;;
;; ### Code organization
;;
;; Other than Clerk itself, the code has *no* library depedencies, not even for
;; the graphical widgets to visualize the boards on this notebook. I also tried
;; to use only basic features of Clojure (no managed refs, multimethods,
;; protocols, transducers).
;;
;; This notebook you are reading is a high-level walkthrough of the implementation.
;;
;; The code is organized in the following namespaces:

;; |namespace | description|
;; |----------|------------|
;; |[clerk-viewers](/src/boardgames/clerk_viewers) | The graphical widgets to visualize boards and moves on Clerk notebooks |
;; |[core](/src/boardgames/core/) | generic domain to model board games|
;; |[chess](/src/boardgames/chess/) | implementation of classic Chess (WIP)|
;; |[checkers](/src/boardgames/checkers) | implementation of Checkers (_TBD_) |
;; |[ttt](/src/boardgames/ttt) | implementation of Tic-Tac-Toe  (_TBD_) |
;;

#_(clerk/html [:ol
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url 'boardgames.clerk-viewers)} "clerk-viewers"]]
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url 'boardgames.core)} "core"]]
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url "/boardgames/core")} "core"]]
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url "/src/boardgames/core")} "core"]]
               [:li [:a {:href "/src/boardgames/core/"} "core4"]]])

 #_^{::clerk/visibility {:code :hide :result :hide}} (clerk/reset-viewers! clerk/default-viewers)

 ^{::clerk/visibility {:code :hide :result :hide}} (clerk/add-viewers! [viewers/board-viewer])

 #_^{:nextjournal.clerk/visibility {:code :hide :result :hide}} (clerk/add-viewers! [viewers/chess-board-viewer])

 #_^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
  (clerk/add-viewers! [viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer])





;; ---

;; ## OK. Where do we start? How to model a board?

;; Let's start with the simplest data structure that could represent the problem
;; at hand. In this case, I with a `board` data structure as vector of
;; vectors (a matrix), like so:

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(def board-1 '[[r n b q k b n r]
               [p p p p p p p p]
               [- - - - - - - -]
               [- - - - - - - -]
               [- - - - - - - -]
               [- - - - - - - -]
               [P P P P P P P P]
               [R N B Q K B N R]])



;; Do you see it? With some imagination, that's a chess board at starting
;; position 🙂. Each element is either a piece or an empty square. I chose
;; single letters instead of words to make the board easier to visualize. Upper
;; case for white pieces, lower case for black.[^no_commas]
;;
;; [^no_commas]: a nice thing about Clojure is that you don't need commas to
;; separate elements of a vector or list.
;;
;; That example above uses Clojure _symbols_. I could have used chars, or keywords
;; instead. The latter being the most idiomatic:

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
 (def board-2 [[:r :n :b :q :k :b :n :r]
               [:p :p :p :p :p :p :p :p]
               [:- :- :- :- :- :- :- :-]
               [:- :- :- :- :- :- :- :-]
               [:- :- :- :- :- :- :- :-]
               [:- :- :- :- :- :- :- :-]
               [:P :P :P :P :P :P :P :P]
               [:R :N :B :Q :K :B :N :R]])

;; One point in favor of keywords is that the literal board doesn't have to be
;; quoted (like symbols do, to avoid evaluation). A point for symbols is that
;; it's easier on the eyes... I'll stick with symbols.
;;
;; It's nice to make it easy to create literal board positions. It's also trivial to
;; get the piece at a given position coordinate:

(get-in board-1 [0 0])

(get-in board-1 [7 4])

;; Now... _position_ is just one attribute of a piece. Here we rely on the
;; letter's case (upper or lower) to represent the color (player actually, for
;; chess) but other games might need more than 2 colors... and we'll surely need
;; other attributes, like whether the piece has been moved or not. So, the above
;; representation is handy, yes... and limited.

;; I ended up representing the board as map, with its dimensions and its set of
;; pieces. This would be an empty 8x8 board:

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
{:pieces #{}, :row-n 3, :col-n 3}

;; The value on the `:pieces` key is a set of pieces, where each piece is a map
;; with its attributes. Here's an example board with a few pieces:

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
{:pieces #{{:type :k, :player 0, :pos [4 2], :flags #{}}
           {:type :p, :player 0, :pos [4 3], :flags #{}}
           {:type :p, :player 1, :pos [3 4], :flags #{}}
           {:type :k, :player 1, :pos [3 5], :flags #{}}}
 :row-n 8
 :col-n 8}

;; * `:type` is the piece type (like `:k` for king, `:p` for pawn). We could use long names here
;; * `:player` is the player number (0 or 1 for chess). This leaves open the possibility of more than 2 players
;; * `:pos` the position of the piece on the board. The origin `[0 0]` being the bottom-left square. Side-effect: this allows for
;;   having more than one piece on the same square (if some game needs that)[^identical-piece-on-set]
;; * `:flags` a set of flags to attach to the piece (like, `:moved?`, or whatever each specific game might need)
;;
;; [^identical-piece-on-set]: Well, ok ok: two totally identical pieces wouldn't
;; be allowed on a Set (dupes aren't allowed)... but I won't over-complicate it
;; for now
;;

;; The `core` namespace is the generic domain for board games. It's not game-specific.
;; Function `core/empty-board` creates a board with the given dimensions:

(core/empty-board 3 3)


;; ## Visualizing our domain

;; Let's write and use a specialized viewer to visualize our boards:
^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 3 3)

^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 2 8)

^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 8 8)


;; That's the idea behind [Clerk](https://book.clerk.vision): build specialized
;; viewers and interactive tools to make your code and data easier to work with
;; and explain.
;;
;; Let's create a standard chess board on its starting possition. We use
;; function `initialize-chess-board` from the `chess` namespace.  Now using
;; a "tabbed" viewer to switch between the plain data structure and the visual
;; board:

^{::clerk/viewer viewers/tabbed-board-viewer
  ::clerk/auto-expand-results? true }
(def a-chess-board (chess/initiate-chess-board))



;; ## Alternate representations

;; Our new board structure is more flexible, and we have nice graphical
;; viewers... but I still I miss the simple matrix syntax: It's good to have an
;; easy way to express _literal_ boards in code without the whole data structure...
;; specially when working at the REPL, or writing unit tests.
;;
;; How about we write a function for that? That's what `core/symbolic->board`
;; does: it converts our original simple matrix to our newer richer
;; structure for boards.
;;
;; Let's initialize a board from a symbolic literal value:
^{::clerk/viewer viewers/side-by-side-board-viewer ::clerk/auto-expand-results? true}
(core/symbolic->board '[[- - - - - - - -]
                        [- - - - - - - -]
                        [- - - k - - - -]
                        [- - - p - - - -]
                        [- - - - P - - -]
                        [- - - - K - - -]
                        [- - - - - - - -]
                        [- - - - - - - -]])

;; And we can go the opposite direction with `core/board->symbolic`, which
;; converts a board structure to a simplified symbolic representation.
^{::clerk/viewer clerk-viewer/code-viewer}
(def symboard (core/board->symbolic a-chess-board))


;; This technique of having another an alternate representation for an domain
;; entity is quite common and useful in many cases. Here, for example, What if
;; we want to shuffle all the pieces on the board? With the matrix
;; representation it's easy to manipulate the pieces using Clojure's standard
;; functions:
^{::clerk/viewer viewers/tabbed-board-viewer}
(def a-random-board (->> symboard
                         (apply concat)
                         shuffle
                         (partition (count symboard))
                         core/symbolic->board))


;; And how about Suffle-chess style starting position?  That is: we shuffle only the first
;; row, and the last row must be its mirror, so that both players start from an "equal" position:
^{::clerk/viewer viewers/tabbed-board-viewer}
(let [first-row (shuffle (first symboard))
      last-row (->> first-row (mapv utils/upper-case-keyword))]
  (-> symboard
      (assoc 0 first-row)
      (assoc (dec (count symboard)) last-row)
      core/symbolic->board))

;; ## Moving pieces

;; This is the core problem of this domain: How to decide which moves are
;; valid?. Actually, it's better to model the problem as that of generating
;; valid moves. So, the question becomes:
;;
;; > _How do we generate all valid moves given a board possition?_
;;
;; In the case of chess, the valid moves for a piece depend on its type. So we'd
;; like to have a way to code the move-generation logic independently for each
;; piece type. However, some of them share similar characteristics (moves on
;; diagonals, or orthogonal, cannot go over other pieces, captures opponent
;; pieces, etc.), so it'd be nice to code this "behaviors" separately and
;; combine them to model each one of the piece types.
;;
;;
;; ### Generating possible (valid) moves
;;
;; Before we dig into how to generate the moves, let's see how we'll represent such moves.
;;
;; We model a move as a list of (one or more) _steps_. Each step includes the piece
;; it affects and the resulting board state. The very first step represents the
;; board position at the start of this move.
;;
;; While generating possible moves, a move is grown (or _expanded_) one step at
;; a time. We'll refer to them as `pmoves` (for "partial moves").  A `pmove`
;; that is not `:finished?` may be expanded with more steps until it is either
;; discarded (a dead-end, with no valid options) or it becomes `:finished?`.[^partial-moves-idea]
;;
;; [^partial-moves-idea]: This idea of "partial moves" I took from the
;; book "Software Design for Flexibility"
;;
;; Let's create an initial `pmove`, for the Rook in position `[0 0]` (using a
;; small board here to keep the noise low):
;;
^{::clerk/viewer viewers/side-by-side-move-viewer}
(core/new-pmove (core/symbolic->board '[[r - k]
                                        [- - -]
                                        [R - K]])
                {:type :R, :player 0, :pos [0 0]})

;; That's a new partial-move, containing only a single step with its starting
;; board (step zero), and thus it is (obviously) not `:finished`.
;;
;; The core move-generation logic starts by calling the game-specific rules
;; functions with this initial pmove. Each rule function _expands_ this pmove by
;; returning a collection of (zero or more) new pmoves, each with an extra step added,
;; some of them `:finished`.
;;
;; The move-generation logic iterates over the rules functions again, passing
;; all the new pmoves that are not `:finished`, until we only have `:finished`
;; ones.
;;
;; Here's an example of one of the final generated moves for the rook above.  We
;; first start a game of chess from a given board position, obtain all possible
;; moves, and filter for the ones finishing on square `[0 2]` (top-right)
^{::clerk/visibility {:code :show :result :show}
  ::clerk/viewer viewers/side-by-side-move-viewer}
(let [game (core/start-game chess/chess-game
                            (core/symbolic->board '[[r - k]
                                                    [- - -]
                                                    [R - K]]))]
  (->> game
       core/possible-pmoves
       (filter (fn [pmove] (= [0 2] (-> pmove :steps first :piece :pos))))
       first))

;; It contains three steps, as we move the `R`ook from position `[0 0]` to `[0
;; 2]`. Each step includes the piece it applies to and a snapshot of the
;; board. The very first step is the initial board. Note that the last step also
;; records the capturing of a piece (a pawn).
;;
;; The idea of modeling moves as a list of steps will make sense when you dig
;; into the implementation the game-specific functions to expand the pmoves.



;; ## Call stack from `(possible-pmoves ...)`


;; Let's see the main functions in the call chain we trigger with `(core/possible-pmove ...)`[^clojure-storm].
;; On each function call, you can click on the argument names to see the value of
;; each:
;;
;; [^clojure-storm]: this viewer is powered by ClojureStorm (and FlowStorm): a
;; runtime tracing debugger that lets you record the execution of any Clojure
;; code, capturing code and values.


^{::clerk/visibility {:code :show :result :hide}}
(def game-1 (core/start-game chess/chess-game))


^{::clerk/viewer clerk-storm/trace-code-viewer}
(clerk-storm/show-trace {:include-fn-names [#"possible-pmoves"
                                            #"candidate-pmoves\*?"
                                            #"expand-pmove"] }
  (core/possible-pmoves game-1))

;; a lot more could be done here... show call trees, return values, etc.


#_(comment
    ;; Show dynamic function calls
    ;; Here's a viewer that evaluates a piece of code, traces all runtime
    ;; information using ClojureStorm, and shows the main function calls with their
    ;; arguments:
)



;; ## All moves

;; OK, let's generate all the possible moves from a starting chess board. We sort
;; them by piece coordinate, and show them visually:


(clerk/row
 (map (partial clerk/with-viewer viewers/tabbed-board-move-viewer)
      (sort-by #(-> % :steps last :piece :pos)
               (core/possible-pmoves game-1))))







;; ### Automated Tests

;; Our _symbolic_ representation of the boards allowed us to create a helper function for running tests:
;; given a board possition, we list the boards we expect as possible moves.
;;
;; Example:
^{::clerk/visibility {:code :show :result :hide}
  ::clerk/viewer clerk-viewer/code-viewer}
(t/expect-moves {:piece :R}

                  '[[p R -]
                    [- - -]
                    [- K -]]

                  '[[[R - -] ;; Capturing the pawn
                     [- - -]
                     [- K -]]

                    [[p - -]
                     [- R -]
                     [- K -]]

                    [[p p R]
                     [- - -]
                     [- K -]]])

;; v2: horizonally
^{::clerk/visibility {:code :show :result :hide}
  ::clerk/viewer clerk-viewer/code-viewer}
(t/expect-moves-2 {:piece :R}
                  '[[- - - -]
                    [- - - -]
                    [R - - -]
                    [- - - -]]

                  '[[- - - -] [- - - -]  [R - - -]   [- - - -] [- - - -] [- - - -]
                    [- - - -] [R - - -]  [- - - -]   [- - - -] [- - - -] [- - - -]
                    [- - - -] [- - - -]  [- - - -]   [- R - -] [- - R -] [- - - R]
                    [R - - -] [- - - -]  [- - - -]   [- - - -] [- - - -] [- - - -]])


;; The interesting thing, however, is when there's a failure...

(clerk/with-viewer clerk.test/test-suite-viewer
  @clerk.test/!test-report-state)

;; For more, see:
;; * [boardgames/clerk-testrunner](/test/boardgames/clerk_testrunner)



;; ---

;; ## ********* DRAFT SKETCH NOTES *********

;; #### * TODO Automatically validate expected moves by comparing against an existing game engine
;; #### * TODO: Explain more about move generation (core primitives + game-specific rules)
;; #### * TODO: implement other games (TTT, checkers)
;; #### * TODO: implement game loop: to actually 'play'. Basically: "apply" moves to game stage



;; ---

;; Just for kicks, let's start off the random board create above
(def a-random-game (core/start-game chess/chess-game a-random-board))

;; Get all possible moves:
(clerk/row
 (map (partial clerk/with-viewer viewers/tabbed-board-move-viewer)
     (sort-by #(-> % :steps last :piece :pos)
              (core/possible-pmoves a-random-game))))
