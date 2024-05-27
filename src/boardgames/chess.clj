
;; # Chess
^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(ns boardgames.chess
  (:require [boardgames.utils :refer [upper-case-keyword ttap>]]
            [nextjournal.clerk :as clerk]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core
             :as core
             :refer [dir-up dir-down dir-left dir-right dir-up-left
                     dir-up-right dir-down-left dir-down-right]]))

{::clerk/visibility {:code :show :result :hide}
 ::clerk/auto-expand-results? true
 ::clerk/budget 1000}

#_ (clerk/add-viewers! [viewers/board-viewer])

(def initial-chess-symbolic-board '[[r n b q k b n r]
                                    [p p p p p p p p]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [P P P P P P P P]
                                    [R N B Q K B N R]])

(defn initiate-chess-board []
  (core/symbolic->board initial-chess-symbolic-board))

(defn initiate-fisher-chess-board []
  (let [first-row (shuffle (first initial-chess-symbolic-board))
        last-row (->> first-row (mapv upper-case-keyword))]
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



(defn pmove-move-piece-same-dir [possible-dirs pmove]
  (let [previous-dir (core/pmove-last-step-direction pmove)
        next-dirs (if previous-dir (list previous-dir) possible-dirs)]
    (->> next-dirs
         (map (partial core/pmove-move-piece pmove)))))

(defn pmove-changed-direction? [pmove]
  (when (> (count (:steps pmove)) 1)
    (< 1 (count
          (into #{}
                (for [[step-a step-b] (partition 2 1 (:steps pmove))]
                  (mapv - (-> step-a :piece :pos) (-> step-b :piece :pos))))))))

(defn pmove-expand-piece-dirs [possible-dirs pmove]
  (->> possible-dirs
       (map (partial core/pmove-move-piece pmove))))

(defn pmoves-stepping-own-disallowed [pmoves]
  (remove core/pmove-on-same-player-piece? pmoves))


(defn pmoves-finish-and-continue [pmoves]
  (let [{finished true unfinished false} (group-by core/pmove-finished? pmoves)]
    (concat unfinished finished (map core/pmove-finish unfinished))))

(defn pmoves-finish-at-one-step [pmoves]
  (concat pmoves (map core/pmove-finish pmoves)))

(defn pmoves-finish-capturing-opponent-piece [pmove]
  (let [first-step (-> pmove :steps last)
        starting-board (:board first-step)
        last-step (-> pmove :steps first)
        piece (:piece last-step)]

    ;; This supports capturing multiple pieces. Not needed for chess
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

(comment

  (pmove-expand-alt
   (pmove-expand-if-first-step dirs)
   (pmove-expand-on-same-direction))

  (remove (some-fn
           pmove-on-same-player-piece?
           pmove-outside-board?
           in-check?)))

(comment
;;
;; Pattern:
;;   1. generate moves (expand): pmove -> [pmove]
;;   2. discard moves (shrink, filter): [pmove] -> [pmove] (remove some pmoves)
;;   3. enrich: [pmove] -> [pmove] (update it with additional info/flags/captured pieces+)
;;   4. finish: [pmove] -> [pmove] (might expand)
;;
;; So, maybe we have move-generators,  move-filters, move-enrichers, move-finishers?

  ;; Rook v2 ideas
  (-> pmove
      (compose-pmove-gens
       (gen-pmoves-simple-dirs rook-dirs))
      (filter-pmoves #_filter-pmoves-occupied-same-player-piece
                     filter-pmoves-outside-board
                     filter-pmoves-in-check ;; this could be generic for all chess moves))

;; Pawn v2 ideas
  (-> pmove
      (compose-pmove-gens
       #_(gen-pmoves-simple-dirs up-dir :empty :opponent) ;; dir from players' view
       (gen-pmoves-simple-dirs up-dir) ;; dir from players' view
       #_(gen-pmoves-capture-dirs dir-fwd))

      (filter-pmoves #_filter-pmoves-occupied-same-player-piece
       filter-pmoves-outside-board
                     filter-pmoves-in-check ;; this could be generic for all chess moves
                     )))))

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

;;


;; ### Rook
(defmethod core/continue-pmove-for-piece :r_OLD [pmove]
  (->>

   #_(pmove-expand-if-first-step dirs) ;; expander
   #_(pmove-expand-on-same-direction)

   #_(if (pmove-first-step? pmove)
       (pmove-move-piece-dirs pmove dirs)
       (pmove-move-piece-same-dir pmove))
   (pmove-move-piece-same-dir rook-dirs pmove)
   (remove core/pmove-on-same-player-piece?)
       ;; TODO finish at other player piece (capture)
   (map pmoves-finish-capturing-opponent-piece)
   (pmoves-finish-and-continue)) ;; vs pmoves-finish-at-one-step ?
  )

(defn pmoves-discard [pred coll]
  (remove pred coll))


(defmethod core/continue-pmove-for-piece :r [pmove]
  (->> pmove
       (pmove-expand-piece-dirs rook-dirs)
       (pmoves-discard (some-fn pmove-changed-direction?
                                core/pmove-on-same-player-piece?
                                #_ pmove-outside-board?))
       #_(pmoves-enrich (pmove-capture-piece))
       (map pmoves-finish-capturing-opponent-piece)
       #_(pmoves-finish-all)
       (pmoves-finish-and-continue) ))

#_(comment

    (-> pmove
        (pmove-expand (pmove-move-piece-dirs dirs))   ;; pmove -> [pmove]
        (pmoves-discard (pmove-on-same-player-piece?)) ;; [pmove] -> [pmove] (shrink)
        (pmoves-enrich (pmove-capture-piece)) ;; [pmove] -> [pmove] (enrich, that is: map)
        (pmoves-finish))   ;; [pmove] -> [pmove] (finish, that is: map and maybe concat)

    (-> pmove
        (pmove-expand-piece-dirs dirs)
        (pmoves-discard  (some-fn pmove-changed-direction?
                                  pmove-on-same-player-piece?
                                  pmove-outside-board?))
        (pmoves-enrich (pmove-capture-piece))
        (pmoves-finish-all))

    ;; Pawn, move fwd:
    (-> pmove
        (pmove-expand-piece-dirs [dir-up]) ;; or down for black
        (pmoves-discard (some-fn (pmove-max-step-count-n 2)
                                 (pmove-max-step-count-n-p 1 piece-first-move?)
                                 pmove-on-same-player-piece?
                                 pmove-on-other-player-piece?
                                 pmove-outside-board?))
        (pmoves-enrich (pmove-enrich-enpassant)
                       (pmove-enrich-promotion))
        (pmoves-finish-all))

    ;; Pawn, move capture:
    (-> pmove
        (pmove-expand-piece-dirs [dir-up-right dir-up-left]) ;; or down for black
        (pmoves-discard (some-fn (every-pred (complement pmove-on-other-player-piece?)
                                             pmove-on-enpassant-pawn?)
                                 pmove-outside-board?))
        (pmoves-enrich (pmove-capture-piece))
        (pmoves-finish-all)))



#_(core/defgame chess-game
    {:name "Chess"
     :player-count 2
     :initiate-board-fn initiate-chess-board
     :expansion-rules []
     :aggregate-rules [] })

(core/defgame chess-game "Chess" 2 initiate-chess-board chess-evolution-rules chess-aggregate-rules)

(core/defgame fisher-chess-game "Chess" 2 initiate-fisher-chess-board chess-evolution-rules chess-aggregate-rules)

;; Usage:
(comment
  (def aGame (core/start-game chess-game)))


;;
;; Convert to/from FEN notation
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

(comment
  (board->fen initial-chess-symbolic-board))
