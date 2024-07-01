(ns user
  (:require [nextjournal.clerk :as clerk]))

(defn start!
  "Starts a Clerk server process "
  ([] (start! {}))
  ([opts]
   (let [defaults {:port 7778
                   :watch-paths ["notebooks" "src" #_"test"]
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

  (nextjournal.clerk/show! 'gameboard)
  (nextjournal.clerk/show! 'nextjournal.clerk.tap)

  (clerk/show! 'boardgames)

  (comment
  ;; - `:bundle`      - if true results in a single self-contained html file including inlined images
  ;; - `:compile-css` - if true compiles css file containing only the used classes
  ;; - `:ssr`         - if true runs react server-side-rendering and includes the generated markup in the html
  ;; - `:browse`      - if true will open browser with the built file on success
  ;; - `:dashboard`   - if true will start a server and show a rich build report in the browser (use with `:bundle` to open browser)
  ;; - `:out-path`  - a relative path to a folder to contain the static pages (defaults to `\"public/build\"`)
  ;; - `:git/sha`, `:git/url` - when both present, each page displays a link to `(str url \"blob\" sha path-to-notebook)`
    )
  (clerk/build! {:paths ["notebooks/*"]
                 :index "notebooks/boardgames.clj"
                 :ssr true
                 #_#_:compile-css true})

  ;; If you started your repl with Portal in the classpath:
  (require 'portal.api)
  (def p (portal.api/open))
  (portal.api/close p)
  (tap> :test)
  (add-tap #'portal.api/submit)

  )

