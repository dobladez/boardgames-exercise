(ns boardgames.simple-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :refer [lower-case upper-case]]
            [boardgames.utils :as utils :refer [ttap>]]
            [boardgames.core :as core]
            [boardgames.chess :as chess]))


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
  [opts initial-board expected-boards]

  (let [{:keys [piece update-board-fn extra-checks] :or {update-board-fn identity extra-checks []}} opts
        game-start (update-board-fn
                    (core/start-game chess/chess-game (core/symbolic->board initial-board)))
        game (if (core/upper-case? piece)
               game-start
               (core/switch-turn game-start))
        piece-type (-> piece name lower-case keyword)
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
                  extra-checks)))))



;; Similar to `expect-moves`, but the expected boards are passed _transposed_ so
;; that they can be written as literal values horizontally in the test code.
;; See usage examples below
(defn expect-moves-2
  [opts initial-board expected-boards-transposed]
  (let [n (/ (count expected-boards-transposed)
             (count (first expected-boards-transposed)))
        expected-boards (apply mapv vector (partition n expected-boards-transposed))]

    (expect-moves opts initial-board expected-boards)))



(deftest chess-rook-moves

  ;; Here we pass the base board and then the expected boards one by one
  ;; Note:  the expected boards must be sorted by [x y] position of the moving piece
  (expect-moves {:piece :R}

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
  (expect-moves-2 {:piece :R}
                  '[[p R -]
                    [- - -]
                    [- K -]]

                  '[[R - -] [p - -] [p - R]
                    [- - -] [- R -] [- - -]
                    [- K -] [- K -] [- K -]])

  (expect-moves-2 {:piece :R
                   :extra-checks [{:captures '(:p)} {} {}]}

                  '[[p R -]
                    [- - -]
                    [- K -]]

                  '[[R - -] [p - -] [p - R]
                    [- - -] [- R -] [- - -]
                    [- K -] [- K -] [- K -]])

  (expect-moves-2 {:piece :R}
                  '[[- - - -]
                    [- - - -]
                    [R - - -]
                    [- - - -]]

                  '[[- - - -] [- - - -]  [R - - -]   [- - - -] [- - - -] [- - - -]
                    [- - - -] [R - - -]  [- - - -]   [- - - -] [- - - -] [- - - -]
                    [- - - -] [- - - -]  [- - - -]   [- R - -] [- - R -] [- - - R]
                    [R - - -] [- - - -]  [- - - -]   [- - - -] [- - - -] [- - - -]])

  (expect-moves-2 {:piece :R}
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


(defn flag-all-pieces-moved [game]
  (update-in game [:board :pieces]
             (fn [pieces] (->> pieces
                               (map #(update-in % [:flags] conj :moved))
                               (into #{})))))

(deftest chess-pawn-moves

  (expect-moves-2 {:piece :P}

                  '[[- - - - -]
                    [- - - R -]
                    [- - - - -]
                    [- P - P -]
                    [- - - - -]]

                  '[[- - - - -] [- - - - -] [- - - - -]
                    [- - - R -] [- P - R -] [- - - R -]
                    [- P - - -] [- - - - -] [- - - P -]
                    [- - - P -] [- - - P -] [- P - - -]
                    [- - - - -] [- - - - -] [- - - - -]])

  (expect-moves-2 {:piece :p} ;; lower case sets the turn to black

                  '[[- - - - -]
                    [- p - - -]
                    [- - - - -]
                    [- - - - -]
                    [- - - - -]]

                  ;; Reminder: boards expected to be sorted by X and then Y position of moving piece
                  '[[- - - - -] [- - - - -]
                    [- - - - -] [- - - - -]
                    [- - - - -] [- p - - -]
                    [- p - - -] [- - - - -]
                    [- - - - -] [- - - - -]])

  ;; Test for already moved pawn
  (expect-moves-2 {:piece :P
                   :update-board-fn flag-all-pieces-moved}

                  '[[- - - - -]
                    [- - - - -]
                    [- - - - -]
                    [- P - - -]
                    [- - - - -]]

                  '[[- - - - -]
                    [- - - - -]
                    [- P - - -]
                    [- - - - -]
                    [- - - - -]])

;; Pawn capturing
  (expect-moves-2 {:piece :P
                   :extra-checks [{} {} {:captures '(:p)}]}

                  '[[- - - - -]
                    [- - - - -]
                    [- - p - -]
                    [- P - - -]
                    [- - - - -]]

                  '[[- - - - -] [- - - - -] [- - - - -]
                    [- - - - -] [- P - - -] [- - - - -]
                    [- P p - -] [- - p - -] [- - P - -]
                    [- - - - -] [- - - - -] [- - - - -]
                    [- - - - -] [- - - - -] [- - - - -]]))
