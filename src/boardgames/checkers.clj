
;; # Checkers/Draughts
(ns boardgames.checkers
  (:require [nextjournal.clerk :as clerk]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]))

{::clerk/visibility {:code :show :result :hide}}

(def initial-checkers-symbolic-board '[[o - o - o - o - o -]
                                       [- o - o - o - o - o]
                                       [o - o - o - o - o -]
                                       [- o - o - o - o - o]
                                       [- - - - - - - - - -]
                                       [- - - - - - - - - -]
                                       [- O - O - O - O - O]
                                       [O - O - O - O - O -]
                                       [- O - O - O - O - O]
                                       [O - O - O - O - O -]])

(defn initial-checkers-board []
  (core/symbolic->board initial-checkers-symbolic-board))

;; Example initial board:
^{::clerk/visibility {:code :show :result :show}
  ::clerk/viewer viewers/board-viewer}
(def -initial-checkers-board (initial-checkers-board))
