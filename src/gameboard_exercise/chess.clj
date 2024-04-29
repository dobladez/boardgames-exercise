
;; # Chess
^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(ns gameboard-exercise.chess
  (:require [gameboard-exercise.utils :refer [upper-case-keyword]]
            [nextjournal.clerk :as clerk]
            [gameboard-exercise.clerk-viewers :as viewers]
            [gameboard-exercise.core :as core :refer [dir-up dir-down dir-left dir-right dir-up-left
                                                      dir-up-right dir-down-left dir-down-right]]))

(clerk/add-viewers! [viewers/board-viewer])

(def initial-chess-symbolic-board [[:r :n :b :q :k :b :n :r]
                                   [:p :p :p :p :p :p :p :p]
                                   [:- :- :- :- :- :- :- :-]
                                   [:- :- :- :- :- :- :- :-]
                                   [:- :- :- :- :- :- :- :-]
                                   [:- :- :- :- :- :- :- :-]
                                   [:P :P :P :P :P :P :P :P]
                                   [:R :N :B :Q :K :B :N :R]])

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(defn initiate-chess-board []
  (core/symbolic->board initial-chess-symbolic-board))

(defn initiate-fisher-chess-board []
  (let [first-row (shuffle (first initial-chess-symbolic-board))
        last-row (->> first-row (map upper-case-keyword) reverse vec)]
    (-> initial-chess-symbolic-board
        (assoc 0 first-row)
        (assoc 7 last-row)
        core/symbolic->board)))

;; TODO: rules
(def chess-evolution-rules
  [(fn simple-pawn-rule [])])

(def chess-aggregate-rules [])

(def rook-dirs [dir-up dir-down dir-left dir-right])
(def bishop-dirs [dir-up-left dir-up-right dir-down-left dir-down-right])
(def all-dirs (concat rook-dirs bishop-dirs))
(def knight-moves [[-2 -1] [-1 -2] [+1 -2] [+2 -1]
                   [-2 +1] [-1 +2] [+1 +2] [+2 +1]])
(def king-basic-moves all-dirs)

;; ### Pawn partial move
(defmethod core/continue-pmove-for-piece :p  [pmove]
  (let [steps (reverse (:steps pmove));;; TODO remove reverse
        player (-> steps last :piece :player);; TODO easier
        direction (if (= 0 player) dir-up dir-down);; TODO extract to generic "(flip-direction player dirs)"
        unfinshed-pmove (core/pmove-append-piece-move pmove direction)
        finished-pmove (core/pmove-finish unfinshed-pmove)]

    ;; TODO capturing moves and en passant. Maybe seperate function?
    (if (or (> (count steps) 1)
            (-> steps first :piece (core/piece-flag? :moved)))
      (list finished-pmove)
      (list finished-pmove unfinshed-pmove))))

;; ### Rook
(defmethod core/continue-pmove-for-piece :r [pmove]
  (let [previous-dir (core/pmove-last-step-direction pmove)
        next-dirs (if previous-dir (list previous-dir) rook-dirs)
        new-pmoves (->> next-dirs
                        (map (partial core/pmove-append-piece-move pmove))
                        (remove core/pmove-on-same-player-piece?))]
    (concat new-pmoves (map core/pmove-finish new-pmoves))))



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
;;;
;;

;;
;; FEN notation
;;
(defn- piece->fen [piece]
  (case piece
    :- "."
    (name piece)))

(defn- row->fen [row]
  (let [row-str (apply str (map piece->fen row))
        grouped (re-seq #"\.+|[^\.]+" row-str)]
    (apply str (map (fn [s]
                      (if (.startsWith s ".")
                        (str (count s))
                        s))
                    grouped))))

(defn board->fen [board]
  (->> board
       (map row->fen)
       (interpose "/")
       (apply str)))

(def initial-board
  [[:r :n :b :q :k :b :n :r]
   [:p :p :p :p :p :p :p :p]
   [:- :- :- :- :- :- :- :-]
   [:- :- :- :- :- :- :- :-]
   [:P :- :- :- :- :- :- :-]
   [:- :- :- :- :- :- :- :-]
   [:- :P :P :P :P :P :P :P]
   [:R :N :B :Q :K :B :N :R]])

(board->fen initial-board)
