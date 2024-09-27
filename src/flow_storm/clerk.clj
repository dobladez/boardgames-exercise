;; # Code Viewers with integration with Flow Storm tracing
^{:nextjournal.clerk/visibility {:code :hide}}
(ns flow-storm.clerk
  (:require [boardgames.utils :refer [TAP>]]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [flow-storm.form-pprinter :as form-pprinter]
            [flow-storm.runtime.indexes.api :as index-api]
            [flow-storm.runtime.debuggers-api :as debuggers-api]
            [flow-storm.utils :as utils]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk.viewer]))



;; ================================================================================
;; ================================================================================
;; ================================================================================
;; Flowstorm "static" callstack viewer


(def trace-code-render-fn
  '(fn [tl] (let [] [nextjournal.clerk.render/inspect-presented tl])))

(defn storm-form->str [form c]
  (let [tokens (form-pprinter/pprint-tokens form)]
    (reduce str
            (for [{:keys [kind text coord]} tokens]
              (let [txt (case kind
                          :sp " "
                          :nl "\n"
                          :text (if (= coord c)
                                  (utils/colored-string text :red)
                                  text))]
                txt)))))
#_(defn- demunge-fn
  [fn-object]
  (let [dem-fn (clojure.repl/demunge (str fn-object))
        pretty (second (re-find #"(.*?\/.*?)[\-\-|@].*" dem-fn))]
    (if pretty pretty dem-fn)))

(def fn-args-viewer
  {:transform-fn (comp clerk/mark-preserve-keys
                       (clerk/update-val (fn [[arg-names arg-values]]
                                           (into {} (map vector
                                                         arg-names
                                                         arg-values)))))
   :render-fn '(fn [label->val]
                 (reagent.core/with-let [!selected-label (reagent.core/atom (ffirst label->val))]
                   [:div.pb-1 ;; note: this padding avoids a vertical scrollbar
                    [:div.flex
                     (into
                      [:div.flex.items-center.font-sans.text-xs.mb-3
                       #_[:span.text-slate-500.mr-2 "View As:"]]
                      (map (fn [label]
                             [:button.px-3.py-1.font-mono.font-medium.hover:bg-indigo-50.rounded-full.hover:text-indigo-600
                              {:class (if (= @!selected-label label) "bg-indigo-100 text-indigo-600" "text-slate-500")
                               :on-click #(reset! !selected-label label)}
                              label]))
                      (keys label->val))
                     [:span " )"]]
                    [:div
                     [nextjournal.clerk.render/inspect-presented (get label->val @!selected-label)]]]))})

(defn- fn-calls [timeline opts]
  (->> timeline
       (map index-api/as-immutable)
       (filter #(or (= :fn-call (:type %)) #_(= :fn-return (:type %))))
       (filter (fn [e] (or (not (:fn-name e))
                           (empty? (:include-fn-names opts))
                           (some #(re-matches % (:fn-name e)) (:include-fn-names opts)))))
       ;; TODO: lookup and add back-refs to this fn idx ?
       ))

(defn- enrich-with-form [thread-id tl-entry]
  (let [{:keys [form-id]} (index-api/frame-data 0 thread-id (:fn-call-idx tl-entry) {})
        {:keys [form/form]} (index-api/get-form form-id)]
    (assoc tl-entry :form form :_form (storm-form->str form :NEVER))))

(def trace-code-viewer
  {:name `trace-code-render
   #_#_:pred (fn [x] x)

   :transform-fn (clerk/update-val #_clerk/mark-presented
                  (fn [{:keys [thread-id timeline opts] :as value}]
                    (let [opts (merge opts (select-keys [:include-fn-names] (meta value)))
                          tl (->> (fn-calls timeline opts)
                                  (map (partial enrich-with-form thread-id))
                                  (take 8))
                          #_(->> timeline
                                 (map index-api/as-immutable)
                                 (filter #(or (= :fn-call (:type %)) #_(= :fn-return (:type %))))
                                 (filter (fn [e] (or (not (:fn-name e))
                                                     (empty? (:include-fn-names opts))
                                                     (some #(re-matches % (:fn-name e)) (:include-fn-names opts)))))
                                 #_(take 10)
                                 (map (fn [e]
                                        (let [{:keys [form-id]} (index-api/frame-data flow-id thread-id (:fn-call-idx e) {})
                                              {:keys [form/form]} (index-api/get-form form-id)]
                                          (assoc e :form form :_form (storm-form->str form :NEVER)))))
                                 #_(map #(select-keys % [:idx :type :fn-name :fn-call-idx :form :_form :fn-args]))

                                 #_(map #(update % :fn-args
                                                 (partial walk/postwalk (fn [val] (if (fn? val)
                                                                                    (demunge-fn val)
                                                                                    val))))))]
                      (clerk/html {::clerk/width :wide}
                                  (for [entry (->> tl #_reverse #_(filter #(= :fn-call (:type %))))]
                                    [:div.flex.flex-col

                                     #_(for [a (:fn-args entry)]

                                         #_[:div [nextjournal.clerk.render/inspect-presented a]]
                                         #_[:div [nextjournal.clerk.render/inspect a]]
                                         #_[:pre (prn-str a)])
                                     ;; [:pre (prn-str (class (:form entry)))]
                                     ;; [:pre (prn-str (:form entry))]

                                     #_(clerk/code (->> (:form entry) (drop 1) (take-while (complement (some-fn string? vector?)))))
                                     (let [args-vector (first (filter vector? (:form entry)))]
                                       [:div.flex.flex-row.gap-2
#_                                        (clerk/code (list (symbol (:fn-name entry)) args-vector))
                                        (clerk/code (str "(" (symbol (:fn-name entry))))
                                        [:div (clerk/with-viewer fn-args-viewer [args-vector (:fn-args entry)])] ])])))))

   #_[:div.pl-12 (clerk/row (:fn-args entry))]
   :render-fn trace-code-render-fn})







;; ================================================================================
;; ================================================================================
;; ================================================================================
;; Flowstorm Stepper






(defn stepper-coord-class [form-id coord]
  (format "%d-coord-%s" form-id (str/join "-" coord)))

(defn stepper-token-class [text]
  (cond
    (contains? (ns-publics 'clojure.core) (symbol text)) "cmt-keyword"
    (contains? #{"def" "if" "do" "loop" "recur" "throw"} text) "cmt-keyword"
    (clojure.string/starts-with? text ":") "cmt-atom"
    (clojure.string/starts-with? text "\"") "cmt-string"
    :else ""))



(defn- new-index [] {:values []
                     :values-idx {}})

(defn- add-to-index! [!values-index trans-fn value]
  (let [{:keys [values values-idx]} @!values-index
        found? (contains? values-idx value)
        idx (if found? (get values-idx value)
                (count values))]
    (when-not found?
      (swap! !values-index #(-> %
                                (update-in [:values-idx] assoc value idx)
                                (update-in [:values] assoc idx (trans-fn value)))))
    idx))

(defn serialize-timeline [viewers imm-timeline]

  (let [!values-index (atom (new-index))
        _add-to-index! (partial add-to-index! !values-index #(clerk.viewer/present
                                                              (clerk/with-viewers viewers %)))
#_#_        _add-to-index! (partial add-to-index! !values-index clerk.viewer/present)
        !callstack-index (atom (new-index))
        _add-to-callstack-index! (partial add-to-index! !callstack-index identity)
        !callstack-entries-index (atom (new-index))
        _add-to-callstack-entries-index! (partial add-to-index! !callstack-entries-index identity)
        timeline (->> imm-timeline
                      ;; collect and replace all values by their position on an index to avoid duplicates
                      (mapv (fn [imm-entry]
                              (case (:type imm-entry)
                                :fn-call   (update imm-entry :fn-args #(mapv _add-to-index! %) #_print-val)
                                :fn-return (update imm-entry :result  _add-to-index!)
                                :fn-unwind (update imm-entry :throwable pr-str)
                                :bind      (update imm-entry :value _add-to-index!)
                                :expr      (update imm-entry :result _add-to-index!)
                                imm-entry)))

                      (reduce (fn [{:keys [timeline callstack]} entry]
                                (let [callstack (case (:type entry)
                                                  :fn-call (conj callstack (_add-to-callstack-entries-index!
                                                                            (let [ret-entry2 (get imm-timeline (inc (:ret-idx entry)))
                                                                                  ret-entry1 (get imm-timeline (:ret-idx entry))]
                                                                              {:fn-name (:fn-name entry)
                                                                               :form-id (:form-id entry)
                                                                               #_#_:return-form-id (:form-id ret-entry)
                                                                               #_#_:return-coord (:coord ret-entry)
                                                                               #_#_:ret-entry1 ret-entry1
                                                                               #_#_:ret-entry2 ret-entry2
                                                                               :ret-form-id (:form-id (get imm-timeline (:fn-call-idx ret-entry2)))
                                                                               :ret-coord (:coord ret-entry2)

                                                                               #_#_:return-coord (:coord ret-entry)

                                                                               :ret-idx (:ret-idx entry)})))
                                                  :fn-return (pop callstack)
                                                  callstack)
                                      entry (assoc entry :callstack (_add-to-callstack-index! callstack))]

                                  {:timeline (conj timeline entry)
                                   :callstack callstack}))

                              {:timeline [] :callstack []})
                      :timeline)]

    {:timeline timeline
     :presented-values (:values @!values-index)
     :callstacks (:values @!callstack-index)
     :callstack-entries (:values @!callstack-entries-index)}))



(comment (form-pprinter/pprint-tokens '(fname [arg1 arg2] 123)) )

(defn pre-render-form [form-id form]
  (let [tokens (form-pprinter/pprint-tokens form)]
    (into
     [:div.pl-2.py-2.font-mono.Xcm-content.whitespace-pre]
     (for [{:keys [kind text coord]} tokens]
       (case kind
         :sp " "
         :nl "\n"
         :text (if coord
                 [:span {:class (str (stepper-coord-class form-id coord) " " (stepper-token-class text))} text]
                 [:span {:class (stepper-token-class text)} text]))))))

(defn pre-render-forms [forms-map]
  (into {} (map (fn [[form-id form]]
                  (let [tokens (form-pprinter/pprint-tokens form)]
                    [form-id (into
                              [:div.pl-2.py-2.font-mono.Xcm-content.whitespace-pre]
                              (for [{:keys [kind text coord]} tokens]
                                (case kind
                                  :sp " "
                                  :nl "\n"
                                  :text (if coord
                                          [:span {:class (str/trim (str (stepper-coord-class form-id coord) " " (stepper-token-class text)))} text]
                                          [:span {:class (stepper-token-class text)} text]))))]))
                forms-map)))

(defn get-form-id [imm-timeline tl-e]
  (if (= :fn-call (:type tl-e))
    (:form-id tl-e)
    (->> tl-e :fn-call-idx (get imm-timeline) :form-id)))

(def timeline-stepper-viewer
  {:name `timeline-stepper-viewer
   :pred (fn [value]  (and (map? value)
                           (contains? value :thread-id)
                           (contains? value :timeline)))
   :transform-fn (comp
                  clerk/mark-preserve-keys
                  clerk/mark-presented
                  (fn [wrapped-value]
                    (let [viewers (:nextjournal/viewers wrapped-value)
                          value (:nextjournal/value wrapped-value)
                          {:keys [thread-id timeline code-form opts]} value]

                      (let [forms (reduce (fn [forms {:keys [form-id]}]
                                            (let [{:keys [form/form]} (index-api/get-form form-id)]
                                              (assoc forms form-id form)))
                                          {}
                                          (fn-calls timeline {}))

                            new-value {:pre-rendered-forms (pre-render-forms forms)
                                       :code-form (pre-render-form 0 code-form)
                                       :ser-timeline (->> timeline (mapv index-api/as-immutable) (serialize-timeline viewers))}]

                        #_(prn (last (-> ret :ser-timeline :presented-values)))
                        #_(spit "/tmp/ser-timeline1.edn" (with-out-str (pp/pprint (->> timeline (mapv index-api/as-immutable)))))
                        #_(spit "/tmp/ser-timeline2.edn" (with-out-str (pp/pprint (-> ret :ser-timeline))))

                        (assoc wrapped-value :nextjournal/value new-value)))))

   :render-fn '(fn [{:keys [pre-rendered-forms ser-timeline code-form]}]
                 (reagent.core/with-let [!current-step (reagent.core/atom 0)
                                         !forms-el (reagent.core/atom nil)]
                   (let [{:keys [timeline presented-values callstacks callstack-entries]} ser-timeline
                         step @!current-step
                         max-step-n (dec (count timeline))
                         tl-entry (get timeline step)
                         get-form-id (fn [tl-e]
                                       (if (= :fn-call (:type tl-e))
                                         (:form-id tl-e)
                                         (->> tl-e :fn-call-idx (get timeline) :form-id)))
                         visible? (fn [e]
                                    (let [rect (.getBoundingClientRect e)]
                                      (<= 0 (.-top rect) (.-bottom rect) (.-innerHeight js/window))))
                         stepper-coord-class (fn [form-id coord]
                                               (str form-id "-coord-" (clojure.string/join "-" coord)))

                         #_#_highlight-styles "background-color: rgb(253 230 138); border-bottom: solid 1px gray; font-weight: bold;" ;; TODO: use a class?
                         highlight-styles-cl {:background-color "rgb(253 230 138)" :border-bottom "solid 1px gray" :font-weight "bold" :color "#000000c9"}]

                     [:div.justify-stretch.grid.grid-cols-5.border-4.border-solid.border-indigo-500.rounded-md
                      {}
                      [:div.col-span-3.flex.flex-col.justify-end

                       [:div.forms.viewer.text-xs.XXXXXcode-viewer.bg-slate-100.dark:bg-slate-800.rounded-tl-md
                        {:ref (fn [el] (reset! !forms-el el))}

                        (let [resolved-stack (->> tl-entry :callstack (get callstacks) (map callstack-entries))
                              highlights (->> resolved-stack
                                              (reduce
                                               (fn [highlights stackframe]
                                                 (if (:ret-form-id stackframe)
                                                   (conj highlights (stepper-coord-class (:ret-form-id stackframe)
                                                                                         (:ret-coord stackframe)))
                                                   highlights))
                                               #{}))]

                          (->> resolved-stack (map :form-id) dedupe
                               (reduce (fn [hiccup form-id]
                                         (conj hiccup
                                               [:div.form.cm-editor.border-b-2.max-h-96.overflow-y-auto
                                                #_(pre-rendered-forms form-id)
                                                (clojure.walk/postwalk
                                                 (fn [form]
                                                   (let [current-step-class (stepper-coord-class  (get-form-id tl-entry) (:coord tl-entry))
                                                         highlight-classes (set (conj highlights current-step-class))]
                                                     (if (and (map? form)
                                                              (:class form)
                                                              (-> (:class form) (clojure.string/split  #" ")
                                                                  set
                                                                  (clojure.set/intersection highlight-classes)
                                                                  not-empty)
                                                              ;; "Highlight complete expression, not just parens:":
                                                              #_(-> (:class form) (clojure.string/split  #" ")
                                                                    (->>
                                                                     (some (fn [element-class]
                                                                             (some #(.startsWith element-class %) highlight-classes))))))
                                                       (assoc form :style highlight-styles-cl)
                                                       form)))

                                                 (pre-rendered-forms form-id))]))
                                       '())))

                        #_[:div.border-t-2
                           (nextjournal.clerk.render/render-code (str code-form) {:language "clojure"})]]
                       [:div.font-sans.text-sm.text-center.opacity-75.p-2 "⬆ call stack ⬆"]]

;; ----------------------------

                      [:div#stepper.flex.flex-col.col-span-2.rounded-r-md.border-l.border-indigo-600
                       #_{:style {:bottom 15 :right 15 :width 600 :height 700 :z-index 10000}}
                       {:style {:height 700}}

                       (let [can-prev? (< 0 @!current-step)
                             can-next? (< @!current-step max-step-n)]
                         [:div#stepper-toolbar.p-2.w-full.flex.justify-between.font-medium.font-sans.text-sm.bg-slate-200.dark:bg-slate-700.rounded-tr-md

                          [:div.flex.gap-2
                           [:button.px-3.py-1.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.border.border-solid.border-slate-600.disabled:opacity-50
                            {:disabled (not can-prev?)
                             :on-click #(reset! !current-step 0)}
                            "|<"]
                           [:button.px-3.py-1.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.border.border-solid.border-slate-600.disabled:opacity-50
                            {:disabled (not can-prev?)
                             :on-click #(swap! !current-step (fn [step] (max 0 (dec step))))}
                            "Prev"]
                           [:button.px-3.py-1.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.border.border-solid.border-slate-600.disabled:opacity-50
                            {:disabled (not can-next?)
                             :on-click #(swap! !current-step (fn [step] (min max-step-n (inc step))))}
                            "Next"]
                           [:button.px-3.py-1.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.border.border-solid.border-slate-600.disabled:opacity-50
                            {:disabled (not can-next?)
                             :on-click #(reset! !current-step max-step-n)}
                            ">|"]]
                          [:div.py-1 (str "Step " (inc step) " of " (inc max-step-n))]])

                       [:div#stepper-val.h-full.w-full.overflow-auto.grow.font-sans.text-sm

                        [:div#stepper-val-header.bg-slate-200.dark:bg-slate-700
                         (cond (zero? step) [:div.p-2 "Press Prev/Next to step through the code execution back and forth"]
                               (= step max-step-n)  [:div.p-2 "Final result:"]
                               :else
                               (case (:type tl-entry)
                                 :fn-call [:div.p-2 "Calling " [:span.font-mono.text-xs (str "(" (:fn-name tl-entry) " ... )")]]
                                 :fn-return [:div.p-2 "Returned from " [:span.font-mono.text-xs (str "(" (->> (:fn-call-idx tl-entry) (get timeline) (:fn-name)) " ... )")]]
                                 nil))]

                        [:div#stepper-val-body.p-2
                         (let [presented-value (get presented-values (:result tl-entry))]
                           [:div
                            (if (zero? @!current-step)
                              [:div
                               [:p "Here you will see the value of the "
                                [:span.p-px {:style highlight-styles-cl} "current"]
                                " code expression highlighted on the call stack on the left panel"]]
                              (when presented-value
                                (nextjournal.clerk.render/inspect-presented
                                 (assoc presented-value :nextjournal/expanded-at
                                        {[1 2 1 0] true, [0 2 1] true, [1 2 1] true, [1 2 1 1] true, [] true, [0 2 1 1] true, [0] true, [1] true, [0 2 1 0] true}
                                        #_{[] true
                                              [0 1] true
                                              [1 1] true
                                              [2 1] true
                                              [3 1] true
                                              [4 1] true
                                              [5 1] true }))

                                #_(update presented-value
                                          :nextjournal/render-opts
                                          assoc :auto-expand-results? true)))])]]]])))})

(defonce _init_storm (index-api/start))


(defn do-trace [the-fn code-form opts]

  (let [thread-id (.getId (Thread/currentThread))]
    (debuggers-api/clear-recordings)
    (debuggers-api/set-recording true)

    ;; (the-fn)
    (eval code-form)

    (flow-storm.runtime.debuggers-api/set-recording false)

    #_(pp/pprint (->> (fn-calls (index-api/get-timeline thread-id) {})
                      (take 50)
                      (map #(index-api/get-form (:form-id %)))
                      #_(map :form/form)))

    {:thread-id thread-id
     #_#_:timeline (take 100 (index-api/get-timeline thread-id))
     :timeline (index-api/get-timeline thread-id)
     :code-form code-form
     :opts opts}))



(defmacro show-trace [opts body]
  `(do-trace (fn []  ~body) (quote ~body) ~opts))


(comment

  (do-trace
   (fn tracing-possible-pmoves [] (boardgames.core/possible-pmoves walkthrough/game-1))
   {:include-fn-names [#"possible-pmoves" #"candidate-pmoves\*?" #"expand-pmove"]})

  (def flow-id 0)
  (def thread-id  (.getId (Thread/currentThread)))
  (def tl (index-api/get-timeline thread-id))

  ;; (timeline-entry [flow-id thread-id idx drift])


  (->> tl (map index-api/as-immutable)
       (filter #(= :fn-call (:type %)))
       #_(map #(index-api/frame-data flow-id thread-id (some % [:fn-call-idx :idx]) {:include-path? true :include-exprs? false :include-binds? false}))
       (take 1))

  (->> tl (map index-api/as-immutable)
       (filter #(or (= :fn-call (:type %)) (= :fn-return (:type %))))
       #_(map #(index-api/frame-data flow-id thread-id (some % [:fn-call-idx :idx]) {:include-path? true :include-exprs? false :include-binds? false}))
       (take 70)
       (map #(str (:type %) " " (:fn-name %)))))
