;; # Chess
^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(ns boardgames.chess
  (:require [boardgames.utils :refer [upper-case-keyword ttap>]]
            [nextjournal.clerk :as clerk]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core
             :as core
             :refer [↑ ↓ ← → ↖ ↗
                     pmove-on-same-player-piece?
                     pmove-on-other-player-piece?
                     pmove-outside-board?
                     pmove-piece-1st-move?
                     pmove-over-max-steps?]]))

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

(def rook-dirs (core/rotations [0 1]))
(def bishop-dirs (core/rotations [1 1]))
(def all-dirs (concat rook-dirs bishop-dirs))
(def knight-dirs (core/flip-and-rotations [1 2]))

(defn expand-pmove-fixed-dir
  "Expand pmove but only on its last step's direction. If no previous step, use all possible dirs.
   Useful for pieces that move in a straight line, like rook, bishop and queen in chess"
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
                                                  :player (core/opponent-player
                                                           (piece :player))}))]
      (-> pmove
          (core/pmove-capture-piece captured)
          core/pmove-finish)

      pmove)))


;;
;; Ideas for combinators:
;;
(comment
  (pmove-expand-alt
   (pmove-expand-if-first-step dirs)
   (pmove-expand-on-same-direction))

  (remove (some-fn
           pmove-on-same-player-piece?
           pmove-outside-board?
           in-check?)))

;; Pattern:
;;   1. generate moves (expand): pmove -> [pmove]
;;   2. discard moves (shrink, filter): [pmove] -> [pmove] (remove some pmoves)
;;   3. enrich: [pmove] -> [pmove] (update it with additional info/flags/captured pieces+)
;;   4. finish: [pmove] -> [pmove] (might expand)
;;
;; So, maybe we have move-generators,  move-filters, move-enrichers, move-finishers?


;; ### Rook
(defn pmoves-discard [pred coll]
  (remove pred coll))

;; ---

(def chess-expansion-rules
  {:k (fn expand-pmove-chess-king [pmove]
        (->> pmove
             (expand-pmove-dirs (mapcat core/rotations [↑ ↗]))
             (pmoves-discard (some-fn pmove-on-same-player-piece?
                                      (partial pmove-over-max-steps? 1)))
             (map pmoves-finish-capturing-opponent-piece)
             (pmoves-finish-and-continue)))

   :r (fn expand-pmove-chess-rook [pmove]
        (->> pmove
             (expand-pmove-dirs [↑ ↓ ← →])
             #_(expand-pmove-dirs (core/rotations ↑))
             (pmoves-discard (some-fn pmove-changed-direction?
                                      pmove-on-same-player-piece?))
             (map pmoves-finish-capturing-opponent-piece)
             (pmoves-finish-and-continue)))

   :b (fn expand-pmove-chess-bishop [pmove]
        (->> pmove
             (expand-pmove-dirs (core/rotations ↗))
             (pmoves-discard (some-fn pmove-changed-direction?
                                      pmove-on-same-player-piece?))
             (map pmoves-finish-capturing-opponent-piece)
             (pmoves-finish-and-continue)))

   :n (fn expand-pmove-chess-knight [pmove]
        (->> pmove
             (expand-pmove-dirs (core/flip-and-rotations [1 2]))
             (pmoves-discard (some-fn (partial pmove-over-max-steps? 1)
                                      pmove-on-same-player-piece?))
             (map pmoves-finish-capturing-opponent-piece)
             (pmoves-finish-and-continue)))

   :q (fn expand-pmove-chess-queen [pmove]
        (->> pmove
             (expand-pmove-dirs (mapcat core/rotations [↑ ↗]))
             (pmoves-discard (some-fn pmove-changed-direction?
                                      pmove-on-same-player-piece?))
             (map pmoves-finish-capturing-opponent-piece)
             (pmoves-finish-and-continue)))

   :p (fn expand-pmove-chess-pawn [pmove]
        (concat
         (->> pmove ;; Move fwd rule
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
              (map pmoves-finish-capturing-opponent-piece))

         #_(->> pmove ;; En-passant rule: TODO
                (expand-pmove-dirs [dir-up-left dir-up-right])
                (pmoves-discard (some-fn (complement pmove-on-other-player-piece?)
                                         (pmove-over-max-steps?? 1)))
                (map pmoves-finish-capturing-opponent-piece))))})

(defn captures-king? [pmove]
  (when-let [captures (-> pmove :steps first :captures seq)]
    (contains?  (set (map :type captures)) :k)))

(def chess-aggregate-rules
  [(fn discard-king-checked-pmoves [pmoves]
     (->> pmoves
          (remove #(let [board (-> % :steps first :board)
                         player-turn (core/opponent-player (-> % :steps first :piece :player))
                         follow-up-moves (core/candidate-pmoves* board player-turn chess-expansion-rules)]
                     (some captures-king? follow-up-moves)))))

   (fn xyz [pmoves]
     pmoves)])


(core/defgame chess-game "Chess" 2 initiate-chess-board chess-expansion-rules chess-aggregate-rules)

(core/defgame fisher-chess-game "Chess" 2 initiate-fisher-chess-board chess-expansion-rules chess-aggregate-rules)

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
