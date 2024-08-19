
(ns scratchpad
  {:nextjournal.clerk/toc true}
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk.viewer]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            ;;            [boardgames.checkers :as checkers]
            [boardgames.utils :as utils]
            [flow-storm.clerk :as clerk-storm]))

{:nextjournal.clerk/visibility {:code :show :result :show}
 :nextjournal.clerk/auto-expand-results? true}

;; ## Scratchpad to try ideas/componets


(comment
  ^{::clerk/visibility {:code :hide :result :hide}} (clerk/reset-viewers! clerk/default-viewers)
  )

^{::clerk/visibility {:code :show :result :hide}}
(clerk/add-viewers! [viewers/board-viewer])


#_

;; ### Basic board
^{::clerk/auto-expand-results? true}
(core/symbolic->board [[:r :n :b :q :k :b :n :r]
                       [:p :p :p :p :p :p :p :p]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:- :- :- :- :- :- :- :-]
                       [:P :P :P :P :P :P :P :P]
                       [:R :N :B :Q :K :B :N :R]])

#_
;; ### nested inside vec:
[(core/symbolic->board [[:r :n :b :q :k :b :n :r]
                        [:p :p :p :p :p :p :p :p]
                        [:- :- :- :- :- :- :- :-]
                        [:- :- :- :- :- :- :- :-]
                        [:- :- :- :- :- :- :- :-]
                        [:- :- :- :- :- :- :- :-]
                        [:P :P :P :P :P :P :P :P]
                        [:R :N :B :Q :K :B :N :R]])]



;; ### Game: get possible moves
(def game-1 (core/start-game chess/chess-game))

game-1


#_
^{:nextjournal.clerk/auto-expand-results? true}
(first (map (partial clerk/with-viewer viewers/tabbed-board-move-viewer {})
            (sort-by #(-> % :steps last :piece :pos)
                     (core/possible-pmoves game-1))))

;; ### possible moves
#_
^{:nextjournal.clerk/auto-expand-results? true}
(clerk/row
 (sort-by #(-> % :steps last :piece :pos)
          (core/possible-pmoves game-1)))

;; ## Clerk storm protype

#_^{::clerk/viewer clerk-storm/trace-code-viewer}
(clerk-storm/show-trace {:include-fn-names [#"possible-pmoves" #"candidate-pmoves\*?" #"expand-pmove"]}
    (core/possible-pmoves game-1))



^{::clerk/viewer clerk-storm/timeline-stepper-viewer ::clerk/width :full }
(clerk-storm/show-trace {:include-fn-names [#"possible-pmoves"  #"candidate-pmoves\*?" #"expand-pmove"] }
  (core/possible-pmoves game-1))


#_#_
(defn factorial-using-recur [n]
  (loop [current n
         next (dec current)
         total 1]
    (if (> current 1)
      (recur next (dec next) (* total current))
      total)))

^{::clerk/viewer clerk-storm/timeline-stepper-viewer ::clerk/width :full }
(clerk-storm/show-trace {:include-fn-names [#"fib"] }
  (factorial-using-recur 8))


#_#_(defn fib [n]
      (->> [0 1]
           (iterate (fn [[a b]] [b (+ a b)]))
           (map first)
           (take n)
           vec))

^{::clerk/viewer clerk-storm/timeline-stepper-viewer ::clerk/width :full }
(clerk-storm/show-trace {:include-fn-names [#"fib"] }
  (fib 8))

#_#_
(defn factorial-using-reduce [n]
  (reduce * (range 1 (inc n))))

^{::clerk/viewer clerk-storm/timeline-stepper-viewer ::clerk/width :full }
(clerk-storm/show-trace {:include-fn-names [#"fib"] }
  (factorial-using-reduce 8))


#_#_
(defn factorial-using-apply-iterate [n]
  (apply * (take n (iterate inc 1))))

^{::clerk/viewer clerk-storm/timeline-stepper-viewer ::clerk/width :full }
(clerk-storm/show-trace {:include-fn-names [#"fib"] }
  (factorial-using-apply-iterate 2))

#_(comment

    ;; ## Playing with nesteding custom Clerk viewers

    #_^{::clerk/visibility {:code :show :result :hide}}
    (clerk/add-viewers! [viewers/board-viewer])

    #_^{::clerk/viewer clerk.viewer/inspect-wrapped-values ::clerk/auto-expand-results? true}
    (clerk.viewer/present 1))








#_#_#_#_
  (def test-viewer-inner
    {:name `test-viewer-inner
     :pred (fn [maybe] (and (map? maybe) (contains? maybe :test-1)))
     :transform-fn (comp clerk/mark-presented
                         (clerk/update-val
                          (fn [v]
                            (clerk/html [:div.inline-flex.gap-2
                                         [:strong "map with `:test-1` viewer"]
                                         [:pre (prn-str v)]]))))})
  (def test-viewer
    {:name `test-viewer
     :pred (fn [maybe] (and (map? maybe) (contains? maybe :test)))
     :transform-fn (fn [wrapped]
                     (-> wrapped
                         clerk/mark-preserve-keys
                         clerk/mark-presented
                         (update :nextjournal/value
                                 (fn [v]
                                   {:extra-data [1 2 3 "four"]
                                    :value v}))))
     :render-fn '(fn [{:keys [extra-data value presented-value]}]
                   (let []
                     [:div.border.border-black.px-1.py-2
                      [:h3 "map with `:test` viewer"]
                      [:div
                       [:h4 "value:"]
                       [nextjournal.clerk.render/inspect value]]]))})

(clerk/add-viewers! [test-viewer test-viewer-inner viewers/board-viewer])

(def test-value {:test true
                 :a-number 4
                 :a-string "testing"
                 :a-vector [1 2 3 "four"]
                 :a-list '(1 2 3 "four")
                 :a-map {:a 1 :b 2}
                 :a-nested-value-a {:test-1 "value-A"}
                 :a-nested-value-b {:test-1 "value-B"}
                 :a-board (core/symbolic->board [[:r :n :b :q :k :b :n :r]
                                                 [:p :p :p :p :p :p :p :p]
                                                 [:- :- :- :- :- :- :- :-]
                                                 [:- :- :- :- :- :- :- :-]
                                                 [:- :- :- :- :- :- :- :-]
                                                 [:- :- :- :- :- :- :- :-]
                                                 [:P :P :P :P :P :P :P :P]
                                                 [:R :N :B :Q :K :B :N :R]])})

#_
(clerk/with-viewers (concat [test-viewer-inner test-viewer] clerk/default-viewers )
  test-value)

#_
(comment
  (clerk/reset-viewers! clerk/default-viewers)

  "ideas on how to show the stack"

;; -> (candidate-moves game-state) ;; <-- form as it appears on parentq
  ;; --> (candidate-moves* game-state) ;; <-- form as it appears on parentq

)
