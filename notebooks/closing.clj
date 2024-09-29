^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns closing
  (:require [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/html
 [:ol.border-b
  [:li [:a {:href "../../"} "Welcome"]]
  [:li [:a {:href (clerk/doc-url "notebooks/intro")} "Introduction"]]
  [:li [:a {:href (clerk/doc-url "notebooks/walkthrough")} "Code Walkthrough"]]
  [:li [:strong "Closing Thoughts"]]])

; # Modeling Chess: Closing Thoughts

;; I really hope you enjoyed this walkthough! I had a lot of fun working on
;; it. I hope you learned something new from it, or that at that least I triggered
;; your curiosity.

;; ## Look Ma! No classes!

;; One of the goals when I started implementing this was to show a simple
;; _functional_ approach to modeling the problem, contrasting it with a more
;; common class-based object-oriented design.
;;
;; The implementation uses no mutation at all, there are no variables.
;
;; There are no classes. And we declared no Types.
;;
;; Note: I'm not against types. Actually, I once in a while miss a good
;; type-checker when working in Clojure. The point I'm trying to make is that
;; types are not at the _core_ of functional programing: data-first, immutability, pure
;; functions, and treating functions as values are.

;; ## Data-Orientation

;; The way we've modeled the problem is by defining the shape of data, and ways to transform it. That's pretty much it.
;;
;; **Data > Code**: Prefer data to code. Look at our rule functions: as
;; implemented, they are very close to being just _data_ too: a good next step
;; could be to define a structure to express expansion rules as data (given a
;; rich set of primitives). Then we could read the rules from configuration
;; files (yaml, xml, json or edn), or a database, allowing end users to define
;; new pieces and games.
;;
;;
;; ## References
;;
;; Some resources related to the ideas from this exercise:
;;
;;  * [Thinking the Clojure Way](https://www.youtube.com/watch?v=vK1DazRK_a0):  A great introductory talk by Rafal Dittwald about functional programming. It's all in JavaScript despite the title
;;  * Book: ["Grokking Simplicity" by Eric Normand](https://www.manning.com/books/grokking-simplicity). I highly recommend it to learn the important practical concepts on functional programming
;;  * Book: ["Structure and Interpretation of Computer Programs" (aka: SICP) by Abelson and Sussman](https://en.wikipedia.org/wiki/Structure_and_Interpretation_of_Computer_Programs): A classic. Excellent book on programming, from zero to pretty advanced topics. Published in 1995, code in Scheme. The book is freely available online
;;  * Book: ["Software Design for Flexibility" (Chris Hanson and Gerald Jay Sussman)](https://mitpress.mit.edu/9780262045490/software-design-for-flexibility/): More recent and more advanced than SCIP, from which I've took the idea of this exercise and some insights for the solution
;;  * Book: ["How to Design Programs" (Felleisen, Findler, Flatt, Krishnamurthi)](https://htdp.org/) Another famous intro to programming using Scheme
;;  * Paul Graham's [bottom-up design essay](http://www.paulgraham.com/progbot.html) (1993)
;;  * The content here is all written as [Clerk](https://book.clerk.vision) _notebooks_
;;  * https://moldabledevelopment.com/
;;  * https://clojure.org/
;;  * [FlowStorm](https://www.flow-storm.org/): The excellent tracing debugger for Clojure (by [jpmonettas](https://github.com/sponsors/jpmonettas)) on which the code-steppers on this notebooks are based
;;  * All talks by Rich Hickey
;;
;; Let me know your suggestions!
;;
;; Thanks again, and happy hacking! 👋
