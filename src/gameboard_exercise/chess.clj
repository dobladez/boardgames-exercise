
;; # Chess
^{:nextjournal.clerk/visibility {:code :hide}}
(ns gameboard-exercise.chess
  (:require [nextjournal.clerk :as clerk]
            [gameboard-exercise.clerk-viewers :as viewers]
            [gameboard-exercise.core :as core :refer [dir-up dir-down dir-left dir-right dir-up-left
                                                      dir-up-right dir-down-left dir-down-right]]
            ))

^{:nextjournal.clerk/visibility {:code :hide :result :hide }}
(clerk/add-viewers! [viewers/board-viewer])

^{:nextjournal.clerk/visibility {:code :hide :result :hide }}
(def upper-case-keyword (comp keyword clojure.string/upper-case name))


(def chess-symbolic-board [[:r :n :b :q :k :b :n :r]
                           [:p :p :p :p :p :p :p :p]
                           [:- :- :- :- :- :- :- :-]
                           [:- :- :- :- :- :- :- :-]
                           [:- :- :- :- :- :- :- :-]
                           [:- :- :- :- :- :- :- :-]
                           [:P :P :P :P :P :P :P :P]
                           [:R :N :B :Q :K :B :N :R]])

^{:nextjournal.clerk/visibility {:code :show :result :hide }}
(defn initiate-chess-board []
  (core/symbolic->board chess-symbolic-board))


(def board1 (initiate-chess-board))

;; Shuffle the pieces
(->> board1
     core/board->symbolic
     (apply concat)
     shuffle
     (partition 8)
     core/symbolic->board)

(defn initiate-fisher-chess-board []
  (let [first-row (shuffle (first chess-symbolic-board))
        last-row (->> first-row (map upper-case-keyword) reverse vec)]
    (-> chess-symbolic-board
        (assoc 0 first-row)
        (assoc 7 last-row)
        core/symbolic->board)))

(initiate-fisher-chess-board)

(def chess-evolution-rules
  [
   (fn simple-pawn-rule []
     )
   ])

(def chess-aggregate-rules [])

(def rook-dirs [dir-up dir-down dir-left dir-right])
(def bishop-dirs [dir-up-left dir-up-right dir-down-left dir-down-right])
(def all-dirs (concat rook-dirs bishop-dirs))
(def knight-moves [[-2 -1] [-1 -2] [+1 -2] [+2 -1]
                   [-2 +1] [-1 +2] [+1 +2] [+2 +1]])
(def king-basic-moves all-dirs)

;; ### Pawn partial move
#_(defmethod core/continue-pmove-for-piece :p
    [pmove]
    (let [steps (reverse (:steps pmove))
          player (-> steps last :piece :player)
          direction (if (= 0 player) dir-up dir-down)
          unfinshed-pmove (core/pmove-append-piece-move pmove direction)
          finished-pmove (core/pmove-finish unfinshed-pmove)]

      (if (or (> (count steps) 1)
              (-> steps first :piece (core/piece-flag? :moved)))
        (list finished-pmove)
        (list finished-pmove unfinshed-pmove))))

(defn- pmove-last-step-direction [pmove]
  (when (> (count (:steps pmove)) 1)
    (let [[last-step prior-step] (:steps pmove)]
      (mapv - (-> last-step :piece :pos) (-> prior-step :piece :pos)))))

;; ### Rook partial move
#_ (defmethod core/continue-pmove-for-piece :r
     [pmove]
     (let [steps (:steps pmove)
           direction (pmove-last-step-direction pmove)
           new-pmoves (if direction
                        (list (core/pmove-append-piece-move pmove direction))
                        (map (partial core/pmove-append-piece-move pmove) rook-dirs))]
       (concat new-pmoves (map core/pmove-finish new-pmoves))
       #_(map core/pmove-finish new-pmoves)))

#_(core/defgame chess-game
    {:name "Chess"
     :player-count 2
     :initiate-board-fn initiate-chess-board
     :expansion-rules []
     :aggregate-rules [] })

(core/defgame chess-game "Chess" 2 initiate-chess-board chess-evolution-rules chess-aggregate-rules)

(core/defgame fisher-chess-game "Chess" 2 initiate-fisher-chess-board chess-evolution-rules chess-aggregate-rules)

;; Usage
(def aGame (core/start-game chess-game))
