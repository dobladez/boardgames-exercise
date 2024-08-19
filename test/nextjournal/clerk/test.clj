;; # 👩‍🔬 Clerk Test Report
;; NOTE: Copied and modified from: https://github.com/nextjournal/clerk-test
(ns nextjournal.clerk.test
  {:nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjourna.clerk/no-cache true}
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [clojure.test :as t]
            [clojure.data :as data]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.builder-ui :as builder-ui]
            [nextjournal.clerk.analyzer :as clerk.analyzer]
            [nextjournal.clerk.viewer :as viewer]
            [nextjournal.clerk.webserver :as webserver]
            [boardgames.core]
            [boardgames.clerk-viewers :as board-viewers]))

(defn ns+var->info [ns var]
  (let [{:as m :keys [test pending skip]} (meta var)]
    (when test
      (assoc m :var var :name (symbol var) :ns ns
             :assertions []
             :status (cond pending :pending skip :skip 'else :queued)))))

(defn ns->ns-test-data
  [ns]
  {:ns ns
   :name (ns-name ns)
   :status :queued
   :file (clerk.analyzer/ns->file ns)
   :test-vars (keep (partial ns+var->info ns) (vals (ns-publics ns)))})

(defn test-paths []
  (into #{}
        (map fs/absolutize)
        (fs/glob "test" "**/*.clj")))

(defn test-nss []
  ;; TODO: allow to specify test paths
  (filter (comp (test-paths)
                #(some-> % nextjournal.clerk.analyzer/ns->file fs/absolutize))
          (all-ns)))

(defn test-plan []
  ;; inspired by kaocha.repl/test-plan
  (mapv ns->ns-test-data (test-nss)))

#_(test-plan)

(defn initial-state []
  ;; TODO: maybe populate plan in an explicit step prior to running tests
  {:test-nss (test-plan)
   :seen-ctxs #{}
   :current-test-var nil
   :summary {}})

#_(keep :name (tree-seq coll? (some-fn :test-nss :test-vars) (initial-state)))

(defonce !test-run-events (atom []))
(defonce !test-report-state (atom (initial-state)))

(defn make-test-plan! []
  (reset! !test-report-state (initial-state)))

(defn reset-state! []
  (reset! !test-run-events [])
  (reset! !test-report-state (initial-state))
  #_(when @webserver/!doc
      (clerk/recompute!)))

(defn ->assertion-data
  [current-test-var {:as event :keys [type]}]
  (assoc event :var current-test-var
               :name (symbol current-test-var)
               :status type))

(defn vec-update-if [pred f & args]
  (partial mapv (fn [item] (if-not (pred item) item (apply f item args)))))

(defn update-test-ns [state nsn f & args]
  (update state :test-nss (vec-update-if #(= nsn (:name %)) f)))

(defn update-test-var [state varn f]
  (update-test-ns state
                  (-> varn namespace symbol)
                  (fn [test-ns]
                    (update test-ns :test-vars (vec-update-if #(= varn (:name %)) f)))))

(defn update-contexts [{:as state :keys [seen-ctxs current-test-var]}]
  (let [ctxs (remove seen-ctxs t/*testing-contexts*)
        depth (count (filter seen-ctxs t/*testing-contexts*))
        ctx-items (map-indexed (fn [i c] {:type :ctx
                                          :ctx/text c
                                          :ctx/depth (+ depth i)}) (reverse ctxs))]
    (-> state
        (update :seen-ctxs into ctxs)
        (update-test-var (symbol current-test-var)
                         #(update % :assertions into ctx-items)))))

(defn update-summary [state {:as event :keys [type]}]
  (update state :summary
          (fn [m]
            (cond
              (some #{type} [:pass :error]) (update m type (fnil inc 0))
              (= :begin-test-var type) (update m :test (fnil inc 0))
              (= :fail type) (update m :fail (fnil inc 0))
              :else m))))

#_(ns-unmap *ns* 'build-test-state)
(defmulti build-test-state (fn bts-dispatch [_state {:as _event :keys [type]}] type))
(defmethod build-test-state :default [state event]
  #_(println :Unhandled event)
  state)

(defmethod build-test-state :begin-test-ns [state event]
  (update-test-ns state (ns-name (:ns event)) #(assoc % :status :executing)))

(defmethod build-test-state :begin-test-var [state {:as event :keys [var]}]
  (-> state
      (update-summary event)
      (assoc :current-test-var var)
      (update-test-var (symbol var) #(assoc % :status :executing))))

(defn update-var-assertions [{:as state :keys [current-test-var]} event]
  (-> state
      update-contexts
      (update-summary event)
      (update-test-var (symbol current-test-var) #(update % :assertions conj (->assertion-data current-test-var event)))))

(defmethod build-test-state :pass [state event] (update-var-assertions state event))

(defmethod build-test-state :fail [state event] (update-var-assertions state event))

(defmethod build-test-state :error [state event] (update-var-assertions state event))

(defn get-coll-status [coll]
  (let [statuses (map :status coll)]
    (or (some #{:error} statuses) (some #{:fail} statuses) :pass)))

(defmethod build-test-state :end-test-var [state {:keys [var]}]
  (update-test-var state (symbol var) #(assoc % :status (get-coll-status (:assertions %)))))

(defmethod build-test-state :end-test-ns [state {:keys [ns]}]
  (update-test-ns state (ns-name ns) #(assoc % :status (get-coll-status (:test-vars %)))))

(defn report [event]
  (swap! !test-run-events conj event)
  (swap! !test-report-state build-test-state event)
  (when (and @webserver/!doc
             (= (-> event :type) :end-test-ns))
    (println "report -> recompute !")
    (prn event)
    (clerk/recompute!))

  #_(when @webserver/!doc
      (clerk/recompute!)))

(comment
  (do
    (reset-state!)
    (binding [t/report report]
      (t/run-tests (the-ns 'demo.a-test)
                   (the-ns 'demo.b-test)
                   (the-ns 'demo.c-test))))

  (clerk/show! 'nextjournal.clerk.test)
  (remove-all-methods build-test-state)
  (ns-unmap *ns* 'build-test-state)
  (test-plan))

(defn bg-class [status]
  (case status
    :pass "bg-green-100"
    :fail "bg-red-50"
    :error "bg-red-50"
    "bg-slate-100"))

(defn status-light [color & [{:keys [size] :or {size 14}}]]
  [:div.rounded-full.border
   {:class (str "bg-" color "-400 border-" color "-700")
    :style {:box-shadow "inset 0 1px 3px rgba(255,255,255,.6)"
            :width size :height size}}])

(defn status->icon [status]
  (case status
    :executing [:div (builder-ui/spinner-svg)]
    :queued (status-light "slate")
    :pending (status-light "amber")
    :skip (status-light "slate")
    :pass (status-light "green")
    :fail (status-light "red")
    :error (builder-ui/error-svg)))

(defn status->text [status]
  (case status
    :queued "Queued"
    :pending "Pending"
    :skip "Skipped"
    :executing "Running"
    :pass "Passed"
    :fail "Failed"
    :error "Errored"))

(defn assertion-badge [{:as ass :keys [status name line expected actual exception message] :ctx/keys [text depth]}]
  (if text
    [:<>
     ;; force contexts to a new line
     [:div.h-0.basis-full]
     [:div.text-slate-500.my-1.mr-2 {:class (when (< 0 depth) (str "pl-" (* 4 depth)))} text]]
    (case status
      :pass [:div.ml-1.my-1.text-green-600 (builder-ui/checkmark-svg {:size 20})]
      :fail [:div.flex.flex-col.p-1.my-2.w-full
             [:div.w-fit
              [:em.text-red-600.font-medium (str name ":" line)]
              (when (and expected actual)
                [:table.not-prose.w-full
                 [:tbody
                  [:tr.hover:bg-red-100.leading-tight.border-none
                   [:td.text-right.font-medium "expected:"]
                   [:td.text-left (viewer/code (pr-str expected))]]
                  [:tr.hover:bg-red-100.leading-tight.border-none
                   [:td.text-right.font-medium "actual:"]
                   [:td.text-left (viewer/code (pr-str actual))]]

                  #_[:tr
                     [:td
                    ;; TODO: robust predicate for boards:
                      [:pre "Board predicate: "
                       [:pre (pr-str (seqable? actual))]]]]

                  (when (and (seqable? actual)
                             (seqable? (second actual)) ;; first is the equal sign
                             (vector? (second (second actual))) ;; <- board pred here
                             (vector? (first (second (second actual)))))
                    (list
                     #_[:tr.hover:bg-red-100.leading-tight.border-none
                        [:td.text-right.font-medium "expected boards:"]
                        [:td.text-left.flex.flex-row.gap-1

                         (->> (second (second actual))
                              (map boardgames.core/symbolic->board)
                              (map #(clerk/with-viewer board-viewers/board-viewer %)))]]

                     #_[:tr.hover:bg-red-100.leading-tight.border-none
                        [:td.text-right.font-medium "actual boards:"]
                        [:td.text-left.flex.flex-row.gap-1

                         (->> (nth (second actual) 2)
                              (map boardgames.core/symbolic->board)
                              (map #(clerk/with-viewer board-viewers/board-viewer %)))]]

                     (let [expected-boards (second (second actual))
                           actual-boards (nth (second actual) 2)
                           [diff-expected diff-actual diff-correct] (map vec (data/diff (set expected-boards) (set actual-boards)))]
                       #_(for [m [{:label "Correct: " :boards diff-correct}
                                  {:label "Expected (missing):" :boards diff-expected}
                                  {:label "Not expected:" :boards diff-actual}]]
                           [:tr.hover:bg-red-100.leading-tight.border-none
                            #_[:td.text-right.font-medium (:label m)]
                            [:td.text-left.flex.flex-row.gap-1
                             (->> (:boards m)
                                  (map boardgames.core/symbolic->board)
                                  (map #(clerk/with-viewer board-viewers/board-viewer %)))]])

                       (for [m [{:label "Correct: " :boards diff-correct}
                                {:label "Expected (missing):" :boards diff-expected}
                                {:label "Not expected:" :boards diff-actual}]
                             :when (seq (:boards m))]

                         [:tr.hover:bg-red-100.leading-tight.border-none
                          [:td.text-right.font-medium.text-black (:label m)]
                          [:td.text-left.flex.flex-row.gap-1
                           (->> (:boards m)
                                (map boardgames.core/symbolic->board)
                                (map #(clerk/with-viewer board-viewers/board-viewer %)))]]))))]])]

             (when message
               [:em.text-orange-500 message])]
      :error [:div.p-1.my-2 {:style {:widht "100%"}}
              [:em.text-red-600.font-medium (str name ":" line)]
              [:div.mt-2.rounded-md.shadow-lg.border.border-gray-300.overflow-scroll
               {:style {:height "200px"}} (viewer/present exception)]])))

(defn test-var-badge [{:keys [name status line assertions]}]
  (let [collapsible? (seq assertions)]
    [(if collapsible? :details :div)
     (cond-> {:class (str "mb-2 rounded-md border border-slate-300 px-3 py-2 font-sans shadow " (bg-class status))}
       (and collapsible? (= status :executing))
       (assoc :open true))
     [(if collapsible? :summary.cursor-pointer :div.pl-3)
      [:span.mr-2.inline-block (status->icon status)]
      [:span.inline-block.text-sm.mr-1 (status->text status)]
      [:span.inline-block.text-sm.font-medium.leading-none (str name ":" line)]]
     (when (seq assertions)
       (into [:div.flex.flex-wrap.mt-2.py-2.border-t-2] (map assertion-badge) assertions))]))

(defn test-ns-badge [{:keys [status file test-vars]}]
  [:details.mb-2.rounded-md.border.border-slate-300.font-sans.shadow.px-4.py-3
   (cond-> {:class (bg-class status)} (= status :executing) (assoc :open true))
   [:summary
    [:span.mr-2.inline-block (status->icon status)]
    [:span.text-sm.mr-1 (status->text status)]
    [:span.text-sm.font-semibold.leading-none.truncate file]]
   (into [:div.ml-2.mt-2] (map test-var-badge) test-vars)])

(def test-ns-viewer {:transform-fn (viewer/update-val (comp viewer/html test-ns-badge))})

(def test-suite-viewer
  {:transform-fn (comp viewer/mark-preserve-keys
                       (viewer/update-val
                        (fn [state]
                          (-> state
                              (update :test-nss (partial map (partial viewer/with-viewer test-ns-viewer)))
                              (update :summary #(when (seq %)
                                                  (clerk/with-viewer clerk/table
                                                    (into [] (map (juxt (comp str/capitalize name first) second)) %))))))))
   :render-fn '(fn [{:keys [test-nss summary]} opts]
                 [:div
                  (when (:nextjournal/value summary)
                    [nextjournal.clerk.render/inspect summary])
                  (if-some [xs (seq (:nextjournal/value test-nss))]
                    (into [:div.flex.flex-col.pt-2] (nextjournal.clerk.render/inspect-children opts) xs)
                    [:h5 [:em.slate-100 "Waiting for tests to run..."]])])})

{::clerk/visibility {:code :hide :result :show}}
(clerk/with-viewer test-suite-viewer
  @!test-report-state)


{::clerk/visibility {:code :hide :result :hide}}

(defn status-report
  "repl helper for summarizing test results"
  [state]
  (keep (fn [{:keys [name assertions]}]
          (when (and name assertions)
            {:name name :status (get-coll-status assertions)}))
        (tree-seq map?
                  (some-fn :assertions :test-vars :test-nss)
                  state)))

(comment
  (reset-state!))
