;; # Game board viewer for Clerk
^{:nextjournal.clerk/visibility {:code :hide}}
(ns gameboard-exercise.clerk-viewers
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk-viewer]
            [gameboard-exercise.core :as core]))

(def ^:private board-render-fn

  '(fn [[board from-pos to-pos captures]]
     (let [row-count (count board)
           col-count (count (first board))
           capture-pos (:pos (first captures))
           piece-to-icon '{r ♜ n ♞ b ♝ q ♛ k ♚ p ♟ R ♜ N ♞ B ♝ Q ♛ K ♚ P ♟}
           captures-icon (some-> captures first :type name symbol piece-to-icon)]
       [:div
        [:div.inline-grid {:style {:grid-template-columns (str "repeat(" col-count ", 1fr)")
                                   :grid-template-rows (str "repeat(" row-count ", 1fr)")
                                   :gap 0
                                   :font-size "36px"
                                   :border "2px solid gray"}}
         (let [piece-style-by-player {0 "-1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 1px 1px 0 #000"}
               square-style-by-player {0 "text-slate-200" 1 "text-black"}] ;; 🨾♙
           (->> board (map-indexed (fn [inverse-row-idx row]
                                     (->> row
                                          (map-indexed (fn [col-idx square*]
                                                         (let [square square* #_ (str square*)
                                                               row-idx (dec (- row-count inverse-row-idx))
                                                               col-idx col-idx
                                                               player-n (if (= (name square)
                                                                               (clojure.string/lower-case (name square)))
                                                                          1 0)]
                                                           [:div.inline-block.flex.border-solid.justify-center.items-center.w-8.h-8.leading-8
                                                            {:id (str col-idx "-" row-idx)
                                                             :style  {:border-color (condp = (str [col-idx row-idx])
                                                                                      (str capture-pos) "red"
                                                                                      (str from-pos) "blue"
                                                                                      (str to-pos) "green"
                                                                                      nil)
                                                                      :border-width (when (contains?  #{from-pos to-pos}  [col-idx row-idx]) "2px")
                                                                      :text-shadow (piece-style-by-player player-n)
                                                                      :background-color (if (odd? (+ col-idx row-idx))
                                                                                          "#f0d9b5"
                                                                                          "#b58863")}
                                                             :class (str " text-bold " (square-style-by-player player-n))}
                                                            (if (= (str [col-idx row-idx]) (str capture-pos))
                                                              [:div.group
                                                               [:div.group-hover:hidden (piece-to-icon square)]
                                                               (when (= (str [col-idx row-idx]) (str capture-pos))
                                                                 [:div.text-red-500.hidden.group-hover:inline captures-icon])]
                                                              (piece-to-icon square))]))))))))]
        (when (and from-pos to-pos)
          [:span.text-slate-500.text-xs.text-center.font-sans (str "From " from-pos " to " to-pos
                                                                   (when capture-pos (str " capturing " (some-> captures first :type))))])])))

(def board-viewer
  {:name `board-viewer
   ;; :pred (fn [board] (= (-> board meta :tag)
   ;;                      :gameboard))
   :transform-fn
   (comp clerk/mark-presented (clerk/update-val  (fn [board]
                                                   (let [board (core/board->symbolic board)
                                                         from-pos nil
                                                         to-pos nil]
                                                     [board from-pos to-pos]))))
   :render-fn board-render-fn})

^{:nextjournal.clerk/visibility {:result :hide}}
(clerk/add-viewers! [board-viewer])

;; ## Test board:
^{::clerk/viewer board-viewer}
(def test-board
  (core/symbolic->board [[:r :n :b :q :k :b :n :r]
                         [:p :p :p :p :p :p :p :p]
                         [:- :- :- :- :- :- :- :-]
                         [:- :- :- :- :- :- :- :-]
                         [:- :- :- :- :- :- :- :-]
                         [:- :- :- :- :- :- :- :-]
                         [:P :P :P :P :P :P :P :P]
                         [:R :N :B :Q :K :B :N :R]]))


(defn new-tabbed-viewer [viewers-map]
  {:transform-fn (comp clerk/mark-preserve-keys
                       (clerk/update-val (fn [val]
                                           (into {} (map (fn [[k viewer]]
                                                           [k (clerk/with-viewer viewer
                                                                {#_#_::clerk/render-evaluator :cherry
                                                                 :nextjournal.clerk/auto-expand-results? true}
                                                                val)])
                                                         viewers-map)))))
   :render-fn '(fn [label->val]
                 (reagent.core/with-let [!selected-label (reagent.core/atom (ffirst label->val))]
                   [:div.flex.justify-start
                    [:div {:class "transition-[height]"}
                     (into
                      [:div.flex.items-center.font-sans.text-xs.mb-3
                       [:span.text-slate-500.mr-2 "View As:"]]
                      (map (fn [label]
                             [:button.px-3.py-1.font-medium.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.transition
                              {:class (if (= @!selected-label label) "bg-indigo-100 text-indigo-600" "text-slate-500")
                               :on-click #(reset! !selected-label label)}
                              label]))
                      (keys label->val))
                     [:div.transition-all.transition.duration-300.ease-in-out
                      [nextjournal.clerk.render/inspect-presented (get label->val @!selected-label)]]]]))})

(defn new-tabbed-viewer-animated-WIP [viewers-map]
  {:transform-fn (comp clerk/mark-preserve-keys
                       (clerk/update-val (fn [val]
                                           (into {} (map (fn [[k viewer]]
                                                           [k (clerk/with-viewer viewer {:nextjournal.clerk/auto-expand-results? true} val)])
                                                         viewers-map)))))
   :render-fn '(fn [label->val]
                 (reagent.core/with-let [!selected-label (reagent.core/atom (ffirst label->val))]
                   [:div.flex-col
                    [:div.flex.justify-center
                     [:div {:class "transition-[height]"}
                      (into
                       [:div.flex.items-center.font-sans.text-xs.mb-3
                        [:span.text-slate-500.mr-2 "View As:"]]
                       (map (fn [label]
                              [:button.px-3.py-1.font-medium.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.transition
                               {:class (if (= @!selected-label label) "bg-indigo-100 text-indigo-600" "text-slate-500")
                                :on-click #(reset! !selected-label label)}
                               label]))
                       (keys label->val))]]
                    [:div.flex.justify-center
                     [:div.relative
                      (map (fn [[label val]]
                             [:div.relative.top-0.left-0.transition-opacity.transition.duration-300.ease-in-out
                              {:class (if (= label @!selected-label) "opacity-100" "opacity-0")}
                              [nextjournal.clerk.render/inspect-presented val]])
                           label->val)]]]))})


(def tabbed-board-viewer (new-tabbed-viewer {"Board" board-viewer
                                             "Data" clerk-viewer/map-viewer}))
#_(clerk/add-viewers! [board-toggle-viewer])

^{::clerk/viewer tabbed-board-viewer}
test-board

(defn new-side-by-side-viewer [viewers-map]
  {:transform-fn (comp clerk/mark-preserve-keys
                       (clerk/update-val (fn [val]
                                           (into {} (map (fn [[k viewer]]
                                                           [k (clerk/with-viewer
                                                                viewer
                                                                {:nextjournal.clerk/auto-expand-results? true}
                                                                val)])
                                                         viewers-map)))))
   :render-fn '(fn [label->val]
                 [:div {:class ""}
                  (into
                   #_[:div.flex.flex-row.gap-4.Xw-full.items-center.justify-center]
                   [:div.grid.gap-4 {:class (str "grid-cols-" (count (keys label->val)))}]

                   (map (fn [[label val]]
                          [:div.overflow-auto [nextjournal.clerk.render/inspect-presented val]]) label->val))])})

(def side-by-side-board-viewer (new-side-by-side-viewer {"Data" clerk-viewer/map-viewer
                                                         "Board" board-viewer}))

^{::clerk/viewer side-by-side-board-viewer}
test-board


(def board-move-viewer
  {:pred (fn [maybe-move] (and (map? maybe-move)
                               (contains? maybe-move :flags)
                               (contains? maybe-move :steps)))
   :transform-fn
   (comp clerk/mark-presented (clerk/update-val
                               (fn [move]
                                 #_(tap> move)
                                 (let [steps (:steps move)
                                       board (:board (first steps))
                                       from-pos (->  (last steps) :piece :pos)
                                       to-pos  (->  (first steps) :piece :pos)
                                       captures? (:captures (first steps))]

                                   [(core/board->symbolic board) from-pos to-pos captures?]))))
   :render-fn board-render-fn})


(def tabbed-board-move-viewer (new-tabbed-viewer {"Board" board-move-viewer
                                                  "Data" clerk-viewer/map-viewer }))





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
