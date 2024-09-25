(ns boardgames.rook-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [boardgames.utils :as u]
            [boardgames.test-utils :as t]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            [nextjournal.clerk :as clerk]))

;; Given a board possition, we can start request the list of possible moves and
;; assert that they contain the moves we expect
(deftest chess-rook-moves-old

  (let [board (core/symbolic->board '[[- - - - - - - -]
                                      [- - - - - - - -]
                                      [- - - - - - - -]
                                      [- - - - - - - -]
                                      [- - - - - - - -]
                                      [- - - - - - - -]
                                      [R - - - - - - -]
                                      [- - - - - - - -]])

        game-1 (core/start-game chess/chess-game board)]

    (let [moves (core/possible-pmoves game-1)
          final-boards (->> (map (comp core/board->symbolic :board first :steps) moves)
                            (into #{}))]

      (is (= 14 (count moves)))
      (is (contains? final-boards '[[- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [R - - - - - - -]]))

      (is (contains? final-boards '[[R - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]]))

      (is (contains? final-boards '[[- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - - - - - - -]
                                    [- - R - - - - -]
                                    [- - - - - - - -]])))))

(deftest chess-rook-moves

  #_(is (= 1 2))

  ;; Here we pass the base board and then the expected boards one by one
  ;; Note:  the expected boards must be sorted by [x y] position of the moving piece
  (t/expect-chess-moves* {:piece :R}

                  '[[k - -]
                    [p - R]
                    [- - K]]

                  '[[[k - -] ;; Capturing the pawn
                     [R - -]
                     [- - K]]

                    [[k - -]
                     [p R -]
                     [- - K]]

                    [[k - R]
                     [p - -]
                     [- - K]]])

  (t/expect-chess-moves* {:piece :R}

                  '[[- - k]
                    [R - p]
                    [K - -]]

                  '[[[R - k]
                     [- - p]
                     [K - -]]

                    [[- - k]
                     [- R p]
                     [K - -]]

                    [[- - k]
                     [- - R] ;; <- Capturing the pawn
                     [K - -]]])

  ;; Here the we express the expected boards "horizontally":
  (t/expect-chess-moves {:piece :R}
                    '[[p R -]
                      [- - -]
                      [- K -]]

                    '[[R - -] [p - -] [p - R]
                      [- - -] [- R -] [- - -]
                      [- K -] [- K -] [- K -]])

  (t/expect-chess-moves {:piece :R}
                    '[[- - k]
                      [R - p]
                      [K - -]]

                    '[[R - k] [- - k] [- - k]
                      [- - p] [- R p] [- - R]
                      [K - -] [K - -] [K - -]])

  (t/expect-chess-moves {:piece :R
                     :extra-checks [{:captures '(:p)} {} {}]}

                    '[[p R -]
                      [- - -]
                      [- K -]]

                    '[[R - -] [p - -] [p - R]
                      [- - -] [- R -] [- - -]
                      [- K -] [- K -] [- K -]])

  (t/expect-chess-moves {:piece :R}
                    '[[- - - -]
                      [- - - -]
                      [R - - -]
                      [- - - -]]

                    '[[- - - -] [- - - -]  [R - - -]   [- - - -] [- - - -] [- - - -]
                      [- - - -] [R - - -]  [- - - -]   [- - - -] [- - - -] [- - - -]
                      [- - - -] [- - - -]  [- - - -]   [- R - -] [- - R -] [- - - R]
                      [R - - -] [- - - -]  [- - - -]   [- - - -] [- - - -] [- - - -]])

  (t/expect-chess-moves {:piece :R}
                    '[[- - - - -]
                      [- - - - -]
                      [- - - - -]
                      [R K - - -]
                      [- - - - -]]

                    '[[- - - - -] [- - - - -]  [- - - - -] [R - - - -]
                      [- - - - -] [- - - - -]  [R - - - -] [- - - - -]
                      [- - - - -] [R - - - -]  [- - - - -] [- - - - -]
                      [- K - - -] [- K - - -]  [- K - - -] [- K - - -]
                      [R - - - -] [- - - - -]  [- - - - -] [- - - - -]]))

#_
^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(clerk/add-viewers! [viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer])

#_
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(t/view-test-case (t/expect-moves-2 {:piece :R}
                    '[[- - - - -]
                      [- - - - -]
                      [- - - - -]
                      [R K - - -]
                      [- - - - -]]

                    '[[- - - - -] [- - - - -]  [- - - - -] [R - - - -]
                      [- - - - -] [- - - - -]  [R - - - -] [- - - - -]
                      [- - - - -] [R - - - -]  [- - - - -] [- - - - -]
                      [- K - - -] [- K - - -]  [- K - - -] [- K - - -]
                      [R - - - -] [- - - - -]  [- - - - -] [- - - - -]]) )
