;; # Code Viewers with integration with Flow Storm tracing
^{:nextjournal.clerk/visibility {:code :hide}}
(ns flow-storm.clerk
  (:require [boardgames.utils :refer [TAP>]]
            [clojure.walk :as walk]
            [clojure.string :as str]
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

(defn serialize-timeline [imm-timeline]


  (let [!values-index (atom (new-index))
        _add-to-index! (partial add-to-index! !values-index clerk.viewer/present)
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
                                                                             {:fn-name (:fn-name entry)
                                                                              :form-id (:form-id entry)}))
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
     :callstack-entries (:values @!callstack-entries-index)
     }))



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
                                          [:span {:class (str (stepper-coord-class form-id coord) " " (stepper-token-class text))} text]
                                          [:span {:class (stepper-token-class text)} text]))))]))
                forms-map)))

(defn get-form-id [imm-timeline tl-e]
  (if (= :fn-call (:type tl-e))
    (:form-id tl-e)
    (->> tl-e :fn-call-idx (get imm-timeline) :form-id)))

(def timeline-stepper-viewer
  {:name `timeline-stepper-viewer
   :transform-fn (comp
                  clerk/mark-preserve-keys
                  clerk/mark-presented
                  (clerk/update-val
                   (fn [{:keys [thread-id timeline values-index code-form opts] :as value}]
                     (let [#_#_opts (merge opts (select-keys [:include-fn-names] (meta value)))
                           forms (reduce (fn [forms {:keys [form-id]}]
                                           (let [{:keys [form/form]} (index-api/get-form form-id)]
                                             (assoc forms form-id form)))
                                         {}
                                         (fn-calls timeline {}))

                           ret {:pre-rendered-forms (pre-render-forms forms)
                                :code-form (pre-render-form 0 code-form)
                                :ser-timeline (->> timeline (mapv index-api/as-immutable) serialize-timeline)}]

                       #_(prn "-----2!!! ")
                       #_(prn (last (-> ret :ser-timeline :timeline)))
                       #_(prn "-----3!!! ")
                       #_(prn (last (-> ret :ser-timeline :presented-values)))
                       (spit "/tmp/ser-timeline.edn" (prn-str (-> ret :ser-timeline)))

                       ret))))

   :render-fn '(fn [{:keys [pre-rendered-forms ser-timeline code-form]}]
                 (reagent.core/with-let [!current-step (reagent.core/atom 1)
                                         !forms-el (reagent.core/atom nil)]
                   (let [{:keys [timeline presented-values callstacks callstack-entries]} ser-timeline
                         step @!current-step
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

                         highlight-styles "background-color: rgb(253 230 138); border-bottom: solid 1px gray; font-weight: bold;" ;; TODO: use a class?
                         re-highlight (fn [prev-step next-step]
                                        (let [prev-e (get timeline prev-step)
                                              next-e (get timeline next-step)
                                              prev-class (stepper-coord-class (get-form-id prev-e) (:coord prev-e))
                                              next-class (stepper-coord-class (get-form-id next-e) (:coord next-e))
                                              prev-dom-elems (.getElementsByClassName (.-firstElementChild @!forms-el) prev-class)
                                              #_(js/document.getElementsByClassName prev-class)
                                              next-dom-elems (.getElementsByClassName (.-firstElementChild @!forms-el) next-class)
                                              #_(js/document.getElementsByClassName next-class)]
                                          (doseq [e prev-dom-elems]
                                            (.removeAttribute e "style"))
                                          (doseq [e next-dom-elems]
                                            (.setAttribute e "style" highlight-styles)
                                            #_(.setClass e "bg-amber-200"))
                                          (let [focus-e (first next-dom-elems)]
                                            (when-not (visible? focus-e)
                                              (.scrollIntoView focus-e (clj->js {:behavior "smooth" :block "nearest" :inline "start"}))))))]

                     [:div.px-12.justify-stretch.grid.grid-cols-5.gap-2
                      [:div.col-span-3
                       [:div.font-sans "Call stack"]
                       [:div.forms.viewer.text-xs.code-viewer.pl-4
                        {:ref (fn [el] (reset! !forms-el el))}

                        (for [form-id (->> tl-entry :callstack (get callstacks) (map callstack-entries) reverse (map :form-id) dedupe)]
                          [:div.form.cm-editor.border-t-2.max-h-96.overflow-y-auto
                           (pre-rendered-forms form-id)])
                        [:div.form.cm-editor.border-t-2
                         {:style {:background-color "rgb(253 230 138)"}} ;; border-bottom: solid 1px gray; font-weight: bold;
                         code-form]
                        #_[:div.border-t-2
                           (nextjournal.clerk.render/render-code (str code-form) {:language "clojure"})]]]

                      [:div#stepper.border-2.border-solid.border-indigo-500.rounded-md.bg-white.flex.flex-col.col-span-2
                       #_{:style {:bottom 15 :right 15 :width 600 :height 700 :z-index 10000}}
                       {:style {:height 700}}

                       [:div#stepper-val.h-full.w-full.overflow-auto.grow.font-sans.text-sm

                        [:div#stepper-val-header.bg-slate-200.p-2.rounded-t-md
                         (case (:type tl-entry)
                           :fn-call [:div "Calling " [:span.font-mono.text-xs (str "(" (:fn-name tl-entry) " ... )")]]
                           :fn-return [:div "Returned by " [:span.font-mono.text-xs (str "(" (->> (:fn-call-idx tl-entry) (get timeline) (:fn-name)) " ... )")]]
                           nil)]

                        [:div#stepper-val-body.p-2
                         (let [presented-value (get presented-values (:result tl-entry))]
                           [:div
                            (when presented-value
                              [nextjournal.clerk.render/inspect-presented presented-value])
                            #_[:div
                               [:h4 "tl entry"]
                               [:pre (prn-str tl-entry)]
                               [:h4 "presented-value"]
                               [:pre (prn-str presented-value)]
                               [:h4 "presented-values"]
                               [:pre (prn-str presented-values)]]])]]

                       [:div#stepper-toolbar.p-2.w-full.flex.justify-between.font-medium.font-sans.text-sm.bg-slate-200.rounded-b-md
                        [:div.flex.gap-2
                         [:button.px-3.py-1.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.border.border-solid.border-slate-600
                          {:on-click #(let [prev-step @!current-step]
                                        (swap! !current-step dec)
                                        (re-highlight prev-step @!current-step))}
                          "Prev"]
                         [:button.px-3.py-1.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.border.border-solid.border-slate-600
                          {:on-click #(let [prev-step @!current-step]
                                        (swap! !current-step inc)
                                        (re-highlight prev-step @!current-step))}
                          "Next"]]
                        [:div.py-1 (str "Step " step " of " (count timeline))]]]])))})


(defonce _init_storm (index-api/start))


(defn do-trace [the-fn code-form opts]

  (let [thread-id (.getId (Thread/currentThread))]
    (debuggers-api/clear-recordings)
    (debuggers-api/set-recording true)

    (the-fn)

    (flow-storm.runtime.debuggers-api/set-recording false)

    {:thread-id thread-id
     :timeline (index-api/get-timeline thread-id)
     #_#_:timeline (index-api/get-timeline thread-id)
     :code-form code-form
     :opts opts} ))



;; (defn code-tracing-view [the-fn opts]
;;   #_ (do-trace the-fn opts)
;;   (clerk/with-viewer trace-code-viewer opts
;;     (do-trace the-fn opts)))

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
