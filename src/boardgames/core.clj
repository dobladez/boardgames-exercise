;; # Core domain:  Primitives to represent grid board games

^{:nextjournal.clerk/visibility {:code :hide}}
(ns boardgames.core
  (:require [nextjournal.clerk :as clerk]
            [clojure.string :refer [lower-case upper-case]]
            [boardgames.utils :as utils :refer [replace-in-set]]))

{::clerk/visibility {:code :show :result :hide}
 ::clerk/auto-expand-results? true
 ::clerk/budget 1000}

;;
;; ## Operations on a board Piece
;;
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

(defn move-piece [piece dir-offsets]
  (-> piece
      (assoc :pos (mapv + (:pos piece) dir-offsets))
      (flag-piece :moved)))

;; We use tuples of [column row] offsets to represent "directions". Here we give
;; some common ones a name. Note: they are allowed to be more than 1
(def dir-up [0 +1])
(def dir-up-left [-1 +1])
(def dir-up-right [+1 +1])
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

;;
;; ## Operations on a Board
;;
(defn new-board [pieces row-n col-n]
  (with-meta {:pieces (set pieces)
              :row-n row-n
              :col-n col-n}
    {:tag :boardgames}))

(defn empty-board [n-rows n-columns]
  (new-board #{} n-rows n-columns))

(defn board->symbolic [board]
  (let [rows  (:row-n board)
        cols (:col-n board)]
    (when (and (number? rows) (number? cols))
      (let [empty-board (vec (repeat rows (vec (repeat cols '-))))]
        (->> (:pieces board)
             (reduce (fn [board piece]
                       (assoc-in board (reverse (:pos piece))
                                 (if (= 1 (:player piece))
                                   (symbol  (:type piece))
                                   (-> piece :type name upper-case symbol))))
                     empty-board)
             reverse
             vec)))))

(defn upper-case? [kw]
 (= (name kw) (upper-case (name kw))))

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
    (new-board pieces (count symbolic-board) (count (first symbolic-board)))))

(defn pieces-at-pos [board pos]
  (filter #(= pos (:pos %)) (:pieces board)))

(defn pieces-matching [board partial-piece]
  (filter #(= (select-keys % (keys partial-piece)) partial-piece)
          (:pieces board)))

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
;; ## Moves
;;
;; A move is comprised a list of (one or more) steps.
;;
;; While generating possible moves, a move is grown one step at a time. Here we
;; refer to them as `pmoves`  (for "partial move").  A `pmove` that is not
;; `:finished?` may be expanded with more steps until it becomes so.
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

#_ (defmulti expand-pmove-step
  "Given a pmove, return the next posible pmoves"
  (fn [pmove]
    (:type (:piece (first (:steps pmove))))))

#_(defmethod expand-pmove-step :default [_] '())


(defn- pmove-piece-type [pmove]
  (:type (:piece (first (:steps pmove)))))

(defn expand-pmove
  "Give a starting base pmove, return the list of all valid (finished) pmoves"
  [expansion-rules base-pmove]

  (if-let [expand-pmove-step (get expansion-rules (pmove-piece-type base-pmove))]

    (->> (expand-pmove-step base-pmove)
         (mapcat (fn [p-move]
                   (cond (pmove-outside-board? p-move) '()
                         (:finished? p-move) (list p-move)
                         :else (expand-pmove expansion-rules p-move))) ;; recurse
                 ))

    (list)))


(defn candidate-pmoves* [board player-turn expansion-rules]
  (->> board
       :pieces
       (filter #(= player-turn (:player %)))
       (mapcat #(expand-pmove expansion-rules (new-pmove board %)))))


(defn candidate-pmoves "Generate (using game expansion rules) list of finished pmoves candidates"
  [game-state]

  (let [{:keys [board game-def]} game-state]
    (candidate-pmoves* board (:turn game-state) (:expansion-rules game-def))
#_    (->> board
         :pieces
         (filter #(= (:turn game-state)
                     (:player %)))
         (mapcat #(expand-pmove (:expansion-rules game-def) (new-pmove board %))))))

(defn- apply-aggregate-rules [game-state pmoves]
  ;; TODO: change impl to avoid need for game-state on agg rules
  #_((apply comp (:aggregate-rules (:game-def game-state))) game-state pmoves)
  ((apply comp (:aggregate-rules (:game-def game-state))) pmoves)


  )

(defn possible-pmoves "Returns list of all finished (and valid) pmoves"
  [game-state]

  (->> (candidate-pmoves game-state)
       (apply-aggregate-rules game-state)))



;;;;
(defn game-apply-pmove [game pmove]
  ;; TODO
  game
  )
(defn board-apply-pmove [board pmove]
  ;; TODO
  )

(defn- pmove-add-step [pmove update-fn]
  (update-in pmove [:steps] (fn [steps] (conj steps (update-fn (first steps))))))

(defn pmove-move-piece [pmove dir]
  (pmove-add-step pmove
                  (fn move-piece-step [step]
                    (let [from-piece (:piece step)
                          to-piece (move-piece from-piece dir)]
                      (-> step
                          (update-in [:board :pieces] replace-in-set from-piece to-piece)
                          (assoc-in [:piece] to-piece))))))

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

#_ (defn pmove-capture-piece_NEW [pmove captured-pieces]
  (let [last-steps-idx (-> pmove :steps count dec)]

    (update-in pmove [:steps last-steps-idx]
               (fn [last-step]
                 (-> last-step
                     (assoc :captures captured-pieces)
                     (update-in [:board :pieces]
                                #(reduce disj % captured-pieces)))))))

(defn pmove-last-step-direction [pmove]
  (when (> (count (:steps pmove)) 1)
    (let [[last-step prior-step] (:steps pmove)]
      (mapv - (-> last-step :piece :pos) (-> prior-step :piece :pos)))))

(defn pmove-on-other-player-piece? [pmove]
  (let [first-step (-> pmove :steps last)
        starting-board (:board first-step)
        last-step (-> pmove :steps first)
        piece (:piece last-step)]
    (not (empty? (pieces-matching starting-board {:player (opponent-player (:player piece))
                                                  :pos (:pos piece)})))))

(defn pmove-on-same-player-piece? [pmove]
  (let [first-step (-> pmove :steps last)
        starting-board (:board first-step)
        last-step (-> pmove :steps first)
        piece (:piece last-step)]
    (not (empty? (pieces-matching starting-board (select-keys piece [:player :pos]))))))

(defn pmove-over-max-steps? [max pmove]
  (> (dec (count (:steps pmove))) max))



(defn pmove-finish [pmove]
  (assoc pmove :finished? true))

(defn pmove-finished? [pmove]
  (:finished? pmove))

(defn pmove-piece-1st-move? [pmove]
  (not (-> pmove :steps last :piece (piece-flag? :moved))))


(defn switch-turn [game-state]
  (update game-state :turn opponent-player))

;;
;; ## Games
;;
(defn make-game [name player-count initiate-board-fn expansion-rules aggregate-rules]
  ;; TODO validations
  {:name name
   :player-count player-count
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

;; TODO:
(defn game-loop [])
