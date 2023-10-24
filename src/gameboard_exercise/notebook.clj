^{:nextjournal.clerk/visibility {:code :hide}}
(ns gameboard-exercise.notebook
  (:require [nextjournal.clerk :as clerk]
            [gameboard-exercise.clerk-viewers :as viewers]
            [gameboard-exercise.core :as core]
            [gameboard-exercise.chess :as chess]
            ;;            [gameboard-exercise.checkers :as checkers]
            [gameboard-exercise.utils :as utils])  )


{::clerk/visibility {:code :show}
 ::clerk/auto-expand-results? true
 ::clerk/budget 1000
 }



;; # Experimenting with board games

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(clerk/add-viewers! [viewers/board-viewer])

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(def upper-case-keyword (comp keyword clojure.string/upper-case name))


;; ## Some empty boards
(core/empty-board 8 8)

(core/empty-board 10 10)

(core/empty-board 12 12)


;; ## Initializing a board from symbols
(core/symbolic->board [[:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :p :k :- :- :-]
                       [:- :- :- :K :P :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]])


;; ## Tinker with a Chess board
(def board (chess/initiate-chess-board))

(def symboard (core/board->symbolic board))

;; ### Shuffle all the pieces
(->> symboard
     (apply concat)
     shuffle
     (partition 8)
     core/symbolic->board)

;; ### Random Fisher-style shuffling
(let [first-row (shuffle (first symboard))
      last-row (->> first-row (map upper-case-keyword) reverse vec)]
  (-> symboard
      (assoc 0 first-row)
      (assoc 7 last-row)
      core/symbolic->board))

;; ## OK OK. Let's start chess game...
(def game-1 (core/start-game chess/chess-game))


;; ## Give me all possible moves...
(count (core/possible-pmoves game-1))

(clerk/row (core/possible-pmoves game-1))

;; ## Only the final board of the possible moves...
(clerk/row  (->> (core/possible-pmoves game-1)
                 (map (comp :board first :steps))))


#_ (clerk/row (map #(-> % :steps first :board core/board->symbolic ) (take 2 (core/possible-pmoves game-1))))
#_ (-> (first (core/possible-pmoves game-1)) :steps last :piece :pos)

#_(clerk/row
   (map #_ #(update-in % [:steps] reverse)
        #(first (:steps %))
        (filter (fn [pmove]
                  (= [1 1] (-> pmove :steps last :piece :pos )))
                (core/possible-pmoves game-1))))
