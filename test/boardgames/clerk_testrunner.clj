(ns boardgames.clerk-testrunner
  {:nextjournal.clerk/visibility {:code :hide :result :show} }

  (:require [clojure.test :as test]

            [boardgames.clerk-viewers :as viewers]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.test :as clerk.test]

            ;; note: require all test ns' to ensure they are all loaded before the runner
            [boardgames.pawn-test]
            [boardgames.rook-test]
            [boardgames.simple-test]
            [boardgames.kingcheck-test]

            ))


^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(defn run-tests-on-clerk []
  (binding [test/report clerk.test/report]
    (clerk.test/reset-state!)
    (test/run-all-tests #"boardgames.*-test$")
    (clerk/recompute!)))

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(defonce test-run (run-tests-on-clerk))


^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(clerk/add-viewers! [viewers/board-viewer])

^{::clerk/visibility {:code :hide :result :show}
  ::clerk/viewers (concat [clerk.test/test-suite-viewer viewers/board-viewer] clerk/default-viewers)}
@clerk.test/!test-report-state


(clerk/with-viewer clerk.test/test-suite-viewer
  @clerk.test/!test-report-state)



#_(comment
    (run-tests-on-clerk)
    )
