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
;;  board games, such as Checkers, Tic-Tac-Toe, and similar
;;
(clerk/html [:div.text-sm.border-t.py-4
             [:strong "TL;DR: "]
             [:span "jump straight to the " [:a {:href "./notebooks/walkthrough"} " Code Walkthrough"]]])
;; ⚠️ **Spoiler Alert!** This is a solution to exercise 2.13 of book _"Software Design for
;; Flexibility."_[^sdff_book]\
;; \
;; 🐰🕳️  What started as a whiteboard discussion sent me down a rabbit-hole to attempt
;; a functional and (hopefully) simple implementation... then document it, then
;; prepare it for a presentation, add visualizations, notebooks, implement a
;; better code viewer, ...
;; 1. [Introduction](./notebooks/intro): Why!? Some more background. Goal. On Clojure
;; 1. [Code Walkthrough](./notebooks/walkthrough): Code Walkthrough of a possible solution. Be patient, might take a few seconds to load!
;; 1. [Further Reading](./notebooks/furtherreading): Some references and resources
;; [^sdff_book]: by Chris Hanson and Gerald Jay Sussman ![image](https://mit-press-us.imgix.net/covers/9780262045490.jpg?auto=format&w=298&dpr=2&q=80)

;; ---

;; I'd appreciate any ideas and suggestions for improvements!\
;; All code and notebooks on [this git repo](https://github.com/dobladez/boardgames-exercise/) \
;; Twitter/X: [@dobladez](https://twitter.com/dobladez) - Mastodon: [@dobladez@mastodon.social](https://mastodon.social/@dobladez)

#_(clerk/html [:div.text-sm.pt-10
               "I'd appreciate any ideas and suggestions for improvements!"
               [:div "All code and notebooks on " [:a {:href "https://github.com/dobladez/boardgames-exercise/"} "this git repo"]]
               [:div "Report " [:a {:href "https://github.com/dobladez/boardgames-exercise/issues"} " issues and suggestions"]]
               [:div
                [:span "I'm on Twitter/X:" [:a {:href "https://twitter.com/dobladez"} "@dobladez"]]
                [:span " - Mastodon:" [:a {:href "https://mastodon.social/@dobladez"} "@dobladez@mastodon.social"]]]

               #_#_#_"I'm " [:a {:href "https://twitter.com/dobladez"} "@dobladez"] " on X/twitter. Happy to hear suggestions and feedback"])



#_(clerk/html [:div.font-bold.font-sans.blur-sm {:class "text-blue-400/25"
                                               :style {:position "fixed"
                                                       :top "50%"
                                                       :left "50%"
                                                       :transform "translate(-50%, -50%) rotate(-35deg)"
                                                       :font-size "400px"
                                                       #_#_:color "rgba(0, 0, 0, 0.1)"
                                                       :z-index 9999
                                                       :pointer-events "none"}}
             "DRAFT"])
