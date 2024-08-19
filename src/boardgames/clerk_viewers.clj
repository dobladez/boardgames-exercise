;; # Game board viewer for Clerk
^{:nextjournal.clerk/visibility {:code :hide}}
(ns boardgames.clerk-viewers
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk.viewer]
            [boardgames.core :as core]
            [boardgames.utils :as utils :refer [TAP>]]))

{::clerk/visibility {:code :show :result :hide}
 ::clerk/auto-expand-results? true
 ::clerk/budget 1000}


;; ## Main render function for a board
;; It takes board structure and returns the HTML to render it.
;; Accepts optional `from` and `to` positions, and list captured pieces to highlight them on the board.
;; We'll reuse this render function to create viewers for boards and for moves.
(def board-render-fn

  '(fn [[board from-pos to-pos captures] {:as opts :keys [path viewer !expanded-at] :or {path []}}]
     (let [collapsible? (> (count path) 4)
           expanded? (or (not collapsible?) (get @!expanded-at path))
           row-count (if expanded? (count board) 1)
           col-count (count (first board))
           capture-pos (:pos (first captures))
           piece-to-icon '{r ♜ n ♞ b ♝ q ♛ k ♚ p ♟ R ♜ N ♞ B ♝ Q ♛ K ♚ P ♟ o "⏺" O "⏺"}
           captures-icon (some-> captures first :type name symbol piece-to-icon)]
       [:div {:class "inline-flex"}
        #_[:pre (str "path " (prn-str path))]
        (when collapsible? [nextjournal.clerk.render/expand-button !expanded-at "" path])
        [:div#board-viewer.flex.flex-col.items-start.pb-1 ;; note: this padding avoids a vertical scrollbar

        ;; [:h4 (str "@!expanded-at " (prn-str @!expanded-at) )]
        ;; [:h4 (str "path " (prn-str path) )]
        ;; [:h4 (str "(get @!expanded-at path)"  (prn-str (get @!expanded-at path))) ]

         [:div.inline-grid.w-fit.g-0 {:style {:grid-template-columns (str "repeat(" col-count ", 1fr)")
                                              :grid-template-rows (str "repeat(" row-count ", 1fr)")
                                              :font-size "36px"
                                              :border "2px solid gray"}}
          (let [piece-style-by-player {0 "-1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 1px 1px 0 #000"}
                square-style-by-player {0 "text-slate-200" 1 "text-black"}] ;; 🨾♙
            (->> (if expanded? board (take 1 (reverse board)))
                 (map-indexed
                  (fn [inverse-row-idx row]
                    (->> row
                         (map-indexed
                          (fn [col-idx square*]
                            (let [square square* #_(str square*)
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
                                                                    (when capture-pos (str " capturing " (some-> captures first :type))))])]])))


;; ## Define a Clerk viewer for boards
;; It basically converts the board model representation to a symbolic one for rendering but the `render-fn` defined above
(def board-viewer
  {:name `board-viewer
   :pred (fn [maybe-board]
           (let [b (or (= (-> maybe-board meta :tag) :boardgames)
                       (and (map? maybe-board)
                            (contains? maybe-board :col-n)
                            (contains? maybe-board :row-n)
                            (contains? maybe-board :pieces)))]
             #_(when b (do (tap> maybe-board)
                         (tap> b)
                         (println maybe-board)
                         ))
             b))
   :transform-fn
   (comp clerk/mark-presented (clerk/update-val
                               (fn [board]
                                 (let [board (core/board->symbolic board)
                                       from-pos nil
                                       to-pos nil]
                                   [board from-pos to-pos]))))
   :render-fn board-render-fn})

#_^{:nextjournal.clerk/visibility {:result :hide}}
(clerk/add-viewers! [board-viewer])

;; Let's create a board for to test our viewers:
^{::clerk/viewer board-viewer
  ::clerk/visibility {:code :show :result :show}}
(def test-board
  (core/symbolic->board [[:r :n :b :q :k :b :n :r]
                         [:p :p :p :p :p :p :p :p]
                         [:- :- :- :- :- :- :- :-]
                         [:- :- :- :- :- :- :- :-]
                         [:- :- :- :- :- :- :- :-]
                         [:- :- :- :- :- :- :- :-]
                         [:P :P :P :P :P :P :P :P]
                         [:R :N :B :Q :K :B :N :R]]))

;;
;; ## Define a Clerk viewer for moves
;; This viewer basically takes the board from the last step of the move structure, and renders it highlighting the `from` and `to` squares
;; plus an indication if a piece was captured.
(def board-move-viewer
  {:pred (fn [maybe-move] (and (map? maybe-move) (contains? maybe-move :flags) (contains? maybe-move :steps)))
   :transform-fn
   (comp clerk/mark-presented (clerk/update-val
                               (fn [move]
                                 (let [steps (:steps move)
                                       board (:board (first steps))
                                       from-pos (->  (last steps) :piece :pos)
                                       to-pos  (->  (first steps) :piece :pos)
                                       captures? (:captures (first steps))]

                                   [(core/board->symbolic board) from-pos to-pos captures?]))))
   :render-fn board-render-fn})


;; ## Tabbed viewer
;;
;; Here is a generic way to create "tabbed" viewers: that is, viewers that show
;; the same value with different nested viewers for the user to choose from.
(defn new-tabbed-viewer
  ([viewers-map] (new-tabbed-viewer viewers-map nil))
  ([viewers-map pred-fn]
   {#_#_:pred pred-fn
    :transform-fn (comp clerk/mark-preserve-keys
                        (clerk/update-val (fn [val]
                                            (into {} (map (fn [[k viewer]]
                                                            [k (clerk/with-viewer viewer
                                                                 {#_#_::clerk/render-evaluator :cherry
                                                                  :nextjournal.clerk/auto-expand-results? true}
                                                                 val)])
                                                          viewers-map)))))
    :render-fn '(fn [label->val]
                  (reagent.core/with-let [!selected-label (reagent.core/atom (ffirst label->val))]
                    [:div.pb-1 ;; note: this padding avoids a vertical scrollbar
                     (into
                      [:div.flex.items-center.font-sans.text-xs.mb-3
                       [:span.text-slate-500.mr-2 "View As:"]]
                      (map (fn [label]
                             [:button.px-3.py-1.font-medium.hover:bg-indigo-50.rounded-full.hover:text-indigo-600
                              {:class (if (= @!selected-label label) "bg-indigo-100 text-indigo-600" "text-slate-500")
                               :on-click #(reset! !selected-label label)}
                              label]))
                      (keys label->val))
                     [:div
                      [nextjournal.clerk.render/inspect-presented (get label->val @!selected-label)]]]))}))

;; We'll use this to define board and move viewers that display both a code view
;; and a graphical board view of the value.
(def tabbed-board-viewer (new-tabbed-viewer {"Board" board-viewer
                                             "Data" clerk.viewer/map-viewer}))

;; Let's try it with our board:
^{::clerk/viewer tabbed-board-viewer
  ::clerk/visibility {:code :show :result :show}}
test-board

;; And also define tabbed viewer for moves:
(def tabbed-board-move-viewer (new-tabbed-viewer {"Board" board-move-viewer
                                                  "Data" clerk.viewer/map-viewer}

                                                 #_(fn [maybe-move]
                                                   (and (map? maybe-move)
                                                        (contains? maybe-move :flags)
                                                        (contains? maybe-move :steps)))))

#_(clerk/add-viewers! [board-toggle-viewer])


;; ## Side-by-side viewer
;;
;; Here is a generic way to create side-by-side viewers: that is, viewers that show
;; the same value twice, in two columns, using a different viewer on each side.
(defn new-side-by-side-viewer
  ([viewers-map] (new-side-by-side-viewer viewers-map nil))
  ([viewers-map pred-fn]

   {:pred pred-fn
    :transform-fn (comp clerk/mark-preserve-keys
                        (clerk/update-val (fn [val]
                                            (into {} (map (fn [[k viewer]]
                                                            [k (clerk/with-viewer
                                                                 viewer
                                                                 {:nextjournal.clerk/auto-expand-results? true}
                                                                 val)])
                                                          viewers-map)))))
    :render-fn '(fn [label->val]
                  [:div
                   (into
                    #_[:div.flex.flex-row.gap-4.Xw-full.items-center.justify-center]
                    [:div.grid.gap-4 {:class (str "grid-cols-" (count (keys label->val)))}]

                    (map (fn [[label val]]
                           [:div.overflow-x-auto.max-h-72.pb-1 ;; note: the padding avoids a vertical scrollbar
                            ^{::clerk/auto-expand-results? true}
                            [nextjournal.clerk.render/inspect-presented ^{::clerk/auto-expand-results? true} val]]) label->val))])}))

;; We'll use this to define board and move viewers that display both a code view
;; and a graphical board view of the value.
(def side-by-side-board-viewer (new-side-by-side-viewer {"Data" clerk.viewer/map-viewer
                                                         "Board" board-viewer}))

^{::clerk/viewer side-by-side-board-viewer
  ::clerk/visibility {:code :show :result :show}}
test-board


#_(clerk/add-viewers! [tabbed-board-move-viewer])

;; And a side-by-side viewer for moves
(def side-by-side-move-viewer (new-side-by-side-viewer {"Data" clerk.viewer/code-viewer
                                                        "Board" board-move-viewer

                                                        }

                                                       (fn [maybe-move]
                                                         (and (map? maybe-move)
                                                              (contains? maybe-move :flags)
                                                              (contains? maybe-move :steps)))))
