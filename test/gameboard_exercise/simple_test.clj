(ns gameboard-exercise.simple-test
  (:require [clojure.test :refer [deftest is]]
            [gameboard-exercise.utils :as utils :refer [ttap>]]
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

(defn expect-moves
  ([piece-type initial-board expected-boards]
   (expect-moves piece-type initial-board expected-boards []))

  ([piece-type initial-board expected-boards extra-checks]
   (let [game (core/start-game chess/chess-game (core/symbolic->board initial-board))
         raw-moves (core/possible-pmoves game)

        ;; Let's sort all the possible moves by piece coordinates
         moves (->> raw-moves
                    (filter #(= piece-type (-> % :steps first :piece :type)))
                    (sort-by #(-> % :steps first :piece :pos second))
                    (sort-by #(-> % :steps first :piece :pos first)))

         final-boards (mapv (comp core/board->symbolic :board first :steps) moves)]

     #_(is (= (count expected-moves-set) (count moves)))
     #_(is (every? #(contains? final-boards %) expected-moves-set))
     (is (=  expected-boards final-boards))

     (when (seq extra-checks)
       (doall (map (fn [actual-move extra-expected]
                     (is (= extra-expected
                            (merge {}
                                   (when-let [captured (->> actual-move :steps first :captures seq)]
                                     {:captures (map :type captured)})))))
                   moves
                   extra-checks))))))



;; Similar to `expect-moves`, but the expected boards are passed _transposed_ so
;; that they can be written as literal values horizontally in the test code.
;; See usage examples below
(defn expect-moves-2
  ([piece-type initial-board expected-boards-transposed]
   (expect-moves-2 piece-type initial-board expected-boards-transposed []))

  ([piece-type initial-board expected-boards-transposed extra-checks]
   (let [n (/ (count expected-boards-transposed)
              (count (first expected-boards-transposed)))
         expected-boards (apply mapv vector (partition n expected-boards-transposed))]

     (expect-moves piece-type initial-board expected-boards extra-checks))))



(deftest chess-rook-moves2

  ;; Here we pass the base board and then the expected boards one by one
  ;; Note:  the expected boards must be sorted by [x y] position of the moving piece
  (expect-moves :r
                '[[p R -]
                  [- - -]
                  [- K -]]

                '[[[R - -] ;; Capturing the pawn
                   [- - -]
                   [- K -]]

                  [[p - -]
                   [- R -]
                   [- K -]]

                  [[p - R]
                   [- - -]
                   [- K -]]])

  ;; Here the we express the expected boards "horizontally":
  (expect-moves-2 :r
                  '[[p R -]
                    [- - -]
                    [- K -]]

                  '[[R - -] [p - -] [p - R]
                    [- - -] [- R -] [- - -]
                    [- K -] [- K -] [- K -]] )

  (expect-moves-2 :r
                  '[[p R -]
                    [- - -]
                    [- K -]]

                  '[[R - -] [p - -] [p - R]
                    [- - -] [- R -] [- - -]
                    [- K -] [- K -] [- K -]]

                  [{:captures '(:p)} {} {}])

  (expect-moves-2 :r
                  '[[- - - -]
                    [- - - -]
                    [R - - -]
                    [- - - -]]

                  '[[- - - -] [- - - -]  [R - - -]   [- - - -] [- - - -] [- - - -]
                    [- - - -] [R - - -]  [- - - -]   [- - - -] [- - - -] [- - - -]
                    [- - - -] [- - - -]  [- - - -]   [- R - -] [- - R -] [- - - R]
                    [R - - -] [- - - -]  [- - - -]   [- - - -] [- - - -] [- - - -]])

  (expect-moves-2 :r
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
