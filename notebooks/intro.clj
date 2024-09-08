^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns intro
  (:require
            [nextjournal.clerk :as clerk]
            [flow-storm.clerk :as clerk-storm]))

; # Modeling Chess: Introduction
;;
;; ---
;; 1. [Welcome](./welcome)
;; 1. Introduction 👈 __you are here__
;; 1. [Code Walkthrough 1](./walkthrough): Let's get started. Basic modeling of board and pieces.
;; ---
;;
;; ### Background story
;;
;; * I was helping undergraduate students with a coding exercise about modeling
;; the game of Chess. Many of them getting trapped in class-based OO
;; design (using Java or Kotlin). Days later:
;;
;; * I re-started reading book _"Software Design for
;; Flexibility_".[^sdff_book] Coincidentally, chapter _2.4 "Abstracting a
;; Domain"_ models the game of Checkers. It then proceeds to abstract a core
;; domain, and leaves the implement the rules of Chess as an exercise.
;; [^sdff_book]: by Chris Hanson and Gerald Jay Sussman ![image](https://mit-press-us.imgix.net/covers/9780262045490.jpg?auto=format&w=298&dpr=2&q=80)
;;
;; Soon after, I found myself working on the exercise, hoping to achieve a
;; simple solution using basic functional programming concepts. These notebooks
;; are my attempt at documenting it.
;;
;; The goal of this exercise is pedagogical: to learn software design concepts,
;; touching upon the basics of topics such us:
;;
;; * Functional programming
;; * Immutability
;; * Literate programming
;; * Interactive (and "moldable") development
;; * Data-orientation
;; * Stratified design
;; * Simplicity
;; * some Testing techniques
;; * and some Clojure
;;
;; ### "Why Clojure? I wished you used _&lt;JavaScript/Python/...&gt;_"
;;
;; Why not? Clojure is a simple functional language, ideal for my goal... and I
;; like it 🙂.
;;
;; If you don't know Clojure:
;;
;;   * I hope the code is not too hard to follow. I tried to use only basic
;; features of the language, and no external library depedencies
;;   * Hey, this might be a good opportunity for you to learn and try Clojure!
;;   * If you don't want to use Clojure, that's fine too: I'd encourage you to
;; re-implement this exercise in your favorite language as you follow the code
;; walkthrough.
;;
;; ### Clojure code-stepping debugger
;;
;; Let's first define a function `hello-message` that receives a `name` (String) and returns a `"Hello ... !"` message (String):

^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(defn hello-message [name]
  (str "Hello " name "!"))

;; If you are new to Lisp languages, the biggest syntax difference is in the way
;; you call a function: on C/JavaScript-like syntax you write: `functionName(arg1,
;; arg2, ...)`, while in Lisp/Clojure you write: `(function-name arg1 arg2 ...)` instead.
;;
;; If you are new to Lisp languages, the biggest syntax difference is in the way
;; you call a function:[^lisp_simplification]
;; * in C/JavaScript-like syntax: `functionName(arg1, arg2, ...)`
;; * in Lisp/Clojure syntax: `(function-name arg1 arg2 ...)`
;; [^lisp_simplification]: an over-simplification off-course, but not a lie
;;
;; Here's an expression that calls the above function passing `"World"`, which evaluates to, you guessed it, `"Hello World!"`:
^{:nextjournal.clerk/visibility {:code :show :result :show}}
(hello-message "World")

;;
;; To make some code snippets easier to follow on these notebooks, I worked on a code viewer that let's you step through the code execution (back and forth).[^flowstorm]
;; [^flowstorm]: Based on the (amazing!) [FlowStorm](https://www.flow-storm.org/) by [jpmonettas](https://github.com/sponsors/jpmonettas) ![](https://github.com/flow-storm/flow-storm-debugger/raw/master/docs/images/icon.png)

#_ "I'm sorry if you favorite language doesn't have something like it. Just wait... every good thing from Lisp/Clojure finds its way into other languages, only decades later ;-)"

;;
;; Given the function above, let's evaluate the following code snippet, which:
;; 1. first calls that function passing `"World"`, and then
;; 2. repeats the result (returned by function `hello-message`) ten times, evaluating to a sequence:

^{::clerk/visibility {:code :hide :result :show}
  ::clerk/width :wide
  ::clerk/viewer clerk-storm/timeline-stepper-viewer}
(clerk-storm/show-trace {:include-fn-names []}
  (repeat 10 (hello-message "World"))
  #_(let [msg (hello-message "World")]
    (repeat 10 msg)))

;;
;; ### Some references
;; Some recommended readings on these topics:
;;
;;  * [Thinking the Clojure Way](https://www.youtube.com/watch?v=vK1DazRK_a0) a great talk by Rafal Dittwald. It's all in JavaScript despite the title
;;  * Book: ["Grokking Simplicity" by Eric Normand](https://www.manning.com/books/grokking-simplicity). A book for beginners. I highly recommend it to learn the important practical concepts on functional programming
;;  * Book: ["Structure and Interpretation of Computer Programs" (aka: SCIP) by Abelson and Sussman](https://en.wikipedia.org/wiki/Structure_and_Interpretation_of_Computer_Programs):  Famous brilliant book on programming, from basics to pretty advanced topics. From 1995, code in Scheme.  The book is freely available online
;;  * Book: ["Software Design for Flexibility" (Chris Hanson and Gerald Jay Sussman)](https://mitpress.mit.edu/9780262045490/software-design-for-flexibility/): More recent and more advanced
;;  * Paul Graham's [bottom-up design essay](http://www.paulgraham.com/progbot.html)
;;  * "Lisp: A language for stratified design"
;;  * The content here is written as [Clerk](https://book.clerk.vision) _notebooks_
;;
;;
;; ### Code organization
;;
;; In case you jump straight to the codebase instead of reading the walktrough: The code is organized in the following namespaces:
;;
;; |namespace | description|
;; |----------|------------|
;; |[clerk-viewers](/src/boardgames/clerk_viewers) | The graphical widgets to visualize boards and moves on Clerk notebooks |
;; |[core](/src/boardgames/core/) | generic domain to model board games|
;; |[chess](/src/boardgames/chess/) | implementation of classic Chess (WIP)|
;; |[checkers](/src/boardgames/checkers) | implementation of Checkers (_TBD_) |
;; |[ttt](/src/boardgames/ttt) | implementation of Tic-Tac-Toe  (_TBD_) |
;;
;; Repo at: https://github.com/dobladez/boardgames-exercise
;;
;; ### GO! 🏁
;;
;; Enought talk. Start the [walkthrough](./walkthrough) of the implementation.
