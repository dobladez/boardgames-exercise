;; # Example of "Stratified Design"
^{:nextjournal.clerk/visibility {:code :hide}}
(ns gameboard-exercise.main
  (:require [nextjournal.clerk :as clerk]
            [gameboard-exercise.clerk-viewers :as viewers]))

;;

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(clerk/add-viewers! [viewers/board-viewer])

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(defn new-board [board-pieces]
  ;; TODO: validate board-pieces dimensions?
  (with-meta
    board-pieces
    {:tag :gameboard } ))

(defn initial-chess-board []
  (new-board
   [[\r \n \b \q \k \b \n \r]
    [\p \p \p \p \p \p \p \p]
    [\- \- \- \- \- \- \- \-]
    [\- \- \- \- \- \- \- \-]
    [\- \- \- \- \- \- \- \-]
    [\- \- \- \- \- \- \- \-]
    [\P \P \P \P \P \P \P \P]
    [\R \N \B \Q \K \B \N \R]]
   ))

(defn initial-checkers-board []
  (new-board
   [[\p \- \p \- \p \- \p \- \p \-]
    [\- \p \- \p \- \p \- \p \- \p]
    [\p \- \p \- \p \- \p \- \p \-]
    [\- \- \- \- \- \- \- \- \- \-]
    [\- \- \- \- \- \- \- \- \- \-]
    [\- \- \- \- \- \- \- \- \- \-]
    [\- \- \- \- \- \- \- \- \- \-]
    [\P \- \P \- \P \- \P \- \P \-]
    [\- \P \- \P \- \P \- \P \- \P]
    [\P \- \P \- \P \- \P \- \P \-]]
   ))

;; Some concepts:

(defn empty-board
  [n-rows n-columns]
  (new-board (vec (repeat n-rows (vec (repeat n-columns \-))))))

(empty-board 8 8)
(empty-board 10 10)

(def aPiece {:type :r ;; possible keywords here defined by each game
             :player 0 ;;
             :position??? nil })

(def aGame {:name :chess
            :player-count 2
            :state [{:board (initial-chess-board)
                     :turn 0
                     :last-move [:type \?]
                     }]})

(def aMove {:steps [{:from [] :to [] :board []}]}

  )


;; Let's create a chess board
(def board1 (initial-chess-board))
(def board2 (initial-checkers-board))


;; Shuffle the pieces ^{::clerk/viewer board-viewer}
(->> board1 (apply concat) shuffle (partition 8) new-board)


(defn game-loop []
  )


#_#_
(with-meta
  board5
  {::clerk/viewer board-viewer})

^{::clerk/viewer board-viewer}
board5

#_^{::clerk/viewer board-viewer}
(map shuffle board1)

#_ (piece-to-icon (get-in board2 [7 7]))
