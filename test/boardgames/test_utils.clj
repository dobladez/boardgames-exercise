(ns boardgames.test-utils
  (:require [clojure.test :refer [deftest is]]
            [clojure.data :as data]
            [clojure.string :refer [lower-case upper-case]]
            [boardgames.utils :as utils :refer [TAP>]]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            [nextjournal.clerk :as clerk]))


(defn expect-moves "helper function to test the possible moves form a given board possition"
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
                  extra-checks)))

    ^{:boardgames/testcase true}
    [initial-board expected-boards final-boards]))

(defn expect-moves-2
  "Similar to `expect-moves`, but the expected boards are passed _transposed_ so that they can be written as literal values horizontally in the test code. See usage examples below"
  [opts initial-board expected-boards-transposed]

  (let [vecs-count (count expected-boards-transposed)
        expected-boards (if (zero? vecs-count) []
                            (apply mapv vector (partition (/ vecs-count
                                                             (count
                                                              (first expected-boards-transposed)))
                                                          expected-boards-transposed)))]

    (expect-moves opts initial-board expected-boards)))

(deftest example-chess-rook-tests

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
                    [- K -] [- K -] [- K -]]))


(defn flag-all-pieces-moved [game]
  (update-in game [:board :pieces]
             (fn [pieces] (->> pieces
                               (map #(update-in % [:flags] conj :moved))
                               (into #{})))))

(defn view-test-case [result]
  (let [[sym-initial-board sym-expected-boards sym-final-boards] result
        expected-boards (mapv core/symbolic->board sym-expected-boards)
        final-boards (mapv core/symbolic->board sym-final-boards)
        initial-board (core/symbolic->board sym-initial-board)

        [diff-expected diff-actual _] (map vec (data/diff (set expected-boards) (set final-boards)))]

    (clerk/html
     [:div {}
      [:h2 "Initial Position:"]
      (clerk/row initial-board)

      (if (and (empty? diff-expected) (empty? diff-actual))
        [:h2 "All Good!"]
        [:div
         [:h3 "Only on Expected set:"]
         (clerk/row diff-expected)
         [:h3 "Only on Actual set:"]
         (clerk/row diff-actual)])])))
