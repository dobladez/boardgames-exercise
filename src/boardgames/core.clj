;; # Core domain


;; Modeling grid-board games

^{:nextjournal.clerk/visibility {:code :hide}}
(ns boardgames.core
  (:require [nextjournal.clerk :as clerk]
            [clojure.string :refer [lower-case upper-case]]
            [boardgames.utils :as utils :refer [replace-in-set upper-case?]]))

{::clerk/visibility {:code :show :result :hide}
 ::clerk/auto-expand-results? true
 ::clerk/budget 1000}

;; ## Pieces
(defn new-piece
  ([type player pos] (new-piece type player pos nil))
  ([type player pos flags] {:type type
                            :player player
                            :pos pos
                            :flags (or flags #{})}))

(defn flag-piece [piece flag]
  (update piece :flags conj flag))

(defn piece-flag? [piece flag]
  (contains? (:flags piece) flag))

(defn piece-never-moved? [piece]
  (not (piece-flag? piece :moved)))

(defn move-piece-to [piece new-pos]
  (-> piece
      (assoc :pos new-pos)
      (flag-piece :moved)))

#_(defn move-piece [piece offset]
  (move-piece-to piece (mapv + (:pos piece) offset)))


;; ## Offsets
;;
;; We use tuples of [column row] offsets to represent shifts in
;; position of a piece. Here we use unicode arrow names for all 1-step offsets
(def ↑ [0 +1])
(def ↓ [0 -1])
(def → [+1 0])
(def ← [-1 0])
(def ↗ [+1 +1])
(def ↖ [-1 +1])
(def ↘ [+1 -1])
(def ↙ [-1 -1])

(defn rotate-right [[x y]]
  [y (- x)])

(defn rotate-left [[x y]]
  [(- y) x])

(defn flip-h [[x y]]
  [(- x) y])

(defn flip-v [[x y]]
  [x (- y)])

(defn rotations [offset]
  (vec (take 4 (iterate rotate-right offset))))

(defn flip-and-rotations [offset]
  (vec (concat (rotations offset)
               (rotations (flip-h offset)))))

(defn pos+ [pos offset]
    (mapv + pos offset))
;;
;; ## Boards
;;
(defn new-board [row-n col-n pieces]
  (with-meta {:pieces (set pieces)
              :row-n row-n
              :col-n col-n}
    {:tag :boardgames}))

(defn empty-board [n-rows n-columns]
  (new-board n-rows n-columns #{}))

(defn board->symbolic [board]
  (let [rows  (:row-n board)
        cols (:col-n board)]
    (when (and (pos? rows) (pos? cols))
      (let [empty-vec (vec (repeat rows (vec (repeat cols '-))))]
        (->> (:pieces board)
             (reduce (fn [board piece]
                       (try
                         (assoc-in board (reverse (:pos piece))
                                   (if (= 1 (:player piece))
                                     (symbol  (:type piece))
                                     (-> piece :type name upper-case symbol)))
                         (catch Exception *ignore*
                           ;; piece off the board boundaries?
                           ;; TODO: avoid that state altogether
                           board)))
                     empty-vec)
             reverse
             vec)))))




(defn board-find-pieces [board partial-piece]
  (filter #(= (select-keys % (keys partial-piece)) partial-piece)
          (:pieces board)))

(defn board-find-piece [board partial-piece]
  (first (board-find-pieces board partial-piece)))

(defn symbolic->board [symbolic-board]
  (let [pieces (->>
                symbolic-board
                reverse
                (map-indexed
                 (fn [row-idx row]
                   (map-indexed
                    (fn [col-idx square]
                      (when-not (= square '-)
                        (new-piece (-> square name lower-case keyword)
                                   (if (upper-case? square)
                                     0 1)
                                   [col-idx row-idx])))
                    row)))
                flatten
                (remove nil?))]
    (new-board (count symbolic-board) (count (first symbolic-board)) pieces)))

(defn pieces-at-pos [board pos]
  (filter #(= pos (:pos %)) (:pieces board)))


(defn pmove-player [pmove]
  (-> pmove :steps first :piece :player))

(defn opponent-player [player]
  (if (= 0 player) 1 0))

(defn adjust-dir-to-player [player dir]
  (if (= 0 player)
    dir
    (let [[x y] dir]
      [x (- y)])))


;;
;; ## Moves and Partial moves (pmoves)
;;
;; A (partial) move is comprised a list of (one or more) steps.
;;
;; While generating possible moves, a move is grown one step at a time. Here we
;; refer to them as `pmoves`  (for "partial move").  A `pmove` that is not
;; `:finished?` may be expanded with more steps until it is discarded or become `:finished?`.
;;
(defn new-pmove [board piece]
  {:steps (list {:board board :piece piece :flags #{}})
   :finished? false
   :flags #{}})

(defn pmove-outside-board? [pmove]
  (let [last-step (first (:steps pmove))
        {:keys [board piece]} last-step
        pos (:pos piece)]
    (or (some neg? pos)
        (>= (first pos) (:col-n board))
        (>= (second pos) (:row-n board)))))

(defn- pmove-piece-type [pmove]
  (:type (:piece (first (:steps pmove)))))

(defn expand-pmove
  "Give a starting base pmove and a set of rules, return the list of all
  valid (finished) pmoves"
  [expansion-rules base-pmove]

  (if-let [expand-pmove-step (get expansion-rules (pmove-piece-type base-pmove))]

    (->> (expand-pmove-step base-pmove)
         (mapcat (fn [p-move]
                   (cond (pmove-outside-board? p-move) '()
                         (:finished? p-move) (list p-move)
                         :else (expand-pmove expansion-rules p-move)))))
    (list)))


(defn candidate-pmoves*
  "Generate all finished candidate pmoves for the given position, player and rules"
  [board player-turn expansion-rules]
  (->> board
       :pieces
       (filter #(= player-turn (:player %)))
       (mapcat #(expand-pmove expansion-rules (new-pmove board %)))))


(defn candidate-pmoves
  "Generate all finished candidate pmoves for the given game"
  [game-state]

  (let [{:keys [board game-def]} game-state]
    (candidate-pmoves* board (:turn game-state) (:expansion-rules game-def))))

(defn- apply-aggregate-rules [game-state pmoves]
  ((apply comp (:aggregate-rules (:game-def game-state))) pmoves))

(defn possible-pmoves "Returns list of all finished (and valid) pmoves"
  [game-state]

  (->> (candidate-pmoves game-state)
       (apply-aggregate-rules game-state)))


;; ### Modifying pmove

(defn- pmove-add-step [pmove update-fn]
  (update-in pmove [:steps] conj (update-fn (-> pmove :steps first))))

(defn pmove-update-last-step [pmove f]
  (update-in pmove [:steps]
             (fn [steps]
               (let [last-step (-> steps first)]
                 (-> (drop 1 steps)
                     (conj
                      (f last-step)))))))

(defn step-move-piece-to [step from-piece pos]
  (let [to-piece (move-piece-to from-piece pos)]
    (-> step
        (update-in [:board :pieces] replace-in-set from-piece to-piece)
        (assoc-in [:piece] to-piece))))

(defn step-move-piece [step from-piece offset]
  (step-move-piece-to step from-piece (pos+ (:pos from-piece) offset)))

(defn pmove-move-piece [pmove dir]
  (pmove-add-step pmove
                  (fn move-piece-step [step]
                    (let [from-piece (:piece step)]
                      (step-move-piece step from-piece dir)))))


(defn pmove-capture-piece [pmove captured-pieces]
  (update-in pmove [:steps]
             (fn [steps]
               (let [first-step (-> steps first)]
                 (-> (drop 1 steps)
                     (conj
                      (-> first-step
                          (assoc :captures captured-pieces)
                          (update-in [:board :pieces]
                                     #(reduce disj % captured-pieces)))))))))

(defn pmove-finish [pmove]
  (assoc pmove :finished? true))


;; ### pmoves: Reading and Predicates
(defn pmove-last-step-direction [pmove]
  (when (> (count (:steps pmove)) 1)
    (let [[last-step prior-step] (:steps pmove)]
      (mapv - (-> last-step :piece :pos) (-> prior-step :piece :pos)))))

(defn pmove-on-other-player-piece? [pmove]
  (let [first-step (-> pmove :steps last)
        starting-board (:board first-step)
        last-step (-> pmove :steps first)
        piece (:piece last-step)]
    (not (empty? (board-find-pieces starting-board {:player (opponent-player (:player piece))
                                                  :pos (:pos piece)})))))

(defn pmove-on-same-player-piece? [pmove]
  (let [first-step (-> pmove :steps last)
        starting-board (:board first-step)
        last-step (-> pmove :steps first)
        piece (:piece last-step)]
    (not (empty? (board-find-pieces starting-board
                                    (select-keys piece [:player :pos]))))))

(defn pmove-max-steps? [max pmove]
  (> (dec (count (:steps pmove))) max))

(defn pmove-finished? [pmove]
  (:finished? pmove))

(defn pmove-piece-1st-move? [pmove]
  (not (-> pmove :steps last :piece (piece-flag? :moved))))

(defn pmove-changed-direction? [pmove]
  (when (> (count (:steps pmove)) 1)
    (< 1 (count
          (into #{}
                (for [[step-a step-b] (partition 2 1 (:steps pmove))]
                  (mapv - (-> step-a :piece :pos) (-> step-b :piece :pos))))))))


;; ### Expanding pmoves

(defn expand-pmove-fixed-dir
  "Expand pmove but only on its last step's direction. If no previous step,
use all possible dirs. Useful for pieces that move in a straight line"
  [possible-dirs pmove]
  (let [previous-dir (pmove-last-step-direction pmove)
        next-dirs (if previous-dir (list previous-dir) possible-dirs)]
    (->> next-dirs
         (map (partial pmove-move-piece pmove)))))

(defn expand-pmove-dirs [possible-dirs pmove]
  (let [player (-> pmove :steps first :piece :player)]
    (->> possible-dirs
         (map (partial adjust-dir-to-player player))
         (map (partial pmove-move-piece pmove)))))

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
    (if-let [captured (seq (board-find-pieces starting-board
                                              {:pos (:pos piece)
                                               :player (opponent-player
                                                        (piece :player))}))]
      (-> pmove
          (pmove-capture-piece captured)
          pmove-finish)

      pmove)))

(defn pmoves-discard [pred coll]
  (remove pred coll))


;;
;; ## Games
;;
(defn make-game [name initiate-board-fn expansion-rules aggregate-rules]
  {:name name
   :initiate-board-fn initiate-board-fn
   :expansion-rules expansion-rules
   :aggregate-rules aggregate-rules})

(defmacro defgame
  "Defines a new var describing a board game"
  [varname & body]
  `(def ~varname (make-game ~@body)))

(defn start-game
  "Starts a new game for the given game definition and (optional) initial board"
  ([gamedef]
   (start-game gamedef ((:initiate-board-fn gamedef))))
  ([gamedef initial-board]
   {:game-def gamedef
    :board initial-board
    :turn 0}))

(defn switch-turn [game-state]
  (update game-state :turn opponent-player))


;; TODO:

(defn game-apply-pmove [game pmove]
  ;; TODO
  game
  )
(defn game-loop []
  )
