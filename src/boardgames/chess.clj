;; # Chess
^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(ns boardgames.chess
  (:require [boardgames.utils :refer [upper-case-keyword TAP>]]
            [nextjournal.clerk :as clerk]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core :refer [↑ ↓ ← → ↖ ↗ pos+
                                              board-find-piece
                                              pmove-finish
                                              pmove-finished?
                                              piece-never-moved?
                                              pmove-on-same-player-piece?
                                              pmove-on-other-player-piece?
                                              pmove-outside-board?
                                              pmove-piece-1st-move?
                                              pmove-max-steps?]]))

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

(defn initiate-shuffle-chess-board []
  (let [first-row (shuffle (first initial-chess-symbolic-board))
        last-row (->> first-row (mapv upper-case-keyword))]
    (-> initial-chess-symbolic-board
        (assoc 0 first-row)
        (assoc 7 last-row)
        core/symbolic->board)))

(defn expand-pmove-fixed-dir
  "Expand pmove but only on its last step's direction. If no previous step,
use all possible dirs. Useful for pieces that move in a straight line"
  [possible-dirs pmove]
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

(defn expand-pmove-dirs [possible-dirs pmove]
  (let [player (-> pmove :steps first :piece :player)]
    (->> possible-dirs
         (map (partial core/adjust-dir-to-player player))
         (map (partial core/pmove-move-piece pmove)))))

(defn pmoves-finish-and-continue [pmoves]
  (let [{finished true unfinished false} (group-by pmove-finished? pmoves)]
    (concat unfinished finished (map pmove-finish unfinished))))

(defn pmoves-finish-at-one-step [pmoves]
  (concat pmoves (map pmove-finish pmoves)))

(defn pmove-finish-capturing [pmove]
  (let [first-step (-> pmove :steps last)
        starting-board (:board first-step)
        last-step (-> pmove :steps first)
        piece (:piece last-step)]

    ;; This supports capturing multiple pieces. Not needed for chess
    (if-let [captured (seq (core/board-find-pieces starting-board
                                                 {:pos (:pos piece)
                                                  :player (core/opponent-player
                                                           (piece :player))}))]
      (-> pmove
          (core/pmove-capture-piece captured)
          pmove-finish)

      pmove)))

(defn pmoves-discard [pred coll]
  (remove pred coll))

;; ---

;; ## Piece pmove expansion functions
;;
;; General pattern:
;;   1. generate moves (expand): pmove -> [pmove]
;;   2. discard moves (shrink, filter): [pmove] -> [pmove] (remove some pmoves)
;;   3. enrich: [pmove] -> [pmove] (update it with additional info/flags/captured pieces+)
;;   4. finish: [pmove] -> [pmove] (might expand)

(defn expand-pmove-for-rook [pmove]
  (->> pmove
       (expand-pmove-dirs [↑ ↓ ← →])
       (pmoves-discard #(or (pmove-on-same-player-piece? %)
                            (pmove-changed-direction? %)))
#_       (pmoves-discard (some-fn pmove-changed-direction?
                                pmove-on-same-player-piece?))
       (map pmove-finish-capturing)
       (pmoves-finish-and-continue)))

(defn expand-pmove-for-pawn [pmove]
  (concat
   (->> pmove ;; Move fwd rule
        (expand-pmove-dirs [↑])
        (pmoves-discard (some-fn pmove-changed-direction?
                                 pmove-on-same-player-piece?
                                 (partial pmove-max-steps?
                                          (if (pmove-piece-1st-move? pmove) 2 1))))
        (pmoves-finish-and-continue))

   (->> pmove ;; Capture rule
        (expand-pmove-dirs [↖ ↗])
        (pmoves-discard #(or (not (pmove-on-other-player-piece? %))
                             (pmove-max-steps? 1 %)))
        (map pmove-finish-capturing))

   #_(->> pmove ;; En-passant rule: TODO
          (expand-pmove-dirs [dir-up-left dir-up-right])
          (pmoves-discard (some-fn (complement pmove-on-other-player-piece?)
                                   (pmove-over-max-steps?? 1)))
          (map pmoves-finish-capturing-opponent-piece))))



(defn captures-king? [pmove]
  (when-let [captures (-> pmove :steps first :captures seq)]
    (contains?  (set (map :type captures)) :k)))

(declare chess-expansion-rules)

(defn pmove-king-in-check? [pmove]
  (let [last-step (-> pmove :steps first)
        board (:board last-step)
        player (core/opponent-player (-> last-step :piece :player))
        reply-moves (core/candidate-pmoves* board
                                            player
                                            (dissoc chess-expansion-rules :k))]
    (some captures-king? reply-moves)))


;; ## Castling

(defn- rook-for-castling? [piece player]
  (and piece
       (= (:type piece) :r)
       (piece-never-moved? piece)
       (= player (:player piece))))


(defn- pmoves-finish-castling [pmove]
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

        pmove))))

(defn expand-pmove-for-king [pmove]

  (concat
   (->> pmove ;; Regular move
        (expand-pmove-dirs (mapcat core/rotations [↑ ↗]))
        (pmoves-discard (some-fn pmove-on-same-player-piece?
                                 (partial pmove-max-steps? 1)))
        (map pmove-finish-capturing)
        (map pmove-finish))

   (->> pmove ;; Castling
        (expand-pmove-dirs [← →])
        (pmoves-discard (some-fn pmove-on-same-player-piece?
                                 pmove-changed-direction?
                                 (complement pmove-piece-1st-move?)
                                 (partial pmove-max-steps? 2)
                                 pmove-king-in-check?))
        (map pmoves-finish-castling))))


(defn expand-pmove-for-bishop [pmove]
  (->> pmove
       (expand-pmove-dirs (core/rotations ↗))
       (pmoves-discard (some-fn pmove-changed-direction?
                                pmove-on-same-player-piece?))
       (map pmove-finish-capturing)
       (pmoves-finish-and-continue)))

(defn expand-pmove-for-knight [pmove]
  (->> pmove
       (expand-pmove-dirs (core/flip-and-rotations [1 2]))
       (pmoves-discard (some-fn (partial pmove-max-steps? 1)
                                pmove-on-same-player-piece?))
       (map pmove-finish-capturing)
       (pmoves-finish-and-continue)))

(defn expand-pmove-for-queen [pmove]
  (->> pmove
       (expand-pmove-dirs (mapcat core/rotations [↑ ↗]))
       (pmoves-discard (some-fn pmove-changed-direction?
                                pmove-on-same-player-piece?))
       (map pmove-finish-capturing)
       (pmoves-finish-and-continue)))



(def chess-expansion-rules
  {:k expand-pmove-for-king
   :r expand-pmove-for-rook
   :b expand-pmove-for-bishop
   :n expand-pmove-for-knight
   :q expand-pmove-for-queen
   :p expand-pmove-for-pawn })

(defn discard-king-checked-pmoves [pmoves]
  (->> pmoves (remove pmove-king-in-check?)))

(def chess-aggregate-rules
  [discard-king-checked-pmoves])


(core/defgame chess-game "Chess" initiate-chess-board chess-expansion-rules chess-aggregate-rules)

(core/defgame shuffle-chess-game "Chess" initiate-shuffle-chess-board chess-expansion-rules chess-aggregate-rules)

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
