
(ns scratchpad
  {:nextjournal.clerk/toc true}
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk-viewer]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            ;;            [boardgames.checkers :as checkers]
            [boardgames.utils :as utils]
            [flow-storm.clerk :as clerk-storm]))

{:nextjournal.clerk/visibility {:code :show :result :show}
 :nextjournal.clerk/auto-expand-results? false}

;; ## Scratchpad to try ideas/componets


(comment
  ^{::clerk/visibility {:code :hide :result :hide}} (clerk/reset-viewers! clerk/default-viewers)
  )

(comment
  ^{::clerk/visibility {:code :show :result :hide}}
  (clerk/add-viewers! [viewers/board-viewer]))



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


^{:nextjournal.clerk/auto-expand-results? true}
(first (map (partial clerk/with-viewer viewers/tabbed-board-move-viewer {})
            (sort-by #(-> % :steps last :piece :pos)
                     (core/possible-pmoves game-1))))

;; ### possible moves
^{:nextjournal.clerk/auto-expand-results? true}
(clerk/row
 (sort-by #(-> % :steps last :piece :pos)
          (core/possible-pmoves game-1)))

;; ## Clerk storm protype
#_#_

^{::clerk/visibility {:code :hide :result :hide}} (clerk/reset-viewers! clerk/default-viewers)
(clerk-storm/code-tracing-view
 (fn tracing-possible-pmoves []
   (core/possible-pmoves game-1))

 {:include-fn-names [#"possible-pmoves" #"candidate-pmoves\*?" #"expand-pmove"]
  })


#_
^{::clerk/viewer clerk-storm/trace-code-viewer}
(clerk-storm/show-trace {:include-fn-names [#"possible-pmoves" #"candidate-pmoves\*?" #"expand-pmove"]}
    (core/possible-pmoves game-1))

^{::clerk/viewer clerk-storm/trace-code-viewer}
(clerk-storm/show-trace {:include-fn-names [#"possible-pmoves" #"candidate-pmoves\*?" #"expand-pmove"] }
  (core/possible-pmoves game-1))





(comment
  "ideas on how to show the stack"

  (core/possible-pmoves arg1 arg2)

  -> (candidate-moves game-state) ;; <-- form as it appears on parentq
  --> (candidate-moves* game-state) ;; <-- form as it appears on parentq



  )
