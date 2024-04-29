^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns gameboard
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk-viewer]
            [gameboard-exercise.clerk-viewers :as viewers]
            [gameboard-exercise.core :as core]
            [gameboard-exercise.chess :as chess]
            ;;            [gameboard-exercise.checkers :as checkers]
            [gameboard-exercise.utils :as utils]))

;; # Experimenting with simple board games
;;
;; In this project we create a simple model for grid-based board games.
;
;; Based on a core shared model, we can implement tic-tac-toe, chess, and
;; checkers.
;;
;; The goal is purely pedagogical: to discover and learn about good software
;; design, functional programming, interactive and "moldable" development.
;;
;; This can be seen as an example of "stratified design": when programs are
;; structured as levels, each of which defines a small "language" for a
;; particular problem domain and provides new primitives to build higher
;; solutions on top off. So, instead of modeling the game of Checkers (or
;; Chess) directly, we first extract a more generic domain language for board
;; games, providing a common basic language which can be used to model multiple
;; different games.
;;
;;
;; Some references:
;;  * book: SICP (Structure and Interpretation of Computer Programs) by Abelson and Sussman
;;  * book: Software Design for Flexibility (Chris Hanson and Gerald Jay Sussman)
;;  * Paul Graham's bottom-up design essay (http://www.paulgraham.com/progbot.html)
;;  * Lisp: a lang for stratified design
;;  * book: Grokking Simplicity (by Eric Normand)
;;  * maybe: blog about layers vs strata (???)
;;
;; ## Namespaces
;; The core is organized as follows:main
;;
;; |namespace | description|
;; |----------|------------|
;; |core | blah|
;; |viewers |blah |
;; |board | |
;; |checkers | |
;; |chess | |
;; |ttt | |

{::clerk/visibility {:code :show}
 ::clerk/auto-expand-results? true
 ::clerk/budget 1000}

^{::clerk/visibility {:code :hide :result :hide}}
(clerk/reset-viewers! clerk/default-viewers)


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

^{::clerk/visibility {:code :hide :result :hide}}


#_ ^{::clerk/visibility {:code :hide :result :hide}} (clerk/add-viewers! [viewers/board-viewer])

#_ ^{:nextjournal.clerk/visibility {:code :hide :result :hide}} (clerk/add-viewers! [viewers/chess-board-viewer])

#_^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(clerk/add-viewers! [viewers/board-viewer viewers/board-move-viewer])



;; ## Scratchpad / testing area
;; ## See impl here: [Chess NS](../src/gameboard_exercise/chess)
#_(clerk/html [:div "See the impl here: " [:a {:href "../src/gameboard_exercise/chess"} "Chess NS"]])




;; ## Some empty boards

(core/empty-board 8 8 )


;; Now, to make boards easier to see, we'll use a special viewer like so:

^{::clerk/visibility {:code :hide :result :show}}
(render-board-expression
 (core/empty-board 8 8 ))


^{::clerk/visibility {:code :hide :result :show}}
(render-board-expression
 (core/empty-board 12 12))


^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(comment
;; ## Initializing a board from symbols
  ^{::clerk/visibility {:code :show :result :hide}}
  (def board1 (core/symbolic->board [[:- :- :- :- :- :- :- :-]
                                     [:- :- :- :- :- :- :- :-]
                                     [:- :- :- :k :- :- :- :-]
                                     [:- :- :- :p :- :- :- :-]
                                     [:- :- :- :- :P :- :- :-]
                                     [:- :- :- :- :K :- :- :-]
                                     [:- :- :- :- :- :- :- :-]
                                     [:- :- :- :- :- :- :- :-]]))

  ^{::clerk/visibility {:code :hide :result :show}}
  (clerk/row [(clerk/with-viewers clerk/default-viewers
                ^{::clerk/auto-expand-results? true}
                board1)
              (clerk/with-viewer viewers/board-viewer
                board1)]))


;; ## Initializing a board from symbols
^{::clerk/visibility {:code :hide :result :show}}
(render-board-expression-str
 "(core/symbolic->board [[:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :k :- :- :- :-]
                       [:- :- :- :p :- :- :- :-]
                       [:- :- :- :- :P :- :- :-]
                       [:- :- :- :- :K :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]])")



;; ## Tinker with a Chess board

^{::clerk/visibility {:code :show :result :hide}}
(def a-chess-board (chess/initiate-chess-board))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/row [(clerk/with-viewers clerk/default-viewers
            ^{::clerk/auto-expand-results? true}
              a-chess-board)
            (clerk/with-viewer viewers/board-viewer
             a-chess-board)])


(def symboard (core/board->symbolic a-chess-board))

;; ### Shuffle all the pieces
^{::clerk/visibility {:code :hide :result :show}}
(render-board-expression-str
   "(def a-random-board (->> symboard
                         (apply concat)
                         shuffle
                         (partition 8)
                         core/symbolic->board))")



^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(def upper-case-keyword (comp keyword str/upper-case name))

;; ### Random Fisher-style shuffling
;; That is: we shuffle the first row only, and the last row must be its mirror

^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(render-board-expression-str
  "(let [first-row (shuffle (first symboard))
        last-row (->> first-row (map upper-case-keyword) reverse vec)]
    (-> symboard
        (assoc 0 first-row)
        (assoc 7 last-row)
        core/symbolic->board))")

;; ## OK. Let's start chess game...
(def game-1 (core/start-game chess/chess-game))

;; ## Give me all possible moves...
(count (core/possible-pmoves game-1))

^{::clerk/viewer clerk-viewer/map-viewer
  ::clerk/auto-expand-results? false}

(first (core/possible-pmoves game-1))

;; Let's sort the moves by piece x-coordinate, and show them visually:
(clerk/row (sort-by #(-> % :steps first :piece :pos first)
                    (core/possible-pmoves game-1)))


;;
;; ## Let's start a new chess game off the random board above
;;
(def a-random-game (core/start-game chess/chess-game a-random-board))

;; ## Give me all possible moves...

(count (core/possible-pmoves a-random-game))

(clerk/row (core/possible-pmoves a-random-game))

;; ## Only the final board of the possible moves...

(clerk/row  (->> (core/possible-pmoves a-random-game)
                 (map (comp :board first :steps))))

;; ## Only those of the Rooks...

(clerk/row  (->> (core/possible-pmoves a-random-game)
                 (filter (fn [pmove]
                           (= :r (-> pmove :steps last :piece :type))))
                 #_(map (comp :board first :steps))))

#_(clerk/row (map #(-> % :steps first :board core/board->symbolic) (take 2 (core/possible-pmoves game-1))))
#_(-> (first (core/possible-pmoves game-1)) :steps last :piece :pos)

#_(clerk/row
   (map #_#(update-in % [:steps] reverse)
    #(first (:steps %))
        (filter (fn [pmove]
                  (= [1 1] (-> pmove :steps last :piece :pos)))
                (core/possible-pmoves game-1))))
