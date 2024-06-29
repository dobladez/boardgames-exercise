(ns boardgames.clerk-test
  (:require [clojure.test :as test]
            [boardgames.rook-test]
            [boardgames.pawn-test]
            [boardgames.kingcheck-test]
            [boardgames.clerk-viewers :as viewers]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.test :as clerk.test]))

;; ^{::clerk/visibility {:code :hide :result :hide}}
;; (clerk/add-viewers! [viewers/board-viewer])

#_(clerk.test/reset-state!)

{::clerk/visibility {:code :hide :result :show}}

^{::clerk/no-cache true}
(clerk/with-viewer clerk.test/test-suite-viewer
  @clerk.test/!test-report-state)

^{::clerk/no-cache true}
(binding [test/report clerk.test/report]

  (clerk.test/reset-state!)
  (println "*************** inside binding****************")
  #_(test/run-all-tests #"^boardgames\..+\-test$")
  #_(test/run-all-tests #"^boardgames\.rook-test")
  (test/run-all-tests))
