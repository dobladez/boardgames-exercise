(ns user
  (:require [nextjournal.clerk :as clerk]))

(defn start!
  "Starts a Clerk server process "
  ([] (start! {}))
  ([opts]
   (let [defaults {:port 7777
                   :watch-paths ["notebooks" "src"]
                   :browse? true}]
     (clerk/serve!

      (merge defaults opts)))))


(defn stop!
  "Stops a Clerk server process "
  []
  (clerk/halt!)
  )


(comment
  (start!)
  (clerk/clear-cache!)
  (stop!)

  ;; If you started your repl with Portal in the classpath:
  (require 'portal.api)
  (def p (portal.api/open))
  (portal.api/close p)
  (tap> :test)
  (add-tap #'portal.api/submit)
  )
