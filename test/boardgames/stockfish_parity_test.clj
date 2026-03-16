(ns boardgames.stockfish-parity-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            [boardgames.stockfish-test-utils :as sf]))

(use-fixtures :once sf/stockfish-fixture)

(def ^:private stockfish-chess-game
  (core/make-game "Chess"
                  chess/initiate-chess-board
                  chess/chess-expansion-rules
                  chess/chess-aggregate-rules))

(def ^:private parity-positions
  [{:name "Initial position (white to move)"
    :board chess/initial-chess-symbolic-board
    :turn 0
    :fen {:castling "KQkq"}}

   {:name "Initial position (black to move)"
    :board chess/initial-chess-symbolic-board
    :turn 1
    :fen {:castling "KQkq"}}

   {:name "White short castling available"
    :board '[[- - - - k - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - K - - R]]
    :turn 0
    :fen {:castling "K"}}

   {:name "White short castling blocked by attack on path"
    :board '[[- - - - k r - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - K - - R]]
    :turn 0
    :fen {:castling "K"}}

   {:name "Knight mobility in open board"
    :board '[[- - - - k - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - N - - - -]
             [- - - - - - - -]
             [- - - - - - - -]
             [- - - - K - - -]]
    :turn 0
    :fen {:castling "-"}}])

(defn- build-parity-context
  [game]
  (let [pmoves (core/possible-pmoves game)
        uci->pmoves (group-by sf/pmove->uci pmoves)
        duplicate-uci (->> uci->pmoves
                           (keep (fn [[uci xs]] (when (> (count xs) 1) uci)))
                           set)
        engine-moves (set (keys uci->pmoves))]
    {:pmoves pmoves
     :uci->pmoves uci->pmoves
     :duplicate-uci duplicate-uci
     :engine-moves engine-moves}))

(defn- parity-boards
  [uci->pmoves moves]
  (->> moves
       (keep (fn [uci]
               (some-> (get uci->pmoves uci)
                       first
                       :steps
                       first
                       :board
                       core/board->symbolic)))
       vec))

(defn- assert-parity!
  [{:keys [name board turn fen]}]
  (testing name
    (let [game (cond-> (core/start-game stockfish-chess-game (core/symbolic->board board))
                 (= 1 turn) core/switch-turn)
          fen-str (sf/game->fen game fen)
          {:keys [uci->pmoves duplicate-uci engine-moves]} (build-parity-context game)
          stockfish-result (try
                             {:moves (sf/stockfish-legal-uci-moves fen-str)}
                             (catch Throwable t
                               {:error t}))]
      (is (empty? duplicate-uci)
          (str "Duplicate UCI keys produced by engine move conversion: " (pr-str (sort duplicate-uci))))
      (if-let [t (:error stockfish-result)]
        (is false {:name name
                   :fen fen-str
                   :board (:board game)
                   :error (str t)})
        (let [stockfish-moves (:moves stockfish-result)
              stockfish-boards (parity-boards uci->pmoves stockfish-moves)
              engine-boards (parity-boards uci->pmoves engine-moves)
              missing (set/difference stockfish-moves engine-moves)
              extra (set/difference engine-moves stockfish-moves)]
          (is (= (set stockfish-boards) (set engine-boards))
              (str "FEN: " fen-str
                   "\nMissing (Stockfish only): " (pr-str (sort missing))
                   "\nExtra (Engine only): " (pr-str (sort extra))))))

      ;; Return the same test-case payload shape used by other tests so Clerk can render boards.
      (let [stockfish-moves (or (:moves stockfish-result) #{})
        stockfish-boards (parity-boards uci->pmoves stockfish-moves)
        engine-boards (parity-boards uci->pmoves engine-moves)]
        ^{:boardgames/testcase true}
        [board stockfish-boards engine-boards]))))

(deftest ^:stockfish stockfish-legal-move-parity-test
  (if-not (sf/stockfish-available?)
    (is true (str "Skipping Stockfish parity tests: " (sf/stockfish-unavailable-reason)))
    (doall (map assert-parity! parity-positions))))

(deftest ^:stockfish stockfish-legal-move-parity-random
  (if-not (sf/stockfish-available?)
    (is true (str "Skipping random-position Stockfish parity tests: " (sf/stockfish-unavailable-reason)))
    (doall
     (for [game (sf/random-positions stockfish-chess-game :n-games 5 :max-plies 30)]
       (let [fen-str (sf/game->fen game)
             {:keys [uci->pmoves duplicate-uci engine-moves]} (build-parity-context game)]
         (testing (str "FEN: " fen-str)
           (is (empty? duplicate-uci)
               (str "Duplicate UCI keys produced by engine move conversion: " (pr-str (sort duplicate-uci))))
           (let [stockfish-result (try
                                    {:moves (sf/stockfish-legal-uci-moves fen-str)}
                                    (catch Throwable t
                                      {:error t}))]
             (if-let [t (:error stockfish-result)]
               (is false {:fen fen-str
                          :board (:board game)
                          :error (str t)})
               (let [stockfish-moves (:moves stockfish-result)
                     stockfish-boards (parity-boards uci->pmoves stockfish-moves)
                     engine-boards (parity-boards uci->pmoves engine-moves)
                     missing (set/difference stockfish-moves engine-moves)
                     extra (set/difference engine-moves stockfish-moves)] 
                 (is (= (set stockfish-boards) (set engine-boards))
                     (str "FEN: " fen-str
                          "\nMissing (Stockfish only): " (pr-str (sort missing))
                          "\nExtra (Engine only): " (pr-str (sort extra))))))

             (let [stockfish-moves (or (:moves stockfish-result) #{})
                   stockfish-boards (parity-boards uci->pmoves stockfish-moves)
                   engine-boards (parity-boards uci->pmoves engine-moves)]
               ^{:boardgames/testcase true}
               [(core/board->symbolic (:board game)) stockfish-boards engine-boards]))))))))