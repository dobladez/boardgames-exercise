^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns furtherreading
  (:require [nextjournal.clerk :as clerk]))


; # Modeling Chess: Further Reading

^{::clerk/visibility {:code :hide :result :show}}
(clerk/html
 [:ol.border-b
  [:li [:a {:href (clerk/doc-url "notebooks/welcome")} "Welcome"]]
  [:li [:a {:href (clerk/doc-url "notebooks/intro")} "Introduction"]]
  [:li [:a {:href (clerk/doc-url "notebooks/walkthrough")} "Code Walkthrough"]]
  [:li [:strong "Further Reading"]]])

;;
;; Some recommended readings related to this little project. Let me know your suggestions:
;;
;;  * [Thinking the Clojure Way](https://www.youtube.com/watch?v=vK1DazRK_a0):  A great introductory talk by Rafal Dittwald about functional programming. It's all in JavaScript despite the title
;;  * Book: ["Grokking Simplicity" by Eric Normand](https://www.manning.com/books/grokking-simplicity). I highly recommend it to learn the important practical concepts on functional programming
;;  * Book: ["Structure and Interpretation of Computer Programs" (aka: SCIP) by Abelson and Sussman](https://en.wikipedia.org/wiki/Structure_and_Interpretation_of_Computer_Programs): A classic. Excellent book on programming, from zero to pretty advanced topics. Published in 1995, code in Scheme. The book is freely available online
;;  * Book: ["Software Design for Flexibility" (Chris Hanson and Gerald Jay Sussman)](https://mitpress.mit.edu/9780262045490/software-design-for-flexibility/): More recent and more advanced then SCIP, from which I've took the idea of this exercise and some insights for the solution
;;  * Paul Graham's [bottom-up design essay](http://www.paulgraham.com/progbot.html) (1993)
;;  * The content here is all written as [Clerk](https://book.clerk.vision) _notebooks_
;;  * https://moldabledevelopment.com/
;;  * https://clojure.org/
;;  * [FlowStorm](https://www.flow-storm.org/): The excellent tracing debugger for Clojure (by [jpmonettas](https://github.com/sponsors/jpmonettas)) on which the code-steppers on this notebooks are based.
;;
;; Thanks again, and happy hacking! 👋
