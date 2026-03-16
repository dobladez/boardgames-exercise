(ns boardgames.stockfish-test-utils
  (:require [clojure.string :as str]
            [boardgames.core :as core])
  (:import [java.io BufferedReader BufferedWriter InputStreamReader OutputStreamWriter]
           [java.util.concurrent LinkedBlockingQueue TimeoutException]))

(def ^:private default-timeout-ms 10000)

(def ^:private stockfish-debug?
  (= "true" (some-> (System/getenv "STOCKFISH_DEBUG") str/lower-case)))

(defonce ^:private !stockfish (atom nil))
(defonce ^:private !stockfish-unavailable-reason (atom nil))

(defn stockfish-bin []
  (or (System/getenv "STOCKFISH_BIN")
      "stockfish"))

(defn- send-line!
  [{:keys [in]} line]
  (when stockfish-debug?
    (println "[Stockfish-Runner] >>" line))
  (.write ^BufferedWriter in (str line "\n"))
  (.flush ^BufferedWriter in))

(defn- read-until!
  [{:keys [queue]} pred timeout-ms]
  (loop [lines []]
    (let [line (.poll ^LinkedBlockingQueue queue timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)]
      (cond
        (nil? line)
        (throw (TimeoutException.
                (str "Timed out waiting for Stockfish output. Collected lines: " (pr-str lines))))

        (= ::eof line)
        lines

        :else
        (let [next-lines (conj lines line)]
          (if (pred line)
            next-lines
            (recur next-lines)))))))

(defn- start-stockfish-process!
  []
  (let [process (-> (ProcessBuilder. [^String (stockfish-bin)])
                    (.redirectErrorStream true)
                    (.start))
        queue  (LinkedBlockingQueue.)
        reader (-> process .getInputStream InputStreamReader. BufferedReader.)
        _      (doto (Thread. ^Runnable
                              (fn []
                                (try
                                  (loop []
                                    (if-let [line (.readLine ^BufferedReader reader)]
                                      (do
                                        (when stockfish-debug?
                                          (println "[Stockfish-Runner]" line))
                                        (.put ^LinkedBlockingQueue queue line)
                                        (recur))
                                      (.put ^LinkedBlockingQueue queue ::eof)))
                                  (catch Throwable _
                                    (try (.put ^LinkedBlockingQueue queue ::eof)
                                         (catch Throwable _))))))
                (.setDaemon true)
                (.start))
        stockfish {:process process
                   :in      (-> process .getOutputStream OutputStreamWriter. BufferedWriter.)
                   :queue   queue}]
    (send-line! stockfish "uci")
    (read-until! stockfish #(= % "uciok") default-timeout-ms)
    (send-line! stockfish "isready")
    (read-until! stockfish #(= % "readyok") default-timeout-ms)
    stockfish))

(defn ensure-stockfish!
  []
  (or @!stockfish
      (try
        (let [instance (start-stockfish-process!)]
          (reset! !stockfish instance)
          (reset! !stockfish-unavailable-reason nil)
          instance)
        (catch Throwable t
          (reset! !stockfish nil)
          (reset! !stockfish-unavailable-reason (.getMessage t))
          nil))))

(defn stockfish-available?
  []
  (boolean (ensure-stockfish!)))

(defn stockfish-unavailable-reason
  []
  (or @!stockfish-unavailable-reason
      (str "Could not start Stockfish. Set STOCKFISH_BIN to your binary path. Tried: " (stockfish-bin))))

(defn stop-stockfish!
  []
  (when-let [{:keys [process] :as stockfish} @!stockfish]
    (try
      (send-line! stockfish "quit")
      (catch Throwable _))
    (try
      (.waitFor process 1 java.util.concurrent.TimeUnit/SECONDS)
      (catch Throwable _))
    (when (.isAlive process)
      (.destroy process)))
  (reset! !stockfish nil))

(defn stockfish-fixture
  [f]
  (ensure-stockfish!)
  (try
    (f)
    (finally
      (stop-stockfish!))))

(def ^:private perft-line-re
  #"^\s*([a-h][1-8][a-h][1-8][qrbn]?)\s*:\s*\d+\s*$")

(defn stockfish-legal-uci-moves
  [fen]
  (when-let [stockfish (ensure-stockfish!)]
    (send-line! stockfish "ucinewgame")
    (send-line! stockfish "isready")
    (read-until! stockfish #(= % "readyok") default-timeout-ms)

    (send-line! stockfish (str "position fen " fen))
    (send-line! stockfish "go perft 1")
    (send-line! stockfish "isready")

    (let [lines (read-until! stockfish #(= % "readyok") default-timeout-ms)]
      (->> lines
           (keep #(second (re-matches perft-line-re %)))
           set))))

(def ^:private files "abcdefgh")

(defn pos->uci-square
  [[x y]]
  (str (.charAt files x) (inc y)))

(defn- castling-pmove?
  [pmove]
  (and (= :k (-> pmove :steps last :piece :type))
       (= 3 (count (:steps pmove)))
       (= :r (-> pmove :steps first :piece :type))))

(defn- castling-target-pos
  [pmove]
  (let [from (-> pmove :steps last :piece :pos)
        prior-pos (-> pmove :steps second :piece :pos)
        dir (mapv - prior-pos from)]
    (mapv + from (mapv #(* 2 %) dir))))

(defn pmove->uci
  [pmove]
  (let [from (-> pmove :steps last :piece :pos)
        to (if (castling-pmove? pmove)
             (castling-target-pos pmove)
             (-> pmove :steps first :piece :pos))]
    (str (pos->uci-square from)
         (pos->uci-square to))))

(defn game->uci-moves
  [game]
  (->> (core/possible-pmoves game)
       (map pmove->uci)
       set))

(defn- piece->fen
  [piece]
  (case piece
    - "."
    (name piece)))

(defn- row->fen
  [row]
  (let [row-str (apply str (map piece->fen row))
        grouped (re-seq #"\.+|[^\.]+" row-str)]
    (apply str (map (fn [s]
                      (if (.startsWith s ".")
                        (str (count s))
                        s))
                    grouped))))

(defn- board->piece-placement-fen
  [symbolic-board]
  (->> symbolic-board
       (map row->fen)
       (interpose "/")
       (apply str)))

(defn- piece-unmoved-at?
  "Returns true if the board has a piece of the given type/player at pos
   that has never moved (no :moved flag in its :flags set)."
  [board player piece-type pos]
  (when-let [p (first (filter #(= (:pos %) pos) (:pieces board)))]
    (and (= (:type p) piece-type)
         (= (:player p) player)
         (not (contains? (:flags p) :moved)))))

(defn derive-castling-rights
  "Derives the FEN castling availability string from the board by checking
   whether each king and its corner rooks are on their starting squares and
   have never moved.  Returns e.g. \"KQkq\", \"K\", or \"-\"."
  [board]
  (let [wk? (piece-unmoved-at? board 0 :k [4 0])
        bk? (piece-unmoved-at? board 1 :k [4 7])
        rights (cond-> []
                 (and wk? (piece-unmoved-at? board 0 :r [7 0])) (conj "K")
                 (and wk? (piece-unmoved-at? board 0 :r [0 0])) (conj "Q")
                 (and bk? (piece-unmoved-at? board 1 :r [7 7])) (conj "k")
                 (and bk? (piece-unmoved-at? board 1 :r [0 7])) (conj "q"))]
    (if (empty? rights) "-" (apply str rights))))

(defn game->fen
  ([game] (game->fen game {}))
  ([game {:keys [castling en-passant halfmove fullmove]
          :or {en-passant "-"
               halfmove 0
               fullmove 1}}]
   (let [castling  (or castling (derive-castling-rights (:board game)))
         placement (-> game :board core/board->symbolic board->piece-placement-fen)
         side-to-move (if (= 0 (:turn game)) "w" "b")]
     (str/join " " [placement side-to-move castling en-passant halfmove fullmove]))))

(defn random-positions
  "Plays out n-games random games from game-def's initial state, each up to
   max-plies moves deep, collecting a game-state snapshot at every ply.
   seed makes the sequence fully reproducible."
  [game-def & {:keys [n-games max-plies seed]
               :or {n-games 5 max-plies 40 seed 42}}]
  (let [rng (java.util.Random. seed)]
    (->> (repeatedly n-games
                     (fn []
                       (loop [game  (core/start-game game-def)
                              plies 0
                              acc   []]
                         (let [moves (vec (core/possible-pmoves game))]
                           (if (or (empty? moves) (>= plies max-plies))
                             acc
                             (let [chosen     (nth moves (.nextInt rng (count moves)))
                                   next-board (-> chosen :steps first :board)
                                   next-game  (-> game
                                                  (assoc :board next-board)
                                                  core/switch-turn)]
                               (recur next-game (inc plies) (conj acc next-game))))))))
         (apply concat))))