{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns welcome
  (:require [nextjournal.clerk :as clerk]
            [intro]
            [walkthrough]
            ))

;; # Coding Exercise: Modeling Chess
;;
;;
;;  > __Assignment__: Model the rules of Chess. Design the code with the
;;  flexibility to easily add new types of pieces with unique movements. The
;;  goal is to create a shared core model that can also be used for other
;;  board games, such as Checkers, Tic-Tac-Toe, and similar.
;;
;; <!-- **TL;DR**: Jump straight to the [code walkthrough](./notebooks/walkthrough).-->
;;
;;
;; What started as a whiteboard discussion sent me down a rabbit-hole to attempt a functional and (hopefully) simple implementation... then document it, then prepare it for a presentation, add visualizations, notebooks, implement a better code viewer... 🐰🕳️
;;
;; Anyway... here's what I have so far, as a handful of notebooks:[^skip-intro]
;; [^skip-intro]: Pro-tip: skip the Introduction
;;
;; 1. [Introduction](./notebooks/intro): Why!? Some more background. Goal. On Clojure
;; 1. [Code Walkthrough](./notebooks/walkthrough): Let's start: Modeling the board and pieces
;; 1. [Code Walkthrough 2](./notebooks/walkthrough2): TBD: The crux of it: valid moves
;; 1. [Code Walkthrough 2](./notebooks/walkthrough4): TBD: ??? More on Testing ???
;; 1. [Code Walkthrough 2](./notebooks/walkthrough5): TBD: ??? Other non-chess games ???
;; 1. [Closing](./notebooks/closing): Pending ideas, some conclusions and recommended readings



(clerk/html [:div.font-bold.font-sans.blur-sm {:class "text-blue-400/25"
                                               :style {:position "fixed"
                                                       :top "50%"
                                                       :left "50%"
                                                       :transform "translate(-50%, -50%) rotate(-35deg)"
                                                       :font-size "400px"
                                                       #_#_:color "rgba(0, 0, 0, 0.1)"
                                                       :z-index 9999
                                                       :pointer-events "none"}}
             "DRAFT"])

;; The repo with all code and notebooks: []("https://github.com/dobladez/boardgames-exercise/")
(clerk/html [:div.text-sm.pt-10
             "I'd appreciate any ideas and suggestions for improvements!"
             [:div "All code and notebooks on " [:a {:href "https://github.com/dobladez/boardgames-exercise/"} "this git repo"]]
             [:div "Report " [:a {:href "https://github.com/dobladez/boardgames-exercise/issues"} " issues and suggestions"]]
             [:div
              [:span "I'm on Twitter/X:" [:a {:href "https://twitter.com/dobladez"} "@dobladez"]]
              [:span " - Mastodon:" [:a {:href "https://mastodon.social/@dobladez"} "@dobladez@mastodon.social"]]]

             #_#_#_"I'm " [:a {:href "https://twitter.com/dobladez"} "@dobladez"] " on X/twitter. Happy to hear suggestions and feedback"])
