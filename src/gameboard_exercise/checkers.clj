
;; # Checkers/Draughts
^{:nextjournal.clerk/visibility {:code :hide}}
(ns gameboard-exercise.checkers
  (:require [nextjournal.clerk :as clerk]
            [gameboard-exercise.clerk-viewers :as viewers]
            [gameboard-exercise.core :as core]
            ))

^{:nextjournal.clerk/visibility {:code :hide :result :hide }}
(clerk/add-viewers! [viewers/board-viewer])

;; An empty "checkers" board (actually, International Draughts, 10x10)
(core/empty-board 10 10)

;; Intitial board
^{:nextjournal.clerk/visibility {:code :show :result :hide }}
(defn initial-checkers-board []
  (core/new-board
   [[\p \- \p \- \p \- \p \- \p \-]
    [\- \p \- \p \- \p \- \p \- \p]
    [\p \- \p \- \p \- \p \- \p \-]
    [\- \p \- \p \- \p \- \p \- \p]
    [\- \- \- \- \- \- \- \- \- \-]
    [\- \- \- \- \- \- \- \- \- \-]
    [\- \P \- \P \- \P \- \P \- \P]
    [\P \- \P \- \P \- \P \- \P \-]
    [\- \P \- \P \- \P \- \P \- \P]
    [\P \- \P \- \P \- \P \- \P \-]]
   ))

(def board1 (initial-checkers-board))
