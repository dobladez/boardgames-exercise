^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns walkthrough
  {:nextjournal.clerk/toc true}
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk-viewer]
            [nextjournal.clerk.test :as clerk.test]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            ;;            [boardgames.checkers :as checkers]
            [boardgames.utils :as utils]
            [boardgames.test-utils :as t]
            [boardgames.clerk-testrunner] ;; <-- needed to trigger test runner
            [flow-storm.clerk :as clerk-storm]))

;; # Modeling chess: Code Walkthrough

^{::clerk/visibility {:code :hide :result :show}}
(clerk/html
 [:ol.border-b
  [:li [:a {:href "../../"} "Welcome"]]
  [:li [:a {:href (clerk/doc-url "notebooks/intro")} "Introduction"]]
  [:li [:strong "Code Walkthrough" ]]
  [:li [:a {:href (clerk/doc-url "notebooks/closing")} "Closing Thoughts"]]])


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
;; [^clojure_maps]: a data structure to store key-value pairs. Aka "dictionary", "hashmap", "associative array" in some languages

^{:nextjournal.clerk/visibility {:code :show :result :hide}
  ::clerk/auto-expand-results? true}
{ :pieces #{}
  :row-n 8
  :col-n 8 }

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

;; We'll use the [`core`](../../src/boardgames/core/) namespace for the code that is generic about the domain
;; of grid games. And we'll use another namespace for each game, like [`chess`](../../src/boardgames/chess/).

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

;; **💡Tip:** Do spend time dreaming-up new tools to make your job easier, less
;; error-prone, more "explainable", visual, easier to monitor, etc. In my
;; experience, most of us developers do not spend enough time working on our own
;; tools... we just work with what's already available to us.

;; So, I wrote a simple viewer to visualize our boards. Let's create a few empty
;; boards of different sizes to try it out. The resulting data structure renders
;; on the left, and a graphical view of it on the right:
^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 3 3)

^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 2 8)

^{::clerk/viewer viewers/side-by-side-board-viewer}
(core/empty-board 8 8)


;; We are not going to go into the boards rendering code right now. If you are curious, feel free to peek into the [`clerk-viewers`](../../src/boardgames/clerk_viewers) namespace.[^clerk-viewers-impl]
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
;; **💡Tip:** This is true for any problem domain: it's always good to make your code as
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
;; [`chess`](../../src/boardgames/chess/) namespace). Its implementation is almost exactly
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


;; **💡Tip:** This technique of having another an alternate representation for
;; an domain entity is quite common and useful in many situations. Some problems
;; become easier from a different perspective/representation.

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
;; We will model a move as a list of _steps_. Each step includes the piece it
;; affects and the resulting board state. The very first step represents the
;; board position at the start of the move.[^tracking-steps]
;; [^tracking-steps]: Keeping track of each step is useful for the
;; move-generation logic, as we'll soon see
;;
;; Here's an example move, using a small board containing just two Rooks. The
;; structure below represents moving `:player 0`'s Rook (`:r`) from `:pos [0
;; 0]` (bottom-left corner) to `:pos [2 0]` (bottom-right). That's three steps:
;; the initial position plus two steps to the right. Rendering only the boards
;; within each step (click to expand):


^{::clerk/visibility {:code :hide :result :hide}
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

^{::clerk/visibility {:code :hide :result :show}
  ::clerk/auto-expand-results? true}
(clerk/with-viewers (concat [viewers/board-viewer] clerk/default-viewers)
  example-move)

;; Here we introduce a more compact viewer for moves, showing all steps in a single board. Here's the same move again:
^{::clerk/viewer viewers/side-by-side-move-viewer
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

^{::clerk/visibility {:code :show :result :hide}}
(def test-board (core/symbolic->board '[[r - k]
                                        [- - -]
                                        [R - K]]))


^{::clerk/visibility {:code :show :result :show}
  ::clerk/viewers (concat [viewers/board-viewer] clerk/default-viewers)
  ::clerk/auto-expand-results? true}
(def pmove-1 (core/new-pmove test-board
                             (core/new-piece :r 0 [0 0])))

;; That's a new bare partial move. Since it only contains the initial step with its starting
;; board (step zero), it's (obviously) not _finished_ ( `:finished? false` ).
;;

;; ### Expanding Partial Moves
;;
;; Given the initial partial move above, we want a function that _expands_
;; it. That is: we need a function that receives a `pmove` and returns one or
;; more `pmove`'s with an extra step added to them.
;;
;; In chess, the valid possible moves for a piece depend on its type. So we'll
;; implement a function for each type (a Rook in this example).

;; Now, here's what I think are the _key insights_ behind the idea of "partial
;; moves":
;;
;; * Our piece-specific functions only deal with generating _one_ single next
;; step. This will simplify their implementation
;; * The logic to repeatedly call these functions and collect the
;; `:finished?` moves is generic: independent from the type of piece, and even
;; from which game we are implementing
;;
;; Let's write our first function to expand the `pmove` of a Rook. We'll do a first
;; baby _step_ (no pun intended 🤡): we'll move the piece one square to the
;; right (ignoring collisions, capturing, and board dimensions):
^{::clerk/visibility {:code :show :result :hide}}
(defn expand-pmove-rook-v1 [pmove]
  (let [last-step (-> pmove :steps first)
        from-piece (:piece last-step)
        offset [1 0] ;; one X-coordinate to the right
        to-piece (update from-piece :pos #(mapv + % offset))
        new-step (-> last-step
                     (assoc :piece to-piece)
                     (update-in [:board :pieces]
                                (fn replace-piece [pieces] (-> pieces
                                                               (disj from-piece)
                                                               (conj to-piece)))))
        new-pmove (update-in pmove [:steps] conj new-step)
        new-pmove-fin (assoc new-pmove :finished? true)]

    [new-pmove new-pmove-fin]))

#_ (❶ ❻ ❼ ❾ ❿ ① ② ③ ④ ⑤ ⑥ ⑦ ⑧ ⑨ ⑩)

;;
;; Short explanation:
;;
;; 1. `offset` is the vector by which we want to shift the piece position (here just `+1` on the first coordinate)
;; 1. `to-piece` becomes our piece with the shifted position
;; 1. `new-step` we create the new step by taking the previous step and replacing its `:piece` with the new one
;; 1. `new-pmove` append the `new-step` to the original pmove to get our new _expanded_ pmove
;; 1. `new-pmove-fin` Question: Is our new pmove _finished_? In other words: Is it a valid
;; complete Rook move? It is! rooks may move one or more squares... so we want
;; to return two `pmoves` with this new extra step: one as finished, and another
;; one as unfinished (meaning, we may keep expanding on it)

;; Let's call it. Here's the code _debugger_ view that let's you step through
;; the code execution. Feel free to go straight to the end to see the final
;; result:


#_(comment
    ^{::clerk/visibility {:code :show :result :show}
      ::clerk/viewers (concat [viewers/board-viewer] clerk/default-viewers)
      ::clerk/auto-expand-results? true}
    (expand-pmove-rook-v1 pmove-1))

^{::clerk/viewers (concat [clerk-storm/timeline-stepper-viewer viewers/board-viewer] clerk/default-viewers)
  ::clerk/width :full
  ::clerk/visibility {:code :hide :result :show}}
(clerk-storm/show-trace {:include-fn-names []}
  (expand-pmove-rook-v1 pmove-1))


;; Some observations about the first pmove expansion function we just wrote:
;;
;; 1. It only moves the piece to the right. What do we need to move it to the left?
;; or up or down? Answer: just a different offset vector! like `[0 1]` to move
;; up

;; 1. It's for Rook moves only. Or is it?  How many lines of code are specific
;; to movements of a Rook?. Answer: again, just the `[1 0]` offset! For example,
;; to do the diagonal up-right movement of a Bishop we'd just use offset `[1 1]`

;; 1. We should extract some expressions into separate functions


;; **💡Tip** (About that last point): We want keep the code within our functions _at
;; the same level of abstraction_: in this case, we want to use the language of
;; pmoves, pieces, moves, and not that of artihmetic, indices or set operations. Some ideas:
;; * `(pmove-move-piece pmove offset)`: create and append a new step
;; * `(move-piece from-piece offset)`: shift a piece position
;; * `(pmove-finish-and-continue pmove)` return the given pmove plus its
;; `:finished` version * `(expand-pmove-offsets offsets)` to do the logic on
;; multiple _directions_

;; So, our first expansion function is imperfect and incomplete. That's
;; OK, it's just v1... we are trying the basic ideas. Before implementing all
;; logic for the Rook and the rest of the pieces, we should move up the
;; ladder and work on how these functions will be called to generate all possible
;; moves.
;;
;; **💡Tip:** In general, it's better to work towards having an end-to-end version of our
;; solution as soon as possible, even if not covering all cases and details. We
;; want to have a _walking skeleton_,[^walking-skeleton] put it to the test, and
;; then evolve it as you work interatively on filling the blanks.
;; [^walking-skeleton]: See: [Walking
;; Skeleton](https://wiki.c2.com/?WalkingSkeleton), [Tracer
;; Bullets](https://wiki.c2.com/?TracerBullets), and [Steel
;; Threads](https://www.cs.du.edu/~snarayan/sada/docs/steelthreads.pdf).  They
;; all account to the same idea: to achieve a minimal end-to-end implementation,
;; touching all components and layers of you architecture. The goal is to
;; minimize risk by trying out and integrating our main ideas as soon as
;; possible
;;

#(comment
   ;; Reminds me of this great drawing about "MVPs" (Minimal Viable Product):
   ;; ![MVP_image](https://blog.crisp.se/wp-content/uploads/2016/01/mvp.png). Not exactly the same
   )



;; ## Generating all candidate Moves

;; So, our _partial moves_ represent our moves "in the making", and we'll have
;; multiple functions to _expand_ them (say, one function per piece type).
;;
;; What we need is a function that will drive the process... the control
;; structure that will invoke each of those functions, multiple times as needed,
;; collecting all results.
;;
;; The inputs to our new function? A board position, the player's turn, and the
;; move-expansion functions, one for each piece type.
;;
;; Here's a copy of `candidate-pmoves*`, straight from the `core` namespace:
;;
^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
 "(defn candidate-pmoves*
  \"Generate all finished candidate pmoves for the given position and player\"
   [board player-turn expansion-rules]
   (->> board
 ①   :pieces
 ②   (filter #(= player-turn (:player %)))
 ③   (mapcat #(expand-pmove expansion-rules (new-pmove board %)))))")

;; 1. Gets all pieces on the board
;; 2. Keeps only those of the given player
;; 3. For each piece: create a new base `pmove` and call `expand-pmove` (shown below) passing the
;; move-expansion functions (`expansion-rules`)

;; BTW, `expansion-rules` is a map from piece type to function, like this:
^{::clerk/visibility {:code :hide :result :show}}
(clerk/code "{:r expand-pmove-rook-v1
 :b expand-pmove-bishop-v1
 :k expand-pmove-king-v1
 ;; ... and so on
             }")

;; And here's the heart of our move-expansion logic:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
 "(defn expand-pmove
  \"Give a starting base pmove, return the list of all valid (finished) pmoves\"
  [expansion-rules base-pmove]

 ①  (if-let [expand-pmove-step (get expansion-rules (pmove-piece-type base-pmove))]

 ②    (->> (expand-pmove-step base-pmove)
 ③         (mapcat (fn [p-move]
                  (cond (pmove-outside-board? p-move) '()
                         (:finished? p-move) (list p-move)
                         :else (expand-pmove expansion-rules p-move)))))
    (list)))")



;; 1. obtain the move-expansion function for our piece
;; 2. call it `(expand-pmove-step )` to expand our base `pmove` to a list of expanded `pmoves` (that is, with en extra step added)
;; 3. collect all resulting pmoves, by:
;; 4.  discarding those `pmoves` landing outside the board boundaries[^board-boundaries]
;; 5.  keeping the `:finished?` pmoves as-is
;; 6.  recursively expanding those pmoves which are not `:finished?`
;; [^board-boundaries]: Questionable... we could have this check within the move
;; functions, or somewhere else as _general_ rules passed as arguments. Some
;; games might event allow moving pieces to special "outside the board"
;; positions? Anyway... KISS, we can revise later
;;
;;
;; These two functions are on our `core` namespaces, since they are not
;; Chess-specific. Our goal is to keep anything game-specific down into the rule
;; functions.[^htdp-book]
;; [^htdp-book]: The code of `expand-pmove` roughtly follows the pattern of
;; _generative-recursive_ functions as described in book ["How To Design
;; Programs"](https://htdp.org/), [Chapter 26.1](https://htdp.org/2024-8-20/Book/part_five.html#%28part._.Adapting_the_.Design_.Recipe%29)
;;
;; *Warning*: this is recursive function with no checks: it depends on
;; well-behaving expansion functions... if these don't stop adding new
;; un-`:finished?` pmoves on each call, our function will diverge and never
;; terminate (we'll overflow the stack). Exercise: add safe guards to limit our
;; recursive function from going _overboard_ (another pun, intended 🤡).
;;
;; Let's try it. Again: go straight to the end to see the final result:

^{::clerk/viewers (concat [clerk-storm/timeline-stepper-viewer viewers/board-viewer] clerk/default-viewers)
  ::clerk/width :full
  ::clerk/visibility {:code :hide :result :show}}
(clerk-storm/show-trace
    {:include-fn-names []}
  (core/candidate-pmoves* test-board 0 {:r expand-pmove-rook-v1 }))



;; ## Writing Tests
;;
;; Now that we have the basics of our model and algorithm in place, it's a
;; good time to start writing automated tests.[^tdd-religion]
;; We want to complete the move-expansion function for the Rook, and then
;; code new ones for all piece types. It's crucial to have automated tests to
;; validate our implementations as we go.
;;
;; [^tdd-religion]: Worshipers of TDD would've started writing tests much
;; earlier. During the early stages of a new problem, I find Clojure's
;; live interactive environment more productive: It gives me the immediate
;; feedback I want. I start writing tests when I need... well... tests :-)
;;
;; Let's think about how to express test cases for `candidate-pmoves*`, together
;; with our first move-expansion function that we wrote ealier for Rooks. How
;; about something like this:
;;
;; 1. _Given_: our expansion function(s), a board position, and a particular piece on the board
;; 2. _When_: we generate all possible moves
;; 3. _Then_: we want the resulting moves to match our set of known expected moves
;;
;; So, our test cases must construct a base `pmove` for the initial board
;; possition and the list of expected _finished_ `pmoves`. Let's _hallucinate_
;; how we could write such tests.[^hallucinate] Remember our literal boards?
;; using vectors? It would be great if we could express both our initial board
;; and the expected moves as as literal boards. After trying multiple ideas, I
;; picked this syntax:

;; [^hallucinate]: I always found LLMs (like GPT/Copilot) _hallucinations_ to be a feature, not a bug. It's a very human-like behavior!

#_
^{::clerk/visibility {:code :show :result :hide}}
(t/expect-moves* {:expansion-rules {:r expand-pmove-rook-v1}}
                 {:piece :R}
                  ;; Initial position:
                '[[- - k]
                  [R - p]
                  [K - -]]

                  ;; Expected position:
                '[[[R - k]
                   [- - p]
                   [K - -]]

                  [[- - k]
                   [- R p]
                   [K - -]]

                  [[- - k]
                   [- - R] ;; <- Capturing the pawn
                   [K - -]]])
#_(comment
;; Easy to see, right? Better if we could express the expected boards
;; horizontally to save space. Like this:
  )

^{::clerk/visibility {:code :show :result :hide}}
(deftest example-test-1 []   ;; define a standard Clojure test

  ;; Dream-up a handy helper function that will run our test case:
  (t/expect-moves {:expansion-rules {:r expand-pmove-rook-v1}}
                  {:piece :R}
                  '[[- - k]
                    [R - p]
                    [K - -]]

                  '[[R - k] [- - k] [- - k]
                    [- - p] [- R p] [- - R]
                    [K - -] [K - -] [K - -]]))

;; Hallucinate no more: I wrote `expect-moves` for you already. We won't dig
;; into it here. Of course, feel free to peek at it on
;; namespace [`test-utils`](../../test/boardgames/test_utils) (warning: it's a bit
;; messy).
;;
;; It's written using _assertions_ from Clojure's standard testing
;; library. Our naive Rook function only implements movements to the
;; right (remember?), so it fails our test. Here's the output we get:

^{::clerk/visibility {:code :hide :result :show}
  ::clerk/auto-expand-results? true}
(clerk/code
 (let [sw (new java.io.StringWriter)]
   (binding [clojure.test/*test-out* sw]
     {:result (example-test-1)}
     (str sw))))

;; Hmmm... of course the standard test runner doesn't know how to format our
;; vectors to make them look like boards. Hard to compare the values of
;; `expected:` vs `actual:` on the output.
;;
;; Obvious idea: write a Clerk viewer for test failures! Function
;; `expect-moves`, in addition to using Clojure's standard test assertions, also returns useful data for this
;; purpose, a vector containing:
;;
;; 1. the original board position
;; 1. the expected boards
;; 1. the actual boards returned
;;
;; Let's call it directly, without Clojure's standard `deftest` (you may need to
;; expand the result to make it easier to see the vectors):

^{::clerk/visibility {:code :hide :result :show}
  ::clerk/auto-expand-results? true}
(def tc1-result (t/expect-moves {:expansion-rules {:r expand-pmove-rook-v1}}
                                {:piece :R}
                                '[[- - k]
                                  [R - p]
                                  [K - -]]

                                '[[R - k] [- - k] [- - k]
                                  [- - p] [- R p] [- - R]
                                  [K - -] [K - -] [K - -]]))

;; And with our test viewer:
^{::clerk/visibility {:code :hide :result :show}
   ::clerk/viewers (concat [viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer] clerk/default-viewers) }
(t/view-test-case  tc1-result)



;; Note that this viewer is very specific to our test cases. It'd be nice to
;; have a dashboard to show the results of all standard project test cases,
;; with custom viewers.  I actually started to prototype the idea (stealing from this repo
;; `https://github.com/nextjournal/clerk-test`)... still ugly and not generic, but to get an idea (you'd have to expand the failed case):
^{::clerk/visibility {:code :hide :result :show}
  ::clerk/viewers (concat [clerk.test/test-suite-viewer viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer] clerk/default-viewers)
  ::clerk/auto-expand-results? true}
(do
  #_(defonce test-run (boardgames.clerk-testrunner/run-tests-on-clerk))
  @clerk.test/!test-report-state )


#_^{::clerk/visibility {:code :hide :result :show}
  ::clerk/auto-expand-results? true}
(clerk/with-viewer clerk.test/test-suite-viewer
  @clerk.test/!test-report-state)

;; ### Automating the automatic tests

;; Writing test cases will be time-consuming, not only coming up with board
;; positions, but also laying out all expected moves.

;; Given that there are many open-source chess engines out there:

;; _Couldn't we write a wrapper that, given a board position, it calls a chess
;; engine to get all valid moves?_
;;
;; This may sound like cheating, because we are using an existing system
;; of what we want to implement ourselves. However, it's perfectly valid and
;; quite common. Examples:

;;   * If writing a compiler for a language that already exists: Wouldn't it
;; make sense to compare it against existing implementations?[^java-tck]

;;   * What about writing a SQL database?
;;   There's [SQLLogicTest](https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki)[^sfqllogictest]
;;   for that
;;
;;   * Even in business software: it's common to replace an old system with new
;; one, while maintaining many of the same business rules: makes sense to rely
;; on the old system to validate the new implementation.
;;
;; [^java-tck]: The [Java TCK (Technology Compatibility
;; Kit)](https://www.jcp.org/en/resources/tdk) is a set of tests for the Java platform. Not
;; just the language, but the Virtual Machines and standard library.
;;
;; [^sfqllogictest]: [SQLLogicTest](https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki)
;; is _"...designed to verify that an SQL database engine computes correct results
;; by comparing the results to identical queries from other SQL database
;; engines"_
;;
;; **💡Tip:** If there's an existing system you can use as a reference, use it!
;; It might serve as a good base against which to test your
;; implementation. There is no shame in that.

;; I leave that as fun exercise for the reader :-). I'd look into stockfish, and
;; interact with it's UCI (universal chess interface).
;;
;; ---

;; Alright, we have enough test tooling. Back to doing actual work...


;; ## Moves for all Chess pieces

;; Our simplistic move-expansion function _always_ adds an extra step, ignoring
;; what's on the board... if called in a loop or recursively, things will never
;; end!.  We need extra checks to discard `pmoves` which don't comply with
;; certain restrictions, like: if the piece went off the board boundaries, or if
;; it landed on a square that is already occupied.
;;
;; For some pieces (like Rook and Bishop) multiple steps are allowed for a move,
;; but all on the same direction. Other pieces can do only one step per
;; move (King). Pawns are special: only one step, sometimes two.
;;
;; What if the step lands on a square occupied by an opponent piece?  In this
;; case, we need to modify our `pmove` to remove the opponent piece from the
;; board (and maybe record the capture). Pawns are different, again: they
;; move in one direction, but capture on another.

;; To keep a flexible design, we want to model all these constraints and
;; behaviors as independent functions operating on `pmoves`. All these functions
;; become the _primitives_ which we can then combine to implement the complete
;; logic for each piece.
;;
;; This is where I spent most of the time modeling and trying different
;; alternatives. Here I'm showing you the current (final?) result at a high
;; level. All the code lives in the [`chess`](../../src/boardgames/chess/) namespace.[^draw-owl]
;; [^draw-owl]: This paragraph reminds me of the famous meme: [![img](https://i.kym-cdn.com/photos/images/original/000/572/078/d6d.jpg)](https://i.kym-cdn.com/photos/images/original/000/572/078/d6d.jpg)


;; ### The Rook v2
;; Let's see a how a more realistic implementation of our Rook's function looks like:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
 "(defn expand-pmove-for-rook [pmove]
   (->> pmove
   ①  (expand-pmove-dirs [↑ ↓ ← →])
   ②  (pmoves-discard #(or (pmove-on-same-player-piece? %)
                            (pmove-changed-direction? %)))
   ③  (map pmoves-finish-capturing-opponent-piece)
   ④  (pmoves-finish-and-continue))))")

;; The code is a pipeline, which takes a `pmove` and then:
;; 1. expands the pmove on the four offsets (directions) of the rook
;; 2. discards the pmove's which land on the same player's piece or change direction (Rooks can only move straight)
;; 3. modifies the pmoves that capture an opponent piece
;; 4. for all remaining pmoves, return two versions: one with `:finished? false` and one with  `:finished? true`


;; ### The Bishop and Queen

;; The implementations for the Bishop and Queen are pretty much identical to
;; that of the Rook, except for the initial direction offsets. So I won't copy
;; them here.

;; A dummy code linter might complain here saying that there's too much "code
;; duplication"... and we should "factor out" the common code of
;; rook/bishop/queen here. We must keep our code "DRY"[^dry], right?

;; Not really... I'm perfectly fine here keeping the definition of each piece
;; independent of each other, and expressed clearly.
;; [^dry]: DRY (Don't Repeat Yourself) principle
;;
;; **💡Tip:** Sometimes we abuse the DRY principle. It's not about not repeating
;; "lines of code"; it's about not repeating a _piece of knowledge_ on multiple
;; places to avoid the risk of making a future change only on one of those
;; places. I don't see that risk here. Actually, if we want to experiment with
;; chess variations, we'd be better off keeping the rules for each piece
;; self-contained.
;;


;; ### The Knight

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

;; Note how for Knights it makes sense to use a single step for the whole
;; move. Nobody said that our offsets are limited to increments of 1!

;; ### See a general pattern here?
;;
;; As we work on implementing all piece rules, you can see that the general pattern for each
;; move-expansion function follows this shape:
;;
;; 1. _Expand_: For the given `pmove`, generate a new one for each of the offsets supported by the piece type
;; 1. _Discard_: those `pmoves` that violate any of the constraints (predicates) specified
;; 1. _Modify_ (or, _enrich_): update the `pmoves`, for example when capturing, or promoting
;; 1. _Finish_: Mark some `pmoves` as `:finished`, maybe keeping the original one as unfinished for the next iteration

;; ### The Pawn

;; Finally, let's look at the pawn. Here I found it simpler to think about moving
;; and capturing as two combined "rules":

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

;; Note: I'm showing some implementations using `some-fn` and `partial` for
;; composing functions, others use `#(or ... )` (anonymous lambda
;; functions). This is just for illustration purposes. The difference is just a
;; matter of style. You should pick one and be consistent.


;; ### The King. Finally
;;
;; The basic movements of a king are just like the Rook/Bishop/Queen (differing
;; only on the offset directions), with the only exception that it's limited to
;; one step.

;; There's also _castling_, which is a pretty different type of move.
;;
;; Here's the expansion function for the king:
^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
 "(defn expand-pmove-for-king [pmove]
  (concat
   (->> pmove ;; Regular move
        (expand-pmove-dirs (mapcat core/rotations [↑ ↗]))
        (pmoves-discard (some-fn pmove-on-same-player-piece?
                                 (partial pmove-max-steps? 1)))
        (map pmove-finish-capturing)
        (map pmove-finish))

   (->> pmove ;; Castling
     ① (expand-pmove-dirs [← →])
     ② (pmoves-discard (some-fn pmove-on-same-player-piece?
     ③                          pmove-changed-direction?
     ④                          (complement pmove-piece-1st-move?)
     ⑤                          (partial pmove-max-steps? 2)
     ⑥                          pmove-king-in-check?))
        (map pmoves-finish-castling)))")

;; You can hopefully "read" the rule of castling on the second section of that function:
;; 1. move horizontally
;; 1.  stop if square occupied by same player
;; 1.  stay on straight line
;; 1.  only if King never moved before
;; 1.  stop expanding at two steps
;; 1.  king cannot be in check (not even on the intermediate steps!). We'll see how `pmove-king-in-check?` does its magic on the next section

;; Function `pmoves-finish-castling` handles the rest of the (non-trivial)
;; details: in ensures there's a rook on the right place which never moved
;; before, no other piece in between, and if all is good, it moves the rook to
;; other side of the King
^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
 "(defn- pmoves-finish-castling [pmove]
  (if (not= 3 (count (-> pmove :steps)))
    pmove

    (let [[last-step prior-step]  (pmove :steps)
          board (:board last-step)
          last-step-pos (-> last-step :piece :pos)
          prior-step-pos (-> prior-step :piece :pos)
          offset (mapv - last-step-pos prior-step-pos)
          player (-> last-step :piece :player)
          next1-piece (board-find-piece board {:pos (-> last-step-pos (pos+ offset))})
          next2-piece (board-find-piece board {:pos (-> last-step-pos (pos+ offset) (pos+ offset))})]

      (if (or (rook-for-castling? next1-piece player)
              (and (nil? next1-piece) (rook-for-castling? next2-piece player)))
        (let [target-rook (or next1-piece next2-piece)]
          (-> pmove
              (core/pmove-update-last-step #(core/step-move-piece-to % target-rook prior-step-pos))
              pmove-finish))

        pmove))))")

;; That handles king-side and queen-side castling for both white and black.

;; Finally, the one HUGE difference with other pieces is that the King cannot be
;; left in check! Note that this limitation applies not only to the moves of the
;; King itself but to all other piece moves too. Therefore, we cannot impose
;; this constraint within the expansion rule of the King only... we need a way
;; to encode more general rules.  That's what we'll tackle on the next
;; section.

;; ## _Aggregate_ rules

;; Up until now, we've implemented game-specific rules as piece-specific pmove
;; expansion rules. As we've discussed for the case of keeping one's King out of
;; check, that's not enough. We want to define some rules that apply across
;; pmoves.

;; The idea (again, taken from Sussman's book) is to define one or more
;; _aggregate_ rules, which our core move-generation function will call after
;; calling all pmove _expansion_ rules, giving them a chance to veto or enrich
;; the `:finished` pmoves coming out of the `expansion` phase.

;; It should now become clear why we called that function on prior sections
;; _`candidate-pmoves*`_: they were still not officially legal moves... just the
;; final candidates before passing through the aggregate rules.


;; ### Aggregate Rule: King never in check

;; Let's then implement our aggregate rule to discard all `pmoves` which leave
;; our own King in check:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
 "(defn discard-king-checked-pmoves [pmoves]
  (->> pmoves (remove pmove-king-in-check?)))")

;; And, of course, `pmove-king-in-check?`:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
"(defn pmove-king-in-check? [pmove]
  (let [last-step (-> pmove :steps first)
        board (:board last-step)
        player (core/opponent-player (-> last-step :piece :player))
  ①     reply-moves (core/candidate-pmoves* board
                                            player
                                            (dissoc chess-expansion-rules :k))]
  ② (some captures-king? reply-moves)))")


;; Our aggregate rule:
;; 1. Call back to `core/candidate-pmoves*` (co-recursion?), passing all expansion rules
;; *except* for that of the King (otherwise, it might go infinitely since we
;; also use `pmove-king-in-check?` from the King's expansion rule
;; for castling.[^aggregate-rules-unfinished])
;; 2. Return whether any of the possible follow-up moves may capture the King
;; [^aggregate-rules-unfinished]: For the castling case, an alternative
;; implementation could be to have another list of aggregate rules to apply on the un-finished
;; pmoves during expansion and do the check there.


;; ### Pending Aggregate Rules
;;
;; There are more aggregate rules we should implement for our rules of chess to be complete. For instance:

;; * detect checkmate
;; * detect stalemate (when the player in turn has no legal moves to make while their King is not in check. It's a draw)
;; * Might be useful to implement pawn's en-passant rule
;;

;; ## Defining a "Game"

;; So far we've been growing our chess language from the bottom up: modeling
;; pieces, boards, partial moves, move-generation applying expansion and
;; aggregate rules, and the rule functions.
;;
;; Except for the rule functions, everything else is generic: not chess-specific. These general functions live under `core`.
;;
;; We need to model the concept of a _game_ definition, for example for chess. What constitues a game definition? So far we have:
;; * Expansion rules
;; * Aggregate rules
;; * Board dimensions
;; * Initial board position
;; * A name :-)

;; So, in `core` we have a simple helper function to create a new game definition:
^{::clerk/visibility {:code :show :result :hide}}
(defn make-game [name initiate-board-fn expansion-rules aggregate-rules]
  {:name name
   :initiate-board-fn initiate-board-fn
   :expansion-rules expansion-rules
   :aggregate-rules aggregate-rules})


;; and then, on our `chess` namespace we can define our game:
^{::clerk/visibility {:code :hide :result :show}}
(clerk/code "(def chess-game (core/make-game
                  \"Chess\"
                  initiate-chess-board
                  chess-expansion-rules
                  chess-aggregate-rules))")


;; ## Starting a "Game"

;; We have our game defined. How we'd need what will be the "public interface"
;; for using (playing) games.
;;
;; The first thing we need is a way to start a game. That is, to get an _instance_ of a game. In `core`:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code "(defn start-game
  \"Starts a new game for the given game definition and (optional) initial board\"
  ([gamedef]
   (start-game gamedef ((:initiate-board-fn gamedef))))
  ([gamedef initial-board]
   {:game-def gamedef
    :board initial-board
    :turn 0}))")

;; Function `start-game` returns what is the initial _state_ of a game, with a reference to the game definition.
;
;; So now we can use our chess game definition to start a new game instance:

^{::clerk/viewers (concat [viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer] clerk/default-viewers)
  ::clerk/auto-expand-results? true}
(def game-1 (core/start-game chess/chess-game))


;; And generate all possible moves for the start position... here sorted by the landing position of the moved piece:
#_(clerk/row
 (map (partial clerk/with-viewer viewers/tabbed-board-move-viewer)
      (sort-by #(-> % :steps last :piece :pos)
               (core/possible-pmoves game-1))))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code
 "(sort-by #(-> % :steps last :piece :pos)
          (core/possible-pmoves game-1))")

^{::clerk/visibility {:code :hide :result :show}}
(clerk/row
 (map (partial clerk/with-viewer viewers/board-move-viewer)
      (->> (core/possible-pmoves game-1)
           (sort-by #(-> % :steps last :piece :pos)))))




;; ## Next... (more exercises for the reader)
;;
;; For a full working version of our model, we need to _apply_ a possible move to our game state.
;; We'd need to keep track of the history of moves as part of that state.
;;
;; We will probably also need game-specific rules at the game-state level. For
;; instance, to decide whether the game is over or not, to tell who won!, and
;; the threefold repetition rule.
;;
;; With that in place, we could, for example, implement a user-interface to play games. We'd
;; basically need to implement the _game loop_.

;; And, of course: the goal was to model a generic core, so we could implement the rules for other games. For example:
;;
;; * Tic-Tac-Toe: One interesting thing about it is that pieces are added to the
;;   board, not moved. So, we'd probably need our `expansion-rules` map to have
;;  a place for that
;; * Checkers: Sussman's book does implement the rules of checkers, you can steal from there :-)
;; * Connect-four?
;; * Othello?
;; * Go? (the board would need to render pieces on the corners instead of inside squares, but's its still a grid board)
;;
;; Going crazy, we could generalize the _board_ to be a graph (not just a
;; grid)... which would acomodate a lot more games.
;;
;; ## Thanks!
;;
;; If you really read this far... **Thanks!**. I've learned quite a bit and had
;; some fun writing this. I hope you got someting out of it too.
;;
;; I'd really appreciate any ideas and suggestions for improvements!
;;
;; See [Closing Thoughts](../closing) for suggested references and further reading.
;;

#_

(comment
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

  #_^{::clerk/viewer clerk-storm/timeline-stepper-viewer ::clerk/width :full}
  (clerk-storm/show-trace {:include-fn-names [#"possible-pmoves" #"candidate-pmoves\*?" #"expand-pmove"]}
    (core/possible-pmoves game-1))
  )




#_(comment
    ;; Show dynamic function calls
    ;; Here's a viewer that evaluates a piece of code, traces all runtime
    ;; information using ClojureStorm, and shows the main function calls with their
    ;; arguments:
    )





#_(comment
  ;; Just for kicks, let's do the same off the random board we've created at the beginning:
  (def a-random-game (core/start-game chess/chess-game a-random-board))

 (clerk/row
  (map (partial clerk/with-viewer viewers/tabbed-board-move-viewer)
       (sort-by #(-> % :steps last :piece :pos)
                (core/possible-pmoves a-random-game)))))
