^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns gameboard
  {:nextjournal.clerk/toc true}
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk-viewer]
            [gameboard-exercise.clerk-viewers :as viewers]
            [gameboard-exercise.core :as core]
            [gameboard-exercise.chess :as chess]
            ;;            [gameboard-exercise.checkers :as checkers]
            [gameboard-exercise.utils :as utils]))

;; # Exercise: modeling grid-board games
;;
;; Inspired by chapter 2.4 "Abstracting a Domain" from "Software Design for
;; Flexibility" (by Chris Hanson and Gerald Jay Sussman), we aim to create a
;; core model that can be used to implement different board games like
;; tic-tac-toe, chess, checkers.
;;
;; ## Goal
;;
;; The goal is purely pedagogical: to learn and teach software design, the
;; basics of functional programming, interactive (and "moldable") development,
;; data-orientation, simplicity, _stratified design_, immutability, Clojure, and
;; to compare the design of non-trivial domain model against other
;; approaches (like object/class-oriented programming).
;;
;; This "notebook" is based on [Clerk](https://book.clerk.vision).
;;
;; ## Some references
;;
;;  * Book: SICP (Structure and Interpretation of Computer Programs) by Abelson and Sussman
;;  * Book: Software Design for Flexibility (Chris Hanson and Gerald Jay Sussman)
;;  * Paul Graham's [bottom-up design essay](http://www.paulgraham.com/progbot.html)
;;  * Lisp: A language for stratified design
;;  * "Grokking Simplicity" by Eric Normand. A book on functional programming for beginners. I highly recommend it to learn the important practical concepts before delving into types and category theory.
;;  * [Thinking the Clojure Way](https://www.youtube.com/watch?v=vK1DazRK_a0) a great talk by Rafal Dittwald. It's in JavaScript despite the title.
;;  * [Clerk](https://book.clerk.vision)
;;
;; ## Code organization
;;
;; The code has *no* library depedencies, not even for the widgets to visualize
;; the boards on this notebook (other than Clerk itself). I also tried to use
;; only basic features of Clojure (no managed refs, multimethods, protocols,
;; transducers).
;;
;; This notebook bellow gives a quick high-level walkthrough of the data
;; structures and code, which might be useful before jumping straight to the
;; code.
;;
;; The code is organized in the following namespaces:

;; |namespace | description|
;; |----------|------------|
;; |clerk-viewers | The graphical widgets to visualize boards and moves on Clerk notebooks |
;; |core | core generic domain to model board games|
;; |checkers | TBD |
;; |chess | implementation of classic Chess |
;; |ttt | Tic-Tac-Toe  TBD |
;;
{::clerk/visibility {:code :show}
 ::clerk/auto-expand-results? true
 ::clerk/budget 1000}

^{::clerk/visibility {:code :hide :result :hide}}
(clerk/reset-viewers! clerk/default-viewers)

^{::clerk/visibility {:code :hide :result :hide}}

#_^{::clerk/visibility {:code :hide :result :hide}} (clerk/add-viewers! [viewers/board-viewer])
#_^{:nextjournal.clerk/visibility {:code :hide :result :hide}} (clerk/add-viewers! [viewers/chess-board-viewer])

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(clerk/add-viewers! [viewers/board-viewer viewers/board-move-viewer])

;; ## How to model a board? Where do we start?

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
               [R N B Q K B N R]] )

;; Do you see it? With some imagination, that's a chess board at starting
;; position :-). Each element is either a piece or an empty square. I chose
;; single letters instead of words to make the board easier to visualize. Upper
;; case for white pieces, lower case for black.[^no_commas]
;;
;; [^no_commas]: a nice thing about Clojure is that you don't need commas to
;; separate elements of a vector or list
;;
;;
;; That example uses Clojure symbols, but I could have used chars, or keywords instead (more idiomatic, actually):

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
;; it's easier on the eyes.
;;
;; It's nice to make it easy to create literal board positions.. and it's trivial to
;; get the piece from a given position coordinate:

(get-in board-1 [0 0])

(get-in board-2 [7 4])

;; Now... _position_ is just one attribute of a piece. Here we rely on the
;; letter's case (upper or lower) to represent the color (player actually, for
;; chess) but other games might need more than 2 colors... and we'll surely need
;; other attributes, like whether the piece has been moved or not. So, the above
;; representation is handy, yes... and very limited.

;; I ended up representing the board as map, with its dimensions and its set of
;; pieces. This would be an empty 8x8 board:

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
{:pieces #{}, :row-n 3, :col-n 3}

;; The value on the `:pieces` key is a set of pieces, where each piece is a map
;; with its attributes. Here's an example of a board with a few pieces:

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
{:pieces #{{:type :k, :player 0, :pos [4 2], :flags #{}}
           {:type :p, :player 0, :pos [4 3], :flags #{}}
           {:type :p, :player 1, :pos [3 4], :flags #{}}
           {:type :k, :player 1, :pos [3 5], :flags #{}}}
 :row-n 8
 :col-n 8}

;; * `:type` is the piece type (like `:k` for king, `:p` for pawn). We could use long names here
;; * `:player` is the player number (0 or 1 for chess). This leaves open the possibility of more than 2 players
;; * `:pos` the position of the piece on the board. Side-effect: this allows for
;;   more than one piece on the same square (if some game needs
;;   that)[^identical-piece-on-set]
;; * `:flags` a set of flags to attach to the piece (like, `:moved?`, or whatever each specific game might need)
;;
;; [^identical-piece-on-set]: Well, ok ok: two totally identical pieces wouldn't
;; be allowed on a Set (dupes aren't allowed)... but I won't over-complicate it
;; for now
;;
;;

;; The `core` namespace is the generic domain for board games, not game-specific.
;; Function `core/empty-board` creates a board with the given dimensions:

(core/empty-board 3 3)


;; ## Visualizing our domain

;; Let's write and use a specialized viewer to visualize our boards:
^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 3 3)

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

^{::clerk/viewer viewers/tabbed-board-viewer}
(def a-chess-board (chess/initiate-chess-board))



;; ## Alternate representations

;; Our new board structure is more flexible, and we have nice graphical
;; viewers... but I still I miss the simple matrix represation: It's good to
;; have an easy way to express _literal_ boards without the whole data
;; structure with all those details...  specially when working at the REPL, or
;; writing unit tests.
;;
;; How about we write a function for that? That's function `core/symbolic->board` : it
;; converts our simple matrix to our richer boards. Let's initialize a board
;; from a symbolic literal value:
^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/symbolic->board '[[- - - - - - - -]
                        [- - - - - - - -]
                        [- - - k - - - -]
                        [- - - p - - - -]
                        [- - - - P - - -]
                        [- - - - K - - -]
                        [- - - - - - - -]
                        [- - - - - - - -]])


;; And we can go the opposite direction with `core/board->symbolic`, which
;; converts a board structure to a simplified symbolic representation:
(def symboard (core/board->symbolic a-chess-board))


;; This technique of having another an alternate representation for an domain
;; entity is quite common and useful in other cases too. What if we want to
;; shuffle all the pieces on the board? With the matrix representation it's
;; easy to manipulate the board using Clojure's standard functions:
^{::clerk/viewer viewers/tabbed-board-viewer}
(def a-random-board (->> symboard
                         (apply concat)
                         shuffle
                         (partition (count symboard))
                         core/symbolic->board))


;; And how about Fisher-style shuffling?  That is: we shuffle only the first
;; row, and the last row must be its mirror, so that both players start from an "equal" position:
^{::clerk/viewer viewers/tabbed-board-viewer}
(let [first-row (shuffle (first symboard))
      last-row (->> first-row (mapv utils/upper-case-keyword))]
  (-> symboard
      (assoc 0 first-row)
      (assoc (dec (count symboard)) last-row)
      core/symbolic->board))

;; ## Moving pieces

;; OK. This is the core problem of this domain: How to decide which moves are valid?
;;
;; I find it's better to model the problem as that of generating valid
;; moves. So, the question becomes: _How to generate all valid moves at a given board?_
;;
;; In the case of chess, the valid moves for a piece depend on its type. So we'd
;; like to have a way to code the move-generation logic independently for each
;; piece type. Some of them share similar moves (diagonals, orthogonal, cannot
;; go over other pieces, captures oponent pieces, etc.), so it'd be nice to code
;; this "behaviors" separately and combine them to define the final piece types.
;;
;; *TBD*

;; ### Generating possible (valid) moves
;;
;; We model a move as list of (one or more) steps. Each step includes the piece
;; it applies to and the resulting board state. The very first step is the
;; initial board.
;;
;; While generating possible moves, a move is grown one step at a time. Here we
;; refer to them as `pmoves`  (for "partial moves").  A `pmove` that is not
;; `:finished?` may be expanded with more steps until it becomes so.
;;
;; Example of a `pmove` in the making (using a small board to avoid noise):
;;

^{::clerk/viewer viewers/tabbed-side-by-side-move-viewer}
(core/new-pmove (core/symbolic->board '[[r - k]
                                        [- - -]
                                        [R - K]])
                {:type :R, :player 0, :pos [0 0]})
;;
;; ## Simple Tests
;; ### TBD



;;
;; # ********* DRAFT SKETCH NOTES *********
;;
;; ## OK. Let's start chess game...
(def game-1 (core/start-game chess/chess-game))

;; Get me all possible moves...
          (count (core/possible-pmoves game-1))

;; Let's take a look at a couple of moves returned:
        ^{::clerk/viewer clerk-viewer/map-viewer
          ::clerk/auto-expand-results? true}
        (first (core/possible-pmoves game-1))

      ^{::clerk/viewer clerk-viewer/map-viewer
        ::clerk/auto-expand-results? true}
      (second (core/possible-pmoves game-1))

;; So, a move contains a list of `:steps`. Each step includes the piece it
;; applies to and a snapshot of the board. The very first step is the initial board.
;;
;; The idea of modeling moves as a list of steps will make sense when you dig
;; into the implementation of `core/possible-moves`.

;; Let's sort all the possible moves by piece x-coordinate, and show them visually:
    (clerk/row (sort-by #(-> % :steps first :piece :pos first)
                        (core/possible-pmoves game-1)))

;; Just for kicks, let's start off the random board create above
(def a-random-game
  (core/start-game chess/chess-game a-random-board))

;; Get all possible Rook moves:
(clerk/row
 (->>  (core/possible-pmoves a-random-game)
       (filter (fn [pmove] (= :r (-> pmove :steps last :piece :type))))))


#_(clerk/row  (->> (core/possible-pmoves a-random-game)
                 (map (comp :board first :steps))))




#_(clerk/row (map #(-> % :steps first :board core/board->symbolic) (take 2 (core/possible-pmoves game-1))))
#_(-> (first (core/possible-pmoves game-1)) :steps last :piece :pos)

#_(clerk/row
   (map #_#(update-in % [:steps] reverse)
    #(first (:steps %))

    (filter (fn [pmove]
              (= [1 1] (-> pmove :steps last :piece :pos)))
            (core/possible-pmoves game-1))))





;; # *** WIP ***

;; ### * TODO: explain move generation impl (core primitives + game-specific rules)
;; ### * TODO: complete chess rules impl
;; ### * TODO: implement game loop: to actually 'play'. Basically: "apply" moves to game stage
;; ### * TODO: implement other games



#_
^{::clerk/visibility {:code :hide :result :hide}}
(defmacro render-board-expression [expr]
  "Render a board expression as code, the resulting clojure map and graphical board view"

  ;; implementation of this was tricky... getting a bit into Clerks internals
  `(clerk/col

    ^{::clerk/visibility {:code :hide :result :show}}
    (clerk/with-viewer clerk-viewer/code-block-viewer
      {:text ~(str expr)})

    (let [maybe-var-value# ~expr
          value# (if (instance? clojure.lang.IDeref maybe-var-value#)
                    (deref maybe-var-value#)
                    maybe-var-value#)]

      ^{::clerk/visibility {:code :hide :result :show}}
      (clerk/row [(clerk/with-viewers clerk/default-viewers
                    ^{::clerk/auto-expand-results? true}
                    value#)
                  (clerk/with-viewer viewers/board-viewer
                    value#)]))))

#_
^{::clerk/visibility {:code :hide :result :hide}}
(defmacro render-board-expression-str [exprS]
  "Render a board expression as code, the resulting clojure map and graphical board view"

  ;; implementation of this was tricky... getting a bit into Clerks internals
  `(clerk/col

    ^{::clerk/visibility {:code :hide :result :show}}
    (clerk/with-viewer clerk-viewer/code-block-viewer {:width :full}
      {:text ~exprS})

    (let [maybe-var-value# ~(edn/read-string exprS)
          value# (if (instance? clojure.lang.IDeref maybe-var-value#)
                   (deref maybe-var-value#)
                   maybe-var-value#)]

      ^{::clerk/visibility {:code :hide :result :show}}
      (clerk/row [(clerk/with-viewers clerk/default-viewers
                    ^{::clerk/auto-expand-results? true}
                    value#)
                  (clerk/with-viewer viewers/board-viewer
                    value#)]))))
