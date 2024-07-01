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


(do
  (binding [test/report clerk.test/report]
    (println "***** about to run clerk.test/rest-state!")
    (clerk.test/reset-state!)

    (println "***** about to run test/run-all-tests!")
    (test/run-all-tests #"boardgames.*-test$")
    #_(clerk/clear-cache! 'test-report)
    #_(clerk/recompute!))

  (clerk/with-viewer clerk.test/test-suite-viewer
    @clerk.test/!test-report-state))
