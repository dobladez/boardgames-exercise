;; # Core domain for simple board games
^{:nextjournal.clerk/visibility {:code :hide}}
(ns gameboard-exercise.core
  (:require [nextjournal.clerk :as clerk]
            [clojure.string :refer [lower-case upper-case]]
            [gameboard-exercise.utils :as utils :refer [replace-in-set]]))

(defn new-piece
  ([type player pos] (new-piece type player pos nil))
  ([type player pos flags] {:type type
                            :player player
                            :pos pos
                            :flags (or flags #{})}))

(defn new-board [pieces row-n col-n]
  (with-meta {:pieces (set pieces)
              :row-n row-n
              :col-n col-n }
    {:tag :gameboard } ))

(defn empty-board [n-rows n-columns]
  (new-board #{} n-rows n-columns))


(defn make-game [name player-count initiate-board-fn expansion-rules aggregate-rules]
  ;; TODO validations?
  {:name name
   :player-count player-count
   :initiate-board-fn initiate-board-fn
   :expansion-rules expansion-rules
   :aggregate-rules aggregate-rules })

(defmacro defgame
  "Defines a new var describing a board game"
  [varname & body]
  `(def ~varname (make-game ~@body)))


(defn piece-flag [piece flag]
  (update piece :flags conj flag))

(defn piece-flag? [piece flag]
  (contains? (:flags piece) flag))

(defn start-game [gamedef]
  {:game gamedef
   :board ((:initiate-board-fn gamedef))
   :turn 0 } )

(defn pmove-add-step [pmove update-fn]
  (update-in pmove [:steps] (fn [steps] (conj steps (update-fn (first steps))))))

(defn pmove-append-piece-move [pmove dir]
  (pmove-add-step pmove
                  (fn move-piece-step [step]
                    (let [piece (:piece step)
                          new-piece (-> piece
                                        (assoc :pos (mapv + (:pos piece) dir))
                                        (piece-flag :moved))]
                      (-> step
                          (update-in [:board :pieces] replace-in-set piece new-piece)
                          (assoc-in [:piece] new-piece))))))
(defn pmove-append-piece-remove [pmove]
  )
(defn pmove-append-piece-add [pmove]
  )

(defn new-pmove [board piece]
  {:steps (list {:board board :piece piece :flags #{} })
   :finished? false
   :flags #{} })

(defn pmove-finish [pmove]
  (assoc pmove :finished? true))

;; "Given a pmove, return the next posible pmoves"

(defmulti continue-pmove-for-piece (fn [pmove]
                                     (:type (:piece (first (:steps pmove))))))

(defmethod continue-pmove-for-piece :default [_] '())

(defn- pmove-outside-board? [pmove]
  (let [last-step (first (:steps pmove))
        {:keys [board piece]} last-step
        pos (:pos piece)]
    (or (some neg? pos)
        (>= (first pos) (:col-n board))
        (>= (second pos) (:row-n board)))))

(defn possible-pmoves-for-piece "Returns list of all validad (and finished) pmoves for a given piece"
  [base-pmove]
  (let [p-moves (continue-pmove-for-piece base-pmove)]
    (mapcat (fn [p-move]
              (cond (pmove-outside-board? p-move) '()
                    (:finished? p-move) (list p-move)
                    :else (possible-pmoves-for-piece p-move)))
            p-moves)))

(defn possible-pmoves "Returns list of all valid (and finished) pmoves"
  [game-state]
  (let [board (:board game-state)
        turn (:turn game-state)]
    (mapcat (fn [piece]
              (possible-pmoves-for-piece (new-pmove board piece)))
            (filter #(= turn (:player %)) (:pieces board)))))


(defn game-loop [rules-maybe]
  )


(def dir-up [0 1])
(def dir-down [0 -1])
(def dir-left [-1 0])
(def dir-right [1 0])
(def dir-up-left [-1 +1])
(def dir-up-right [+1 +1])
(def dir-down-left [-1 -1])
(def dir-down-right [+1 -1])



(defn board->symbolic [board]
  (let [rows  (:row-n board)
        cols (:col-n board)
        empty-board (vec (repeat rows (vec (repeat cols :-))))]
    (->> (:pieces board)
         (reduce (fn [board piece]
                   (assoc-in board (reverse (:pos piece))
                             (if (= 1 (:player piece))
                               (:type piece)
                               (-> piece :type name upper-case keyword))))
                 empty-board)
         reverse
         vec)))

(defn symbolic->board [symbolic-board]
  (let [pieces (remove nil?
                       (flatten
                        (map-indexed (fn [row-idx row]
                                       (map-indexed (fn [col-idx square]
                                                      (when-not (= square :-)
                                                        (new-piece (-> square name lower-case keyword)
                                                                   (if (= (name square) (upper-case (name square)))
                                                                     0 1)
                                                                   [col-idx row-idx])))
                                                    row))
                                     (reverse symbolic-board))))]
    (new-board pieces (count symbolic-board) (count (first symbolic-board)))))
#_
(comment

  (def aPiece {:type :r     ;; possible keywords here defined by each game
               :player 0    ;;
               :position??? nil })

  (def aGame {:name :chess
              :player-count 2
              :board {:pieces (initial-chess-board)
                      :turn 0
                      :last-move [:type \?]
                      }})
  (def a-step {:board {} :piece {} :flags #{} })
  (def a-pmove1 {:board {} :piece {} :steps []})

  (def a-pmove {:finished? false
                :flags #{}
                :steps '({:board {} :piece {} :flags #{} }
                         {:board {} :piece {} :flags #{}
                          ;; ...
                          })})


  (def a-pmove {:finished? false
                :flags #{}
                :steps '({:board {} :piece {} :flags #{} }
                         {:board {} :piece {} :flags #{}
                          ;; ...
                          })}))
