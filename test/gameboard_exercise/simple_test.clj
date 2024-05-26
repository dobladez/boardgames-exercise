(ns gameboard-exercise.simple-test
  (:require [clojure.test :refer [deftest is]]

            [gameboard-exercise.core :as core]
            [gameboard-exercise.chess :as chess]
            ))


(deftest example-test
  (is (= 4 (+ 2 2))))



;; Given a board possition, we can start request the list of possible moves and
;; assert that they contain the moves we expect
(deftest chess-rook-moves

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


;; To avoid repetition, we define a helper function to test the possible moves
;; form a given board possition
(defn expect-moves [initial-board expected-boards-set]

  (let [game (core/start-game chess/chess-game (core/symbolic->board initial-board))
        moves (core/possible-pmoves game)
        final-boards (->> (map (comp core/board->symbolic :board first :steps) moves)
                          (into #{}))]

    #_(is (= (count expected-moves-set) (count moves)))
    #_(is (every? #(contains? final-boards %) expected-moves-set))
    (is (=  final-boards expected-boards-set))))



;; Similar to `expect-moves`, but the expected boards are passed _transposed_ so
;; that they can be written as literal values horizontally in the test code.
;; See usage examples below
(defn expect-moves-2 [initial-board expected-boards-transposed]

  (let [n (/ (count expected-boards-transposed)
             (count (first expected-boards-transposed)))
        expected-boards (apply mapv vector (partition n expected-boards-transposed))]

    (expect-moves initial-board (set expected-boards))))



(deftest chess-rook-moves2

  ;; Here we pass the base borad and then the expected boards one by one
  (expect-moves '[[p R -]
                  [- - -]
                  [- K -]]

                '#{[[R - -] ;; Capturing the pawn
                    [- - -]
                    [- K -]]

                   [[p - R]
                    [- - -]
                    [- K -]]

                   [[p - -]
                    [- R -]
                    [- K -]]})

  ;; Here the we express the expected boards "horizontally":
  (expect-moves-2 '[[p R -]
                    [- - -]
                    [- K -]]

                  '[[R - -]  [p - R] [p - -]
                    [- - -]  [- - -] [- R -]
                    [- K -]  [- K -] [- K -]])

  (expect-moves-2 '[[- - - -]
                    [- - - -]
                    [R - - -]
                    [- - - -]]

                  '[[- - - -]  [R - - -]  [- - - -]  [- - - -] [- - - -] [- - - -]
                    [R - - -]  [- - - -]  [- - - -]  [- - - -] [- - - -] [- - - -]
                    [- - - -]  [- - - -]  [- - - -]  [- R - -] [- - R -] [- - - R]
                    [- - - -]  [- - - -]  [R - - -]  [- - - -] [- - - -] [- - - -]])

  (expect-moves-2 '[[- - - - -]
                    [- - - - -]
                    [- - - - -]
                    [R K - - -]
                    [- - - - -]]

                  '[[- - - - -]  [- - - - -] [R - - - -] [R - - - -] [- - - - -]
                    [- - - - -]  [R - - - -] [- - - - -] [- - - - -] [- - - - -]
                    [R - - - -]  [- - - - -] [- - - - -] [- - - - -] [- - - - -]
                    [- K - - -]  [- K - - -] [- K - - -] [- K - - -] [- K - - -]
                    [- - - - -]  [- - - - -] [- - - - -] [- - - - -] [R - - - -]]))
