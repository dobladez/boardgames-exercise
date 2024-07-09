(ns boardgames.clerk-testrunner
  {:nextjournal.clerk/visibility {:code :hide :result :show} }

  (:require [clojure.test :as test]
            [boardgames.rook-test]
            [boardgames.pawn-test]
            [boardgames.kingcheck-test]
            [boardgames.clerk-viewers :as viewers]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.test :as clerk.test]))


#_(defonce trigger-run nil)
#_(def test-report
    (clerk/with-viewer clerk.test/test-suite-viewer
      @clerk.test/!test-report-state))


(defn run-tests-on-clerk []
  (binding [test/report clerk.test/report]
    (clerk.test/reset-state!)
    (test/run-all-tests #"boardgames.*-test$")
    (clerk/recompute!)))

(defonce test-run (run-tests-on-clerk))

(clerk/with-viewer clerk.test/test-suite-viewer
  @clerk.test/!test-report-state)



(comment
  (run-tests-on-clerk)
  )
