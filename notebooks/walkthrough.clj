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

;; # Modeling chess: Code Walkthrough

;; ---
;; 1. [Welcome](./welcome)
;; 1. [Introduction](./intro): Goals. Clojure. Some background and recommended references.
;; 1. Code Walkthrough 👈 __you are here__
;; ---

#_(clerk/html [:ol
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url 'boardgames.clerk-viewers)} "clerk-viewers"]]
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url 'boardgames.core)} "core"]]
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url "/boardgames/core")} "core"]]
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url "/src/boardgames/core")} "core"]]
               [:li [:a {:href "/src/boardgames/core/"} "core4"]]])

 #_^{::clerk/visibility {:code :hide :result :hide}} (clerk/reset-viewers! clerk/default-viewers)
 #_^{::clerk/visibility {:code :hide :result :hide}} (clerk/add-viewers! [viewers/board-viewer])
 #_^{:nextjournal.clerk/visibility {:code :hide :result :hide}} (clerk/add-viewers! [viewers/chess-board-viewer])
 #_^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
  (clerk/add-viewers! [viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer])


;; ## How can we model a board?

;; We want to model a board for grid-based games (like chess or
;; checkers). The pieces can be of different types (pawn, rook, etc.) and
;; colors. Each piece has a position on the board.
;;
;; Let's start with the simplest data structure to represent the problem. We can
;; model a `board` simply as a vector of vectors (a matrix?), like so:

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
;; position 🙂. Each element of the inner vectors is either a piece (a letter)
;; or an empty square (`-`). Upper case for white pieces, lower case for
;; black.[^no_commas]
;;
;; [^no_commas]: a nice thing about Clojure: you don't need commas to separate
;; elements of a vector or list.

;; That code defines a new _var_ named `board-1` to hold our vector of vectors of symbols.

;; Here we use Clojure _symbols_ for each piece. We could have used chars or
;; Clojure _keywords_ instead (which would've been more idiomatic). We'll stick
;; with symbols which are easier on the eyes.[^symbols_quoting]
;;
;; [^symbols_quoting]: NOTE: symbols are commonly used as names for
;; _variables_ (like `board-1` here). The single quote `'` char at the beginning
;; of the vector tells Clojure to take the following expression _literally_,
;; without trying to resolve symbols as variables. If this is confusing, never
;; mind: think of them as one-character "constants" here.

;; It's nice to have a way to express a board positions as a _literal_
;; expression like that. This representation also makes it trivial to get the
;; piece at a given position coordinate:

(get-in board-1 [0 0])

(get-in board-1 [7 4])

;; Now... What if we want to track more data about each piece?  _Position_ is
;; just one attribute of a piece, and here its implied by its location on the
;; vectors. To represent the color of each piece (player side actually) here we
;; rely on the letter's case (upper or lower), so we are limited to 2. Although
;; we don't want to over-design, we'll surely need other attributes to keep
;; track of, like whether the piece has been moved or not, which is needed in
;; Chess for certain moves.

;; So, the above representation is handy... and limited.  What if we just
;; represent pieces as maps[^clojure_maps], and the board as a map too?. The board would
;; initially include its dimensions and the set of pieces.  This would be an
;; empty 8x8 board:
;; [^clojure_maps]: a data structure to stores key-value pairs. Aka "dictionary", "hashmap", "associative array" in some languages

^{:nextjournal.clerk/visibility {:code :show :result :hide}
  ::clerk/auto-expand-results? true}
{ :pieces #{}
  :row-n 3
  :col-n 3 }

;; The value on the `:pieces` key is a Set of pieces[^clojure-sets], where each piece is another Map
;; with its attributes. Here's another board with a few pieces:
;; [^clojure-sets]: Map literals are written like this in Clojure:  `{...}`. Set literals as: `#{...}`

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
{:pieces #{{:type :k, :player 0, :pos [4 2], :flags #{}}
           {:type :p, :player 0, :pos [4 3], :flags #{}}
           {:type :p, :player 1, :pos [3 4], :flags #{}}
           {:type :k, :player 1, :pos [3 5], :flags #{}}}
 :row-n 8
 :col-n 8}

;; * `:type` is the piece type (like `:k` for king, `:p` for pawn)
;; * `:player` is the player number (0 or 1 for chess). This leaves open the possibility of more than 2 players
;; * `:pos` the position of the piece on the board. The origin `[0 0]` being the bottom-left square.
;; * `:flags` a set of flags to attach to the piece (like, `:moved?`, or whatever each specific game might need)
;;
;; This representation is more flexible. We can attach any data we might need
;;   onto our pieces. For instance, if a game needs to keep track of pieces
;;   outside the board, we could do that by using special values on
;;   `:pos`... and we could potentially allow more than one piece on the same
;;   position[^identical-piece-on-set].
;; [^identical-piece-on-set]: Well, ok smart reader: two totally identical pieces wouldn't
;; be allowed on a Set (sets don't allow duplicates)... but we'll keep it simple for now
;;

;; We can write a trival function to create a board with the given dimensions. That's what `core/empty-board` does:

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(defn new-board [pieces row-n col-n]
  {:pieces (set pieces)
   :row-n row-n
   :col-n col-n})

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(defn empty-board [n-rows n-columns]
  (new-board #{} n-rows n-columns))

;; Create an empty 3x3 board:

(core/empty-board 3 3)

;; We'll use the [`core`](/src/boardgames/core/) namespace for the code that is generic about the domain
;; of grid games. And we'll use another namespace for each game, like [`chess`](/src/boardgames/chess/).

;;
;; ## Visualizing our domain
;;
;; Before going any further: Wouldn't it be nice to see a graphical representation of our boards?

;; That's the idea behind notebooks and [Clerk](https://book.clerk.vision) in
;; particular: to build specialized viewers for your problem domain, and
;; interactive tools to make your code and data easier to work with and
;; explain[^moldable].
;; [^moldable]: The big idea here is [Moldable
;; Development](https://moldabledevelopment.com/), a way of programming through
;; custom tools built for each problem.

;; So, I wrote a simple viewer to visualize our boards. Let's create a few empty
;; boards of different sizes to try it out. The resulting data structure renders
;; on the left, and a graphical view of it on the right:
^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 3 3)

^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 2 8)

^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 8 8)


;; We are not going to go into the boards rendering code right now. If you are curious, feel free to peek into the [`clerk-viewers`](../src/boardgames/clerk_viewers) namespace.[^clerk-viewers-impl]
;; [^clerk-viewers-impl]: Warning: the implementation of the viewers is a bit messy... needs some clean-up

;; Let's create one with a few pieces:
^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/new-board 8 8 #{{:type :k, :player 0, :pos [4 2], :flags #{}}
                      {:type :p, :player 0, :pos [4 3], :flags #{}}
                      {:type :p, :player 1, :pos [3 4], :flags #{}}
                      {:type :k, :player 1, :pos [3 5], :flags #{}}} )


;; OK. What if we want to create a standard chess board on its starting
;; possition? That'd be pretty verbose with our new maps representation. It was
;; short and "visual" with the matrix representation we started with... not
;; anymore 😕.

;; ## Alternate representations

;; Our new board structure is more flexible, and we now have graphical
;; viewers... but I still I miss the simple matrix syntax: It's good to have an
;; easy way to express _literal_ boards in code...
;; specially when working at the REPL, or writing unit tests.
;;
;; This is true for any problem domain: it's always good to make your code as
;; close as possible to the language of your domain.
;;
;; What if we had a function for that? I mean, let's write a function that
;; converts our original simple matrix to our newer richer structure for
;; boards. That's exactly what function `core/symbolic->board` does.
;;
;; Let's initialize a board just like our last one but from a symbolic literal value:
^{::clerk/viewer viewers/side-by-side-board-viewer ::clerk/auto-expand-results? true}
(core/symbolic->board '[[- - - - - - - -]
                        [- - - - - - - -]
                        [- - - k - - - -]
                        [- - - p - - - -]
                        [- - - - P - - -]
                        [- - - - K - - -]
                        [- - - - - - - -]
                        [- - - - - - - -]])

;; So now we can create a full Chess board at initial position from a compact representation:
^{::clerk/viewer viewers/side-by-side-board-viewer ::clerk/auto-expand-results? true}
(core/symbolic->board '[[r n b q k b n r]
                        [p p p p p p p p]
                        [- - - - - - - -]
                        [- - - - - - - -]
                        [- - - - - - - -]
                        [- - - - - - - -]
                        [P P P P P P P P]
                        [R N B Q K B N R]])


;;
;; Of course, we want to have a function for that: `chess/initialize-chess-board` (in the
;; [`chess`](/src/boardgames/chess/) namespace). Its implementation is almost exactly
;; like the last snippet above.
;;
;; Let's try it here:

^{::clerk/viewer viewers/tabbed-board-viewer
  ::clerk/auto-expand-results? true }
(def a-chess-board (chess/initiate-chess-board))

;;  Note that we are using a "tabbed" viewer this time, which lets you switch
;; between the plain data structure and the visual board.

;; Of course we'd also want to go the opposite direction, that is: convert a
;; board structure to the simplified symbolic representation. That's what
;; `core/board->symbolic` does. Here we convert the starting chess board
;; position we've just created back into a matrix:
^{::clerk/viewer clerk-viewer/code-viewer}
(def symboard (core/board->symbolic a-chess-board))


;; This technique of having another an alternate representation for an domain
;; entity is quite common and useful in many situations.

;; For example: What if we want to shuffle all the pieces on the board? With the
;; matrix representation it's easy to manipulate the pieces using Clojure's
;; standard functions... So: we take our rich initial chess board, convert it,
;; shuffle the pieces, and convert it back:
^{::clerk/viewer viewers/tabbed-board-viewer}
(def a-random-board (->> a-chess-board
                         core/board->symbolic
                         (apply concat)
                         shuffle
                         (partition (:row-n a-chess-board))
                         core/symbolic->board))


;; And how about Shuffle-chess style starting position?  That is: we shuffle only
;; the first row (behind the pawns), and the last row must be its mirror, so
;; that both players start from an "equal" army formation:[^fisher_style]
;; [^fisher_style]: I leave Fisher-style shuffle as an exercise for the reader:
;; we'd need to iterate on the shuffling until we get a position complying with
;; fisher's restrictions
^{::clerk/viewer viewers/tabbed-board-viewer}
(let [first-row (shuffle (first symboard))
      last-row (->> first-row (mapv utils/upper-case-keyword))]
  (-> symboard
      (assoc 0 first-row)
      (assoc (dec (count symboard)) last-row)
      core/symbolic->board))

;; ## Modeling Moves

;; We've been playing with pieces, boards and visualizations while avoiding the
;; real problem: that of modeling the rules of chess.[^procrast] In particular,
;; modeling piece moves. And how to decide which moves are valid?
;; [^procrast]: Was that procrastinating? or sharpening the axe? A bit of both?
;;
;; We'll tackle the problem as that of
;; _generating_ valid moves. So, the question becomes:
;;
;; > _How do we generate all valid moves given a board possition?_
;;
;; And this is the crux of this whole exercise.
;;
;; ### Representing Moves
;;
;; We will model a move as a simple map. Key `:steps` will contain a list
;; of (one or more) _steps_. Each step includes the piece it affects and the
;; resulting board state. The very first step represents the board position at
;; the start of this move.
;;
;; Here's a minimal example, using a board containing just two Rooks (using a
;; small 3x3 board just to save space). The move below represents moving
;; `:player` 0's Rook (`:r`) from `:pos [0 0]` (bottom-left corner) to `:pos [2
;; 0]. That's three steps: the initial position plus two steps to the right:

^{::clerk/visibility {:code :hide :result :show}
  ::clerk/auto-expand-results? true}
(def example-move
  {:steps
   (list
    {:board
     {:pieces
      #{{:type :r, :player 0, :pos [2 0], :flags #{}}
        {:type :r, :player 1, :pos [2 2], :flags #{}}},
      :row-n 3,
      :col-n 3},
     :piece {:type :R, :player 0, :pos [2 0]},
     :flags #{}}
    {:board #_(core/new-board 8 8 #{{:type :r, :player 0, :pos [1 0], :flags #{}}
                                  {:type :r, :player 1, :pos [7 7], :flags #{}}})
     {:pieces
        #{{:type :r, :player 0, :pos [1 0], :flags #{}}
          {:type :r, :player 1, :pos [2 2], :flags #{}}},
        :row-n 3,
        :col-n 3},
     :piece {:type :R, :player 0, :pos [1 0]},
     :flags #{}}
    {:board
     {:pieces
      #{{:type :r, :player 0, :pos [0 0], :flags #{}}
        {:type :r, :player 1, :pos [2 2], :flags #{}}},
      :row-n 3,
      :col-n 3},
     :piece {:type :R, :player 0, :pos [0 0]},
     :flags #{}})})

;;
;; It's easier to see if we render the boards within each step. Here's the same example again (click to expand):
;;
^{::clerk/visibility {:code :hide :result :show}
  ::clerk/auto-expand-results? true}
(clerk/with-viewers (concat [viewers/board-viewer] clerk/default-viewers)
  example-move)


;; And here is new viewer: a compact view of a `pmove` in a single board. Here's the same move again:

^{::clerk/viewer viewers/board-move-viewer
  ::clerk/visibility {:code :hide :result :show}}
example-move


;; ### Idea: Partial Moves

;; Moves will be constructed one step at a time. We start by creating a basic move
;; containing only the initial step (_step zero_, the starting position of the
;; move), and then we call a function to _expand_ the move (that is, to add a new step).

;; We'll call these "partial moves", `pmoves` for short.  A `pmove` that is not
;; `:finished?` is not yet a valid move, but may be expanded with more steps
;; until it is either discarded or it becomes `:finished?`.[^partial-moves-idea]
;
;; [^partial-moves-idea]: I stole this idea of "partial moves" from Chapter 2.4 of
;; _"Software Design for Flexibility"_ by Hanson and Sussman
;;
;; Let's create an initial `pmove` for the Rook in position `[0 0]` (using a
;; small 3x3 board to save space):
;;
^{::clerk/viewer viewers/side-by-side-move-viewer}
(def pmove-1
  (core/new-pmove (core/symbolic->board '[[r - k]
                                          [- - -]
                                          [R - K]])
                  (core/new-piece :r 0 [0 0])))

;; That's a new partial move, containing only the initial step with its starting
;; board (step zero), and thus it is (obviously) not `:finished`.
;;

;; ### Generating Move Steps
;;
;; Given the initial partial move above, we want a function that
;; _expands_ it, that is: that appends a new step to it.
;;
;; In chess, the valid possible moves for a piece depend on its type. So we'd
;; need to implement function for each type (a Rook in this example).

;; Now, here's what I think are the _key insights_ behind the idea of "partial
;; moves":
;;
;; * Our piece-specific functions only deal with generating _one_ single next
;; step. This will simplify their implementation
;; * The logic to repeatedly call these functions and collect the
;; `:finished?` moves is generic: independent from the type of piece, and even
;; from which game we are implementing
;;
;; Let's write a simplistic version of function to expand the `pmove` of a
;; Rook. We'll do a first baby step (no pun intended 🤡) and only move the piece
;; one square to the right (ignoring collisions, capturing or board dimensions):
^{::clerk/visibility {:code :show :result :hide}}
(defn expand-pmove-rook-right [pmove]
  (let [last-step (-> pmove :steps first)
        from-piece (:piece last-step)
        offset [1 0] ;; one X-coordinate to the right
        to-piece (update from-piece :pos
                         (fn shift-position [pos] (mapv + pos offset)))
        new-step (update-in last-step [:board :pieces]
                            (fn replace-piece [pieces] (-> pieces
                                                           (disj from-piece)
                                                           (conj to-piece))))
        new-pmove (update-in pmove [:steps] conj new-step)
        new-pmove-fin (assoc new-pmove :finished true)]

    [new-pmove new-pmove-fin]))

#_ (❶ ❻ ❼ ❾ ❿ ① ② ③ ④ ⑤ ⑥ ⑦ ⑧ ⑨ ⑩)

;;
;; Short explanation:
;;
;; 1. `offset` is the coordinates vector by which we want shift the piece position (just 1 on the first coordinate)
;; 1. `to-piece` becomes our piece with the shifted position
;; 1. `new-step` we create the new step by taking the previous step and replacing its `:piece` with the new one
;; 1. `new-pmove` append the `new-step` to the original pmove to get our new _expanded_ pmove
;; 1. `new-pmove-fin` Question: Is our new pmove _finished_? In other words: Is it a valid
;; complete Rook move? It is! rooks may move one or more squares... so we want
;; to return two `pmoves` with this new extra step: one as finished, and another
;; one as unfinished (meaning, we should keep expanding on it)

;; Let's call it:
^{::clerk/visibility {:code :show :result :hide}}
(expand-pmove-rook-right pmove-1)

^{::clerk/visibility {:code :hide :result :show}
  ::clerk/auto-expand-results? true}
(clerk/with-viewers (concat [viewers/board-viewer] clerk/default-viewers)
  (expand-pmove-rook-right pmove-1))

;; Here's a _debugger_ view that let's you step through the code execution:
^{::clerk/viewer clerk-storm/timeline-stepper-viewer
  ::clerk/width :full
  ::clerk/visibility {:code :hide :result :show} }
(clerk-storm/show-trace {:include-fn-names [] }
  (expand-pmove-rook-right pmove-1))


;; Some important observations about that first move-generation function we just
;; wrote:
;;
;; 1. It only moves the piece to the right. What do we need to move it left?
;; or up or down? Answer: just a different offset vector!. Like,  `[0 1]` to move
;; up

;; 1. It's for Rook moves only. Or is it?  How many lines of code are specific
;; to movements of a Rook?. Answer: again, just the `[1 0]` offset! For example,
;; doing the diagonal up-right movement of a Bishop would work by using offset `[1 1]`

;; 1. We should extract some expression into separate functions to keep our code
;; _at the same level of abstraction_. Some ideas:
;;     * `(pmove-move-piece pmove offset)`: create and append a new step
;;     * `(move-piece from-piece offset)`: shift a piece position
;;     * `(pmove-finish-and-continue pmove)` return the given pmove plus its
;; `:finished` version
;;     * `(expand-pmove-offsets offsets)` to do the logic on multiple _directions_

;; ### Move Constraints and Rules

;; Our simplistic move-generation function always adds an extra step, ignoring
;; board boundaries and other pieces on the board.
;;
;; We need extra checks to discard `pmoves` which don't comply with certain
;; restrictions, like: the piece went off the board, or it landed on a square
;; that is already occupied.
;;
;; For some pieces (like Rook and Bishop) we have to generate multiple steps for
;; a move, but on the same direction. Other pieces can do only one step per
;; move (King and Knight). Pawns are special: only one step, sometimes two.
;;
;; What if the step lands on a square occupied by an opponent piece?  In this
;; case, we need to modify our `pmove` to remove the opponent piece from the
;; board (and maybe record the capture). Pawns are different, again: they
;; move in one direction, but capture on another.


;; The keep a flexible design, we want to model all these constraints and
;; behaviors as independent functions operating on `pmoves`. These functions
;; become the primitives which we can then combine to implement to complete
;; logic for each piece.

;;
;; Let's see a how the current (non-naive) implementation of our Rook's function looks like:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
 "(defn expand-pmove-for-rook [pmove]
   (->> pmove
        (expand-pmove-dirs [↑ ↓ ← →])
        (pmoves-discard #(or (pmove-on-same-player-piece? %)
                             (pmove-changed-direction? %)))
        (map pmoves-finish-capturing-opponent-piece)
        (pmoves-finish-and-continue))))")

;; I hope the code is clear:
;; 1. expand the pmove on all those offsets (directions)
;; 1. discard the pmove's which land on the same player's piece, or change direction (Rooks can only move straight)
;; 1. enrich pmoves which capture an opponent piece
;; 1. for all remaining pmoves, clone them to return a version with `:finished? false` and `:finished? true`


;; The implementation of Bishop and Queen is pretty much identical to the
;; Rook. So let's peak at the Knight:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
 "(defn expand-pmove-for-knight [pmove]
      (->> pmove
           (expand-pmove-dirs (core/flip-and-rotations [1 2]))
           (pmoves-discard (some-fn (partial pmove-over-max-steps? 1)
                                    pmove-on-same-player-piece?))
           (map pmoves-finish-capturing-opponent-piece)
           (pmoves-finish-and-continue)))")

;; It only differs on the offsets, and in that it restricts max steps to 1.[^knight]
;; `flip-and-rotations` expands the given offset by first mirroring it
;; horizontally, and then generating four 90-degree rotations:
;; [^knight]: If you are surprised by the fact that there's no need for code about "jumping pieces", you are not the first one

(core/flip-and-rotations [1 2])


;; As I worked on implementing most rules of chess, the general pattern for each
;; move-generation function follows this shape:
;;
;; 1. _Expand_: For the given `pmove`, generate a new one for each of the offsets supported by the piece type
;; 1. _Discard_: those `pmoves` that don't pass either one of the checks (predicates) specified
;; 1. _Modify_ (or, _enrich_): update the `pmoves`, for example when capturing, or promoting
;; 1. _Finish_: Mark some `pmoves` as `:finished`, maybe keeping the original one as unfinished for the next iteration


;; Finally, let's look at the pawn. Here I found it simpler to thing the moving
;; and capturing as two combined "rules", each following the above pattern:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
"(defn expand-pmove-for-pawn [pmove]
      (concat
         (->> pmove ;; Move forward rule
              (expand-pmove-dirs [↑])
              (pmoves-discard (some-fn pmove-changed-direction?
                                       pmove-on-same-player-piece?
                                       (partial pmove-over-max-steps?
                                                (if (pmove-piece-1st-move? pmove) 2 1))))
              (pmoves-finish-and-continue))

         (->> pmove ;; Capture rule
              (expand-pmove-dirs [↖ ↗])
              (pmoves-discard #(or (not (pmove-on-other-player-piece? %))
                                   (pmove-over-max-steps? 1 %)))
              (map pmoves-finish-capturing-opponent-piece))))

         ;; En-passant rule: TODO
         ;; Promotion rule: TODO
")

;; Final comment: some implementations use `some-fn`, and `partial`, others use
;; `#(or ... )`, just for illustration purposes. The difference is just a matter
;; of style. You should pick one and be consistent.
;;

;; 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖

;; 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖

;; 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖

;; 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖 🔖


;; The core move-generation logic starts by calling the game-specific rules
;; functions with this initial pmove. Each rule function _expands_ this pmove by
;; returning a collection of (zero or more) new pmoves, each with an extra step added,
;; some of them `:finished`.
;;
;; The move-generation logic iterates over the rules functions again, passing
;; all the new pmoves that are not `:finished`, until we only have `:finished`
;; ones.
;;
;;
;; ---
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

;; ---
;;
;; In the case of chess, the valid moves for a piece depend on its type. So we'd
;; like to have a way to code the move-generation logic independently for each
;; piece type. However, some of them share similar characteristics (moves on
;; diagonals, or orthogonal, cannot go over other pieces, captures opponent
;; pieces, etc.), so it'd be nice to code this "behaviors" separately and
;; combine them to model each one of the piece types.
;;
;; ---
;;


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


;; ^{::clerk/viewer clerk-storm/trace-code-viewer ::clerk/budget nil}
;; (clerk-storm/show-trace {:include-fn-names [#"possible-pmoves"
;;                                             #"candidate-pmoves\*?"
;;                                             #"expand-pmove"] }
;;   (core/possible-pmoves game-1))


#_^{::clerk/viewer clerk-storm/timeline-stepper-viewer ::clerk/width :full }
(clerk-storm/show-trace {:include-fn-names [#"possible-pmoves" #"candidate-pmoves\*?" #"expand-pmove"] }
  (core/possible-pmoves game-1))




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







;; ### automated Tests

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
