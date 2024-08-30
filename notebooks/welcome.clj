{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns welcome
  (:require [nextjournal.clerk :as clerk]))

;; # Exercise in modeling chess
;; _(... and solution, with simple functional programming)_
;;
;; I stared this little "project" after the following two events happened the
;; same week:
;;
;; * I was helping Software Engineering students with an exercise in modeling
;; the game of Chess and variations of it. Many of them getting trapped in
;; class-based OO design (using Java or Kotlin).
;;
;; * I was reading chapter 2.4 "Abstracting a Domain" from the book "Software
;; Design for Flexibility"[^sdff_book]. Coincidentally, it models the game of Checkers and asks to extend
;; it to Chess as an exercise.
;;
;; [^sdff_book]: by Chris Hanson and Gerald Jay Sussman ![image](https://mit-press-us.imgix.net/covers/9780262045490.jpg?auto=format&w=298&dpr=2&q=80)
;;
;; I've split this into separate notebooks:
;;
;; * [Introduction](./notebooks/intro)[^boring]: Quick intro. Why? Why in Clojure? Goals. Background. Some References.
;;
;; * [Code Walkthrough](./notebooks/walkthrough): Code walkthrough
;; * [Code Walkthrough 2](./notebooks/walkthrough): TBD: ??? basic modeling of boards and pieces ???
;; * [Code Walkthrough 3](./notebooks/walkthrough): TBD: ??? generaling valid moves ???
;; * [Code Walkthrough 2](./notebooks/walkthrough): TBD: ??? tests ???
;; * [Code Walkthrough 2](./notebooks/walkthrough): TBD: ??? generalizing the domain for other games??
;;
;; [^boring]: The intro is boring. Just go straight to the code walkthroughs
;;
#_ "I'm [@dobladez](https://twitter.com/dobladez) on X/twitter, and I'm happy to hear your thoughts, suggestions, and feedback."


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

(clerk/html [:div.text-sm.pt-10 "I'm " [:a {:href "https://twitter.com/dobladez"} "@dobladez"] " on X/twitter. Happy to hear suggestions and feedback"])
