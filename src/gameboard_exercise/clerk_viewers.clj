;; # Game board viewer for Clerk
^{:nextjournal.clerk/visibility {:code :hide}}
(ns gameboard-exercise.clerk-viewers
  (:require [nextjournal.clerk :as clerk]
            [gameboard-exercise.core :as core]))

(def ^:private board-render-fn2

  '(fn [[board from-pos to-pos]]
     (let [row-count (count board)
           col-count (count (first board))]
       [:div
        #_[:span (str "From " from-pos " to " to-pos)]
        [:div.inline-grid {:style {:grid-template-columns (str "repeat(" col-count ", 1fr)")
                                   :grid-template-rows (str "repeat(" row-count ", 1fr)")
                                   :gap 0
                                    ;; :width (str (* 1.3 col-count) "em")
                                    ;; :height (str (* 1.3 row-count) "em")
                                   :font-size "36px"
                                   :border "2px solid gray"}}
         (let [piece-to-icon {:r '♜ :n '♞ :b '♝ :q '♛ :k '♚ :p '♟
                              :R '♜ :N '♞ :B '♝ :Q '♛ :K '♚ :P '♟}

               piece-style-by-player {0 "-1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 1px 1px 0 #000"}
               square-style-by-player {0 "text-slate-200" 1 "text-black"}] ;; 🨾♙

           (->> board (map-indexed (fn [inverse-row-idx row]
                                     (->> row
                                          (map-indexed (fn [col-idx square]
                                                         (let [row-idx (dec (- row-count inverse-row-idx))
                                                               col-idx col-idx
                                                               player-n (if (= (name square)
                                                                               (clojure.string/lower-case (name square)))
                                                                          1 0)]
                                                           [:div.inline-block.flex.border-solid.justify-center.items-center.w-8.h-8.leading-8
                                                            {:id (str col-idx "-" row-idx)
                                                             :style  {#_#_:display "flex"
                                                                      #_#_:border "1px solid gray"
                                                                      :border-color (when (contains?  #{from-pos to-pos}  [col-idx row-idx]) "blue")
                                                                      :border-width (when (contains?  #{from-pos to-pos}  [col-idx row-idx]) "2px")
                                                                      :text-shadow (piece-style-by-player player-n)
                                                                      #_#_:justify-content "center"
                                                                      #_#_:align-items "center"
                                                                      :background-color (if (odd? (+ col-idx row-idx))
                                                                                          "#b58863"
                                                                                          "#f0d9b5")}
                                                             :class (str #_(if (odd? (+ col-idx row-idx))
                                                                             "bg-amber-800"
                                                                             "bg-amber-200")
                                                                     " text-bold "
                                                                         (square-style-by-player player-n))}
                                                            (piece-to-icon square)]))))))))]])))

(def board-viewer
  {:transform-fn
   (comp clerk/mark-presented (clerk/update-val  (fn [board]
                                                   (let [board (core/board->symbolic board)
                                                         from-pos nil
                                                         to-pos nil]
                                                     [board from-pos to-pos]))))

   #_(clerk/update-val board->symbolic2)

   :pred (fn [board] (= (-> board meta :tag)
                        :gameboard))
   ;; :render-fn
   ;; '(fn [v] [:pre (str "meta3:" (meta v))])

   :render-fn board-render-fn2
   #_'(fn [board]
        (let [row-count (count board)
              col-count (count (first board))]
          [:div.inline-grid {:style {:grid-template-columns (str "repeat(" col-count ", 1fr)")
                                     :grid-template-rows (str "repeat(" row-count ", 1fr)")
                                     :gap 0
                                   ;; :width (str (* 1.3 col-count) "em")
                                   ;; :height (str (* 1.3 row-count) "em")
                                     :font-size "36px"
                                     :border "2px solid gray"}}
           (let [piece-to-icon {:r '♜ :n '♞ :b '♝ :q '♛ :k '♚ :p '♟
                                :R '♜ :N '♞ :B '♝ :Q '♛ :K '♚ :P '♟}

                 piece-style-by-player {0 "-1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 1px 1px 0 #000"}
                 square-style-by-player {0 "text-slate-200" 1 "text-black"}] ;; 🨾♙

             (->> board (map-indexed (fn [row-idx row]
                                       (->> row
                                            (map-indexed (fn [col-idx square]
                                                           (let [player-n (if (= (name square)
                                                                                 (clojure.string/lower-case (name square)))
                                                                            1 0)]
                                                             [:div.inline-block.flex.border-1.border-solid.justify-center.items-center.w-8.h-8.leading-8
                                                              {:id (str col-idx "-" row-idx)
                                                               :style  {#_#_:display "flex"
                                                                        #_#_:border "1px solid gray"

                                                                        :text-shadow (piece-style-by-player player-n)
                                                                        #_#_:justify-content "center"
                                                                        #_#_:align-items "center"
                                                                        :background-color (if (odd? (+ col-idx row-idx))
                                                                                            "#b58863"
                                                                                            "#f0d9b5")}
                                                               :class (str #_(if (odd? (+ col-idx row-idx))
                                                                               "bg-amber-800"
                                                                               "bg-amber-200")
                                                                       " text-bold "
                                                                           (square-style-by-player player-n))}
                                                              (piece-to-icon square)]))))))))]))})

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

(def board-move-viewer
  {:transform-fn
   (comp clerk/mark-presented (clerk/update-val
                               (fn [move]
                                 #_(tap> move)
                                 (let [steps (:steps move)
                                       board (:board (first steps))
                                       from-pos (->  (last steps) :piece :pos)
                                       to-pos  (->  (first steps) :piece :pos)]

                                   [(core/board->symbolic board) from-pos to-pos]
                                   #_(update-in move [:steps]
                                                (fn [steps]
                                                  (map #(update % :board core/board->symbolic) steps)))))))
   ;; :pred (fn [maybe-steps] (and (seq? maybe-steps)
   ;;                              (map? (first maybe-steps))
   ;;                              (contains? (first maybe-steps) :board)))
   :pred (fn [maybe-move] (and (map? maybe-move)
                               (contains? maybe-move :flags)
                               (contains? maybe-move :steps)))

;; :render-fn
   ;; '(fn [v] [:div (str "Value:" v)])

   :render-fn board-render-fn2
   #_'(fn [move]

        (let [steps (:steps move)
              board (:board (first steps))
              from-pos (->  (last steps) :piece :pos)
              to-pos  (->  (first steps) :piece :pos)
              row-count (count board)
              col-count (count (first board))]
          [:div
           [:h3 (str "From " from-pos " to " to-pos)]
           [:div.inline-grid {:style {:grid-template-columns (str "repeat(" col-count ", 1fr)")
                                      :grid-template-rows (str "repeat(" row-count ", 1fr)")
                                      :gap 0
                                    ;; :width (str (* 1.3 col-count) "em")
                                    ;; :height (str (* 1.3 row-count) "em")
                                      :font-size "36px"
                                      :border "2px solid gray"}}
            (let [piece-to-icon {:r '♜ :n '♞ :b '♝ :q '♛ :k '♚ :p '♟
                                 :R '♜ :N '♞ :B '♝ :Q '♛ :K '♚ :P '♟}

                  piece-style-by-player {0 "-1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 1px 1px 0 #000"}
                  square-style-by-player {0 "text-slate-200" 1 "text-black"}] ;; 🨾♙

              (->> board (map-indexed (fn [inverse-row-idx row]
                                        (->> row
                                             (map-indexed (fn [col-idx square]
                                                            (let [row-idx (dec (- row-count inverse-row-idx))
                                                                  col-idx col-idx
                                                                  player-n (if (= (name square)
                                                                                  (clojure.string/lower-case (name square)))
                                                                             1 0)]
                                                              [:div.inline-block.flex.border-solid.justify-center.items-center.w-8.h-8.leading-8
                                                               {:id (str col-idx "-" row-idx)
                                                                :style  {#_#_:display "flex"
                                                                         #_#_:border "1px solid gray"
                                                                         :border-color (when (contains?  #{from-pos to-pos}  [col-idx row-idx]) "blue")
                                                                         :border-width (when (contains?  #{from-pos to-pos}  [col-idx row-idx]) "2px")
                                                                         :text-shadow (piece-style-by-player player-n)
                                                                         #_#_:justify-content "center"
                                                                         #_#_:align-items "center"
                                                                         :background-color (if (odd? (+ col-idx row-idx))
                                                                                             "#b58863"
                                                                                             "#f0d9b5")}
                                                                :class (str #_(if (odd? (+ col-idx row-idx))
                                                                                "bg-amber-800"
                                                                                "bg-amber-200")
                                                                        " text-bold "
                                                                            (square-style-by-player player-n))}
                                                               (piece-to-icon square)]))))))))]]))})

;;
;; Viewer based on chessboard-element (external custom element lib)
;;

(defn- piece->fen [piece]
  (case piece
    :- "."
    (name piece)))

(defn- row->fen [row]
  (let [row-str (apply str (map piece->fen row))
        grouped (re-seq #"\.+|[^\.]+" row-str)]
    (apply str (map (fn [s]
                      (if (.startsWith s ".")
                        (str (count s))
                        s))
                    grouped))))

(defn board->fen [board]
  (->> board
       (map row->fen)
       (interpose "/")
       (apply str)))

(def chess-board-viewer
  {:pred (fn [board] (= (-> board meta :tag)
                        :gameboard))
   :transform-fn (clerk/update-val
                  (fn [board]
                    #_(tap> board)

                    (-> board
                        core/board->symbolic
                        board->fen))) ;;  clerk/mark-presented
   :render-fn '(fn [fen-str]

                 (let [script-src "https://unpkg.com/chessboard-element?module"
                       existing-script (js/document.querySelector (str "script[src='" script-src "'"))]
                   (when-not existing-script
                     (let [script-elem (js/document.createElement "script")]
                       (.setAttribute script-elem "type" "module")
                       (.setAttribute script-elem "src" "https://unpkg.com/chessboard-element?module")
                       (.appendChild (.-head js/document) script-elem))))

                 [:chess-board.w-72 {:position fen-str #_"rnbqkbnr/8/8/8/8/8/8/RNBQKBNR"}]
                 #_(when value
                     [nextjournal.clerk.render/with-d3-require
                      {:package
                       #_["@shoelace-style/shoelace"]
                       ["@shoelace-style/shoelace/dist/components/button/button.js"]
                       #_["https://unpkg.com/chessboard-element/bundled/chessboard-element.bundled.js"]
                       #_["chessboard-element@1.2.0"]}
                      (fn [chessboard-element]
                        [:div
                         [:script {:type "module" :src "https://unpkg.com/chessboard-element?module"}]
                         [:div [:chess-board]]]
                        #_[:div {:ref (fn [el] (when el
                                                 (.render mermaid (str (gensym)) value #(set! (.-innerHTML el) %))))}])]))})
