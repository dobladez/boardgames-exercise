
;; # Tic-Tac-Toe
(ns boardgames.ttt
  (:require [nextjournal.clerk :as clerk]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]))

{::clerk/visibility {:code :show :result :hide}}

(def initial-symbolic-board '[[- - -]
                              [- - -]
                              [- - -]])

(defn initial-board []
  (core/symbolic->board initial-symbolic-board))

;; Example initial board:
^{::clerk/visibility {:code :show :result :show}
  ::clerk/viewer viewers/board-viewer}
(def -initial-board (initial-board))

;; ## TBD: not yet implemented

;; One interesting thing aspect of Tic Tac Toe, different from Chess or checkers
;; is that a move may introduce a new piece to the board.
