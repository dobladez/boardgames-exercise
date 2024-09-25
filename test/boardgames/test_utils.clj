(ns boardgames.test-utils
  (:require [clojure.test :refer [deftest is]]
            [clojure.data :as data]
            [clojure.string :refer [lower-case upper-case]]
            [boardgames.utils :as utils :refer [upper-case? TAP>]]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            [nextjournal.clerk :as clerk]))

(defn expect-moves* "helper function to test the possible moves form a given board possition"
  [game-def opts initial-board expected-boards]

  (let [{:keys [piece update-board-fn extra-checks] :or {update-board-fn identity extra-checks []}} opts
        game-start (update-board-fn
                    (core/start-game game-def (core/symbolic->board initial-board)))
        game (if (upper-case? piece)
               game-start
               (core/switch-turn game-start))
        piece-type (-> piece name lower-case keyword)
        raw-moves (core/possible-pmoves game)

        ;; Let's sort all the possible moves by final piece coordinates
        moves (->> raw-moves
                   (filter #(= piece-type (-> % :steps last :piece :type)))
                   (sort-by #(-> % :steps first :piece :pos second))
                   (sort-by #(-> % :steps first :piece :pos first)))

        final-boards (mapv (comp core/board->symbolic :board first :steps) moves)]

    #_(is (= (count expected-moves-set) (count moves)))
    #_(is (every? #(contains? final-boards %) expected-moves-set))
    (is (=  (set expected-boards) (set final-boards)))

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

(defn expect-moves
  "Similar to `expect-moves*`, but the expected boards are passed _transposed_ so that they can be written as literal values horizontally in the test code. See usage examples below"
  [game-def opts initial-board expected-boards-transposed]

  (let [vecs-count (count expected-boards-transposed)
        expected-boards (if (zero? vecs-count) []
                            (apply mapv vector (partition (/ vecs-count
                                                             (count
                                                              (first expected-boards-transposed)))
                                                          expected-boards-transposed)))]

    (expect-moves* game-def opts initial-board expected-boards)))

(defn expect-chess-moves*
  [opts initial-board expected-boards]
  (expect-moves* chess/chess-game opts initial-board expected-boards))

(defn expect-chess-moves
  "Similar to `expect-chess-moves*`, but the expected boards are passed _transposed_ so that they can be written as literal values horizontally in the test code. See usage examples below"
  [opts initial-board expected-boards-transposed]
  (expect-moves chess/chess-game opts initial-board expected-boards-transposed))



(deftest example-chess-rook-tests

  ;; Here we pass the base board and then the expected boards one by one
  ;; Note:  the expected boards must be sorted by [x y] position of the moving piece
  (expect-chess-moves* {:piece :R}

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
  (expect-chess-moves {:piece :R}
                  '[[p R -]
                    [- - -]
                    [- K -]]

                  '[[R - -] [p - -] [p - R]
                    [- - -] [- R -] [- - -]
                    [- K -] [- K -] [- K -]])

  (expect-chess-moves {:piece :R
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

        [diff-expected diff-actual diff-correct] (map vec (data/diff (set expected-boards) (set final-boards)))]

    (clerk/html
     [:div.flex.flex-row.gap-2 {}
      [:div
       [:h4 "Initial Position"]
       (clerk/row initial-board)]

      (if (and (empty? diff-expected) (empty? diff-actual))
        [:h4 "All Good!"]
        (list [:div.border-l.pl-2
               [:h4 "Only on Expected set"]
               (clerk/col diff-expected)]
              [:div.border-l.pl-2
               [:h4 "Only on Actual set"]
               (clerk/col diff-actual)]
              [:div.border-l.pl-2
               [:h4 "OK"]
               (clerk/col diff-correct)
               ]))])))
