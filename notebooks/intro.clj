^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns intro
  (:require
            [nextjournal.clerk :as clerk]
            [flow-storm.clerk :as clerk-storm]))

;; # Modeling chess: Intro
;;
;; ### Problem statement
;;
;; Model the game of chess.  Make the model flexible so that chess variations
;; can be also be implemented (for example, defining special pieces with new
;; movements). Design it so that a core of the model can be used to implement
;; other board games, such as Checkers, Tic-Tac-Toe and similar.
;;
;; ### Goal
;;
;; The goal of this exercise is purely pedagogical: to learn and teach some
;; software design concepts, covering the basics of topics such us:
;;
;; * Functional programming
;; * Literate programming
;; * Interactive (and "moldable") development
;; * Data-orientation
;; * Stratified design
;; * Simplicity
;; * Immutability
;; * Clojure
;;
;; ### Why Clojure? I wised you used _&lt;JavaScript/Python/...&gt;_
;;
;; The implemenation is in Clojure. It's a simple functional language I like a lot.

;; If you don't know Clojure, this might be a good opportunity to learn the
;; basics of it. I encourage you to re-implement the exercise in your favorite
;; language as you follow the code walkthroughs.
;;
;; Note: This is not a tutorial on Clojure itself, but I hope it's not too hard
;; to follow. I tried to use only basic features of Clojure (no managed refs, no
;; multimethods, no protocols, no transducers, etc.)  Also, the code has no
;; external library depedencies, not even for the graphical widgets to visualize
;; the boards on the notebooks.
;;

(defn example-adder [a b]
  (+ a b))

(defn example-hello [name]
  (str "Hello " name "!"))

^{::clerk/viewer clerk-storm/timeline-stepper-viewer}
(clerk-storm/show-trace {:include-fn-names []}
  (let [sum (example-adder 1 2)
        msg (example-hello "World")]
    (str msg " - " sum)))

;; ### Some references
;;
;;  * [Thinking the Clojure Way](https://www.youtube.com/watch?v=vK1DazRK_a0) a great talk by Rafal Dittwald. It's all in JavaScript despite the title.
;;  * Book: SICP (Structure and Interpretation of Computer Programs) by Abelson and Sussman
;;  * Book: Software Design for Flexibility (Chris Hanson and Gerald Jay Sussman)
;;  * Paul Graham's [bottom-up design essay](http://www.paulgraham.com/progbot.html)
;;  * "Lisp: A language for stratified design"
;;  * "Grokking Simplicity" by Eric Normand. A book on functional programming for beginners. I highly recommend it to learn the important practical concepts before delving into types and category theory.
;;  * The content is written as [Clerk](https://book.clerk.vision) _notebooks_
;;
;;
;; ### Code organization
;;
;; In case you jump straight to the codebase instead of reading the walkgrough: The code is organized in the following namespaces:
;;
;; |namespace | description|
;; |----------|------------|
;; |[clerk-viewers](/src/boardgames/clerk_viewers) | The graphical widgets to visualize boards and moves on Clerk notebooks |
;; |[core](/src/boardgames/core/) | generic domain to model board games|
;; |[chess](/src/boardgames/chess/) | implementation of classic Chess (WIP)|
;; |[checkers](/src/boardgames/checkers) | implementation of Checkers (_TBD_) |
;; |[ttt](/src/boardgames/ttt) | implementation of Tic-Tac-Toe  (_TBD_) |
;;
;;
;; ### GO! 🏁
;; Go to the first notebook [notebook](./walkthrough) for a code walkthrough of the implementation.

#_(clerk/html [:ol
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url 'boardgames.clerk-viewers)} "clerk-viewers"]]
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url 'boardgames.core)} "core"]]
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url "/boardgames/core")} "core"]]
               [:li [:a {:href (nextjournal.clerk.viewer/doc-url "/src/boardgames/core")} "core"]]
               [:li [:a {:href "/src/boardgames/core/"} "core4"]]])
