
;; # Chess
^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(ns gameboard-exercise.chess
  (:require [gameboard-exercise.utils :refer [upper-case-keyword]]
            [nextjournal.clerk :as clerk]
            [gameboard-exercise.clerk-viewers :as viewers]
            [gameboard-exercise.core :as core :refer [dir-up dir-down dir-left dir-right dir-up-left
                                                      dir-up-right dir-down-left dir-down-right]]))

(clerk/add-viewers! [viewers/board-viewer])

(def __initial-chess-symbolic-board [[:r :n :b :q :k :b :n :r]
                                   [:p :p :p :p :p :p :p :p]
                                   [:- :- :- :- :- :- :- :-]
                                   [:- :- :- :- :- :- :- :-]
                                   [:- :- :- :- :- :- :- :-]
                                   [:- :- :- :- :- :- :- :-]
                                   [:P :P :P :P :P :P :P :P]
                                   [:R :N :B :Q :K :B :N :R]])

(def initial-chess-symbolic-board '[[r n b q k b n r]
                                    [p p p p p p p p]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [P P P P P P P P]
                                    [R N B Q K B N R]])

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
        unfinshed-pmove (core/pmove-move-piece pmove direction)
        finished-pmove (core/pmove-finish unfinshed-pmove)]

    ;; TODO capturing moves and en passant. Maybe seperate function?
    (if (or (> (count steps) 1)
            (-> steps first :piece (core/piece-flag? :moved)))
      (list finished-pmove)
      (list finished-pmove unfinshed-pmove))))

;; ### Rook
(defmethod core/continue-pmove-for-piece :r_OLD [pmove]
  (let [previous-dir (core/pmove-last-step-direction pmove)
        next-dirs (if previous-dir (list previous-dir) rook-dirs)
        new-pmoves (->> next-dirs
                        (map (partial core/pmove-move-piece pmove))
                        (remove core/pmove-on-same-player-piece?))]
    (concat new-pmoves (map core/pmove-finish new-pmoves))))

;;
;; Rook v2 (WIP
;;


;; possible combinators:
;; 1. move piece in given direction:  pmove -> [pmove]
;; 2. 2nd+ steps on same direction :  pmove -> pmove | nil (or [pmove] -> [pmove])
;; 3. stop on same-player piece (obstruction):  pmove -> pmove | nil (or [pmove] -> [pmove])
;; 4. stop on board edge :  pmove -> pmove | nil (or [pmove] -> [pmove])
;; 5. capture on opponent piece :  pmove -> pmove
;; 6. capture on en passant :  pmove -> pmove
;; 7. promotion :  pmove -> pmove
;; 8. castling :  pmove -> pmove
;; 9. check :  pmove -> pmove
;; 10. checkmate :  pmove -> pmove
;; 11. stalemate :  pmove -> pmove


;; (defn pmove-move-piece-dirs [dirs pmove]
;;   (map (partial core/pmove-move-piece pmove) dirs))

(defn pmove-move-piece-same-dir [possible-dirs pmove]
  (let [previous-dir (core/pmove-last-step-direction pmove)
        next-dirs (if previous-dir (list previous-dir) possible-dirs)]
    (->> next-dirs
         (map (partial core/pmove-move-piece pmove)))))

(defn pmoves-stepping-own-disallowed [pmoves]
  (remove core/pmove-on-same-player-piece? pmoves))

(defn pmoves-finish-at-one-or-more-steps [pmoves]
  (concat pmoves (map core/pmove-finish pmoves)))

(defn pmoves-finish-at-one-step [pmoves]
  (concat pmoves (map core/pmove-finish pmoves)))

(defn pmoves-finish-capturing-opponent-piece [pmove]
;; TODO

  #_(concat pmoves (map core/pmove-finish pmoves))
  (let [first-step (-> pmove :steps last)
        starting-board (:board first-step)
        last-step (-> pmove :steps first)
        piece (:piece last-step)]
    (if-let [captured (seq (core/pieces-matching starting-board
                                                 {:pos (:pos piece)
                                                  :player (core/opponent
                                                           (piece :player))}))]
      (-> pmove
          (core/pmove-capture-piece captured)
          core/pmove-finish)

      pmove)))

;;
;; Ideas of combinators:
;;
;; (pmove-expand-alt
;;        (pmove-expand-if-first-step dirs)
;;        (pmove-expand-on-same-direction))
;;
;; (remove (some-fn
;;;          pmove-on-same-player-piece?
;;           pmove-outside-board?
;;           in-check?)) ; <- for King

;;
;; Pattern:
;;   1. expand: pmove -> [pmove]
;;   2. filter: [pmove] -> [pmove] (remove some pmoves)
;;   3. enrich: [pmove] -> [pmove] (update it with additional info/flags/captured pieces)
;;   4. finish: [pmove] -> [pmove] (might expand)
;;
(defmethod core/continue-pmove-for-piece :r [pmove]
  (->> #_ (pmove-expand-if-first-step dirs) ;; expander
       #_ (pmove-expand-on-same-direction)

       #_ (if (pmove-first-step? pmove)
            (pmove-move-piece-dirs pmove dirs)
            (pmove-move-piece-same-dir pmove))
       (pmove-move-piece-same-dir rook-dirs pmove)
       (remove core/pmove-on-same-player-piece?)
       ;; TODO finish at other player piece (capture)
       (map pmoves-finish-capturing-opponent-piece)
       (pmoves-finish-at-one-or-more-steps)) ;; vs pmoves-finish-at-one-step ?
  )



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
