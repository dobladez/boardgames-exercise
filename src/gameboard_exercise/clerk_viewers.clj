;; # Game board viewer for Clerk
^{:nextjournal.clerk/visibility {:code :hide}}
(ns gameboard-exercise.clerk-viewers
  (:require [nextjournal.clerk :as clerk]
            [gameboard-exercise.core :as core]))



(def board-viewer
  {:transform-fn (comp clerk/mark-presented (clerk/update-val core/board->symbolic))
   :pred (fn [board] (= (-> board meta :tag)
                        :gameboard ))
   ;; :render-fn
   ;; '(fn [v] [:div (str "Value:" v)])

   :render-fn
   '(fn [board]
      (let [row-count (count board)
            col-count (count (first board))]
        [:div.inline-grid {:style {
                                   :grid-template-columns (str "repeat(" col-count ", 1fr)")
                                   :grid-template-rows (str "repeat(" row-count ", 1fr)")
                                   :gap 0
                                   :width (str (* 1.3 col-count) "em")
                                   :height (str (* 1.3 row-count) "em")
                                   :font-size 28
                                   :borer "2px solid gray"}}
         (let [piece-to-icon {:r '♜ :n '♞ :b '♝ :q '♛ :k '♚ :p '🨾 :R '♜ :N '♞ :B '♝ :Q '♛ :K '♚ :P '🨾︎ }
               piece-style-by-player {0 "-1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 1px 1px 0 #000"}
               square-style-by-player {0 "text-slate-200" 1 "text-black"}
               ] ;; 🨾♙

           (->> board (map-indexed (fn [row-idx row]
                                     (->> row  (map-indexed (fn [col-idx square]
                                                              (let [player-n (if (= (name square)
                                                                                    (clojure.string/lower-case (name square)))
                                                                               1 0)]
                                                                [:div.inline-block {:id (str col-idx "-" row-idx)
                                                                                    :style  {:display "flex"
                                                                                             :border "1px solid gray"
                                                                                             :text-shadow (piece-style-by-player player-n)
                                                                                             :justify-content "center"
                                                                                             :align-items "center" }
                                                                                    :class (str (if (odd? (+ col-idx row-idx))
                                                                                                  "bg-amber-800"
                                                                                                  "bg-amber-200")
                                                                                                " text-bold "
                                                                                                (square-style-by-player player-n) )}
                                                                 (piece-to-icon square)]))))
                                     )) ))]))})


^{:nextjournal.clerk/visibility {:result :hide}}
(clerk/add-viewers! [board-viewer])


;; ## Test board:
(core/symbolic->board [[:r :n :b :q :k :b :n :r]
                       [:p :p :p :p :p :p :p :p]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:P :P :P :P :P :P :P :P]
                       [:R :N :B :Q :K :B :N :R]])
