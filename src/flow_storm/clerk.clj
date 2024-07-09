;; # Code Viewers with integration with Flow Storm tracing
^{:nextjournal.clerk/visibility {:code :hide}}
(ns flow-storm.clerk
  (:require [boardgames.utils :refer [TAP>]]
            [clojure.walk :as walk]
            [flow-storm.form-pprinter :as form-pprinter]
            [flow-storm.runtime.indexes.api :as index-api]
            [flow-storm.runtime.debuggers-api :as debuggers-api]
            [flow-storm.utils :as utils]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk.viewer]))


(def idx (atom 0))
(def _flow-id 0)
(def _thread-id 47)

(defn show-current []
  (let [flow-id _flow-id
        thread-id _thread-id
        {:keys [type fn-ns fn-name coord fn-call-idx result] :as idx-entry} (index-api/timeline-entry flow-id thread-id @idx :at)
        {:keys [form-id]} (index-api/frame-data flow-id thread-id fn-call-idx {})
        {:keys [form/form]} (index-api/get-form form-id)]
    (case type
      :fn-call (let [{:keys [fn-name fn-ns]} idx-entry]
                 (println "Called" fn-ns fn-name))
      (:expr :fn-return) (let [{:keys [coord result]} idx-entry]
                           (form-pprinter/pprint-form-hl-coord form coord)
                           (println "\n")
                           (println "==[VAL]==>" (utils/colored-string result :yellow))))))

(defn step-next []
  (swap! idx inc)
  (show-current))

(defn step-prev []
  (swap! idx dec)
  (show-current))

;; --------

(def trace-code-render-fn

  '(fn [tl]
     (let []
       [nextjournal.clerk.render/inspect-presented tl]
       #_[nextjournal.clerk.render/inspect tl]

       #_[:div
          [:h1 "*Storm experiments:"]
          [:div
           [:pre (prn-str tl)]
           #_(for [entry (->> tl reverse #_(filter #(= :fn-call (:type %))))]
               [:div
                [:div.flex.gap-2 #_(for [a (:fn-args entry)]
                                     #_[:div a]
                                     #_[:div [nextjournal.clerk.render/inspect-presented a]]
                                     [:div [nextjournal.clerk.render/inspect a]]
                                     #_[:pre (prn-str a)])]
                #_[:div.flex.gap-2 (for [a (:fn-args entry)]
                                     [:pre (prn-str (meta a))])]

                #_[:pre (:_form entry)]
                #_[:div [nextjournal.clerk.render/render-folded-code-block (:_form entry)]]
                #_[:div [nextjournal.clerk.render/render-code-block (:_form entry)]]
                #_[:pre (prn-str (select-keys entry [:type :fn-name]))]])]])))


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

(defn- demunge-fn
  [fn-object]
  (let [dem-fn (clojure.repl/demunge (str fn-object))
        pretty (second (re-find #"(.*?\/.*?)[\-\-|@].*" dem-fn))]
    (if pretty pretty dem-fn)))



(def flow-id 0)
#_(def thread-id  (.getId (Thread/currentThread)))



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
  (let [{:keys [form-id]} (index-api/frame-data flow-id thread-id (:fn-call-idx tl-entry) {})
        {:keys [form/form]} (index-api/get-form form-id)]
    (assoc tl-entry :form form :_form (storm-form->str form :NEVER))))

(def trace-code-viewer
  {:name `trace-code-render
   #_#_:pred (fn [x] x)

   :transform-fn (clerk/update-val #_clerk/mark-presented
                  (fn [{:keys [thread-id timeline opts] :as value}]
                    (println "********* meta value: " (meta value))
                    (println "********* value: " (prn value))
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
   #_#_:render-fn trace-code-render-fn})







(defonce _init_storm (index-api/start))


(defn do-trace [the-fn opts]

  (let [thread-id (.getId (Thread/currentThread))]
    (debuggers-api/clear-recordings)
    (debuggers-api/set-recording true)

    (the-fn)

    (flow-storm.runtime.debuggers-api/set-recording false)

    {:thread-id thread-id
     :timeline (index-api/get-timeline thread-id)
     :opts opts} ))



(defn code-tracing-view [the-fn opts]
  #_ (do-trace the-fn opts)
  (clerk/with-viewer trace-code-viewer opts
    (do-trace the-fn opts)))

(defmacro show-trace [opts body]
  `(do-trace (fn []  ~body) ~opts))



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