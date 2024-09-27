
^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns scratchpad2
  {:nextjournal.clerk/toc true
   :nextjournal.clerk/visibility {:code :show :result :show}}
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk.viewer] ))


;; ## Scratchpad

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(def test-viewer-inner
  {:name `test-viewer-inner
   :pred (fn [maybe] (and (map? maybe) (contains? maybe :test-1)))
   :transform-fn (comp clerk/mark-presented
                       (clerk/update-val
                        (fn [v]
                          (clerk/html [:div.inline-flex.gap-2
                                       [:strong "test-viewer-inner:"]
                                       [:pre (prn-str v)]]))))})


^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(def test-viewer
    {:name `test-viewer
     :pred (fn [maybe] (and (map? maybe) (contains? maybe :test)))
     :transform-fn (fn [wrapped]
                     (-> wrapped
                         clerk/mark-preserve-keys
                         clerk/mark-presented
                         (update :nextjournal/value
                                 (fn [value]
                                   {:extra-data [1 2 3 "four"]
                                    :value value }))))
     :render-fn '(fn [{:keys [extra-data value presented-value]}]
                   (let []
                     [:div.border.border-black.px-1.py-2
                      [:h3 "test-viewer:"]
                      [:div
                       [:h4 "value:"]
                       [nextjournal.clerk.render/inspect value]]]))})





^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(def test-explicit-presenting-viewer
    {:name `test-explicit-presenting-viewer
     :pred (fn [maybe] (and (map? maybe) (contains? maybe :explicit-present)))
     :transform-fn (fn [wrapped]
                     (-> wrapped
                         clerk/mark-preserve-keys
                         clerk/mark-presented
                         (update :nextjournal/value
                                 (fn [value]
                                   {:extra-data (clerk.viewer/present [1 2 3 "four"])
                                    :value (clerk.viewer/present (dissoc value :explicit-present)) }))))

     :render-fn '(fn [{:keys [extra-data value presented-value]}]
                   (let []
                     [:div.border.border-black.px-1.py-2
                      [:h3 "test-explicit-presenting-viewer:"]
                      [:div
                       [:h4 "value:"]
                       [nextjournal.clerk.render/inspect-presented value]]]))})

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(clerk/add-viewers! [test-viewer test-viewer-inner test-explicit-presenting-viewer])



{:test true
 :a-number 4
 :a-string "testing"
 :a-nested-value-a {:test-1 "value-A"} ;; <-- I want `test-viewer-inner` to render this
}




{:explicit-present true
 :a-number 4
 :a-string "testing"
 :a-nested-value-a {:test-1 "value-A"}
}





^{::clerk/visibility {:code :show :result :show}
  ::clerk/auto-expand-results? true
  ::clerk/viewers (concat [{:pred #(= 1 %)
                            :transform-fn (fn [x] (update x :nextjournal/value (fn xyz [_] (keys x)) ))}]
                          clerk/default-viewers)}
[{ :a 1}]
